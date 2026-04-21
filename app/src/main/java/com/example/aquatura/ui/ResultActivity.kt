package com.example.aquatura.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aquatura.R
import com.example.aquatura.data.FishInfoRepository
import com.example.aquatura.data.Prediction
import com.example.aquatura.databinding.ActivityResultBinding
import com.google.android.material.chip.Chip
import java.io.File

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        


        
        val fishName = intent.getStringExtra(EXTRA_FISH_NAME) ?: "Unknown"
        val confidence = intent.getFloatExtra(EXTRA_CONFIDENCE, 0f)
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val fishIndex = intent.getIntExtra(EXTRA_FISH_INDEX, -1)

        
        val fishInfo = if (fishIndex >= 0) {
            FishInfoRepository.getByIndex(fishIndex)
        } else {
            FishInfoRepository.getByName(fishName)
        }

        
        binding.fishName.text = fishName
        
        val confidencePercent = confidence * 100
        binding.confidenceChip.text = "${confidencePercent.toInt()}% Match"
        
        // Update confidence chip color
        val (bgColor, textColor) = when {
            confidence >= 0.85 -> Pair(R.color.confidence_high, R.color.white)
            confidence >= 0.65 -> Pair(R.color.confidence_medium, R.color.white)
            else -> Pair(R.color.confidence_low, R.color.white)
        }
        binding.confidenceChip.chipBackgroundColor = ColorStateList.valueOf(
            ContextCompat.getColor(this, bgColor)
        )
        binding.confidenceChip.setTextColor(ContextCompat.getColor(this, textColor))
        
        // Keep fish image background black (set in XML) - no border color application needed

        
        val refDrawableId = if (fishIndex >= 0) getFishReferenceDrawable(fishIndex) else 0
        if (refDrawableId != 0) {
            try {
                // Safely load the resource bitmap with downsampling if needed
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(resources, refDrawableId, options)
                
                // Calculate optimal inSampleSize
                options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
                options.inJustDecodeBounds = false
                
                val bitmap = BitmapFactory.decodeResource(resources, refDrawableId, options)
                if (bitmap != null) {
                    binding.resultImage.setImageBitmap(bitmap)
                } else {
                    // Fallback if decoding fails
                    loadCapturedImage(imagePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadCapturedImage(imagePath)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                // Try to load captured image instead, or just fail gracefully
                loadCapturedImage(imagePath)
            }
        } else {
            loadCapturedImage(imagePath)
        }

        
        binding.resultImage.setOnClickListener {
            showImagePreview()
        }

        
        if (fishInfo != null) {
            
            binding.scientificName.text = fishInfo.scientificName
            binding.scientificName.visibility = View.VISIBLE

            
            binding.localNames.text = fishInfo.localNames.joinToString(", ")

            
            binding.description.text = fishInfo.description

            
            binding.maxSize.text = "${fishInfo.maxSizeCm.toInt()} cm"
            binding.maxWeight.text = fishInfo.maxWeightKg?.let { "${it} kg" } ?: "—"

            
            binding.habitat.text = fishInfo.habitat

            // Set up diet
            binding.diet.text = fishInfo.diet
        } else {
            
            binding.scientificName.visibility = View.GONE
            binding.localNamesLabel.visibility = View.GONE
            binding.localNames.visibility = View.GONE
            binding.description.text = "Information not available for this fish."
            binding.maxSize.text = "—"
            binding.maxWeight.text = "—"
            binding.habitat.text = "Information not available."
            binding.diet.text = "Information not available."
        }
        
        // Set up similar species
        val predictions = try {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            intent.getSerializableExtra(EXTRA_PREDICTIONS) as? ArrayList<Prediction>
        } catch (e: Exception) {
            null
        }
        
        if (predictions != null && predictions.size > 1) {
            setupSimilarSpecies(predictions.drop(1).take(3))
        } else {
            binding.similarSpeciesCard.visibility = View.GONE
        }
        
        // Set up learn more button
        setupLearnMoreButton(fishInfo?.scientificName ?: fishName)
        
        binding.scanAgainFab.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
    
    private fun setupSimilarSpecies(predictions: List<Prediction>) {
        val recyclerView = binding.similarSpeciesRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = SimilarSpeciesAdapter(predictions) { selectedFish ->
            // User tapped a similar fish - reload this activity with new fish
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(EXTRA_FISH_NAME, selectedFish.fishName)
                putExtra(EXTRA_CONFIDENCE, selectedFish.confidence)
                putExtra(EXTRA_FISH_INDEX, selectedFish.classIndex)
                putExtra(EXTRA_IMAGE_PATH, getIntent().getStringExtra(EXTRA_IMAGE_PATH))
            }
            startActivity(intent)
        }
    }
    
    private fun setupLearnMoreButton(scientificName: String) {
        binding.learnMoreButton.setOnClickListener {
            // Use Wikipedia as alternative to FishBase
            val wikipediaUrl = "https://en.wikipedia.org/wiki/${scientificName.replace(" ", "_")}"
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wikipediaUrl))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No browser found. Please install a web browser.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showImagePreview() {
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = android.widget.ImageView(this)
        imageView.setImageDrawable(binding.resultImage.drawable)
        imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        imageView.setBackgroundColor(android.graphics.Color.BLACK)
        
        imageView.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.setContentView(imageView)
        dialog.show()
    }


    private fun getFishReferenceDrawable(index: Int): Int {
        val resourceName = "fish_$index"
        return try {
            resources.getIdentifier(resourceName, "drawable", packageName)
        } catch (e: Exception) {
            0
        }
    }
    
    private fun loadCapturedImage(imagePath: String?) {
        if (imagePath != null) {
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                    binding.resultImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    companion object {
        const val EXTRA_FISH_NAME = "extra_fish_name"
        const val EXTRA_CONFIDENCE = "extra_confidence"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_FISH_INDEX = "extra_fish_index"
        const val EXTRA_PREDICTIONS = "extra_predictions"
    }
}
