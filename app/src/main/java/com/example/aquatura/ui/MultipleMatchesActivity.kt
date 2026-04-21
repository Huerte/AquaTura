package com.example.aquatura.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.aquatura.R
import com.example.aquatura.data.FishInfoRepository
import com.example.aquatura.data.Prediction
import com.example.aquatura.databinding.ActivityMultipleMatchesBinding
import com.google.android.material.card.MaterialCardView

class MultipleMatchesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMultipleMatchesBinding
    private var matches: List<Prediction> = emptyList()
    
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // User clicked "Scan Again" in ResultActivity - propagate to MainActivity
            setResult(RESULT_OK)
            finish()
        }
        // If RESULT_CANCELED (Back pressed), stay on this screen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultipleMatchesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        matches = try {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            intent.getSerializableExtra(EXTRA_PREDICTIONS) as? ArrayList<Prediction> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val topMatch = matches.firstOrNull()
        if (topMatch != null && topMatch.confidence > 0.8f) {
            binding.headerTitle.text = getString(R.string.match_found_title)
            binding.headerSubtitle.text = getString(R.string.match_found_subtitle)
        } else {
            binding.headerTitle.text = getString(R.string.multiple_matches_title)
            binding.headerSubtitle.text = getString(R.string.multiple_matches_subtitle)
        }

        displayMatches()

        binding.retakeButton.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun displayMatches() {
        val top3 = matches.take(3)
        
        top3.forEachIndexed { index, prediction ->
            val cardView = LayoutInflater.from(this).inflate(
                R.layout.item_fish_match_card,
                binding.matchesContainer,
                false
            ) as MaterialCardView

            val fishName = cardView.findViewById<TextView>(R.id.fishName)
            val fishDescription = cardView.findViewById<TextView>(R.id.fishDescription)
            val confidenceText = cardView.findViewById<TextView>(R.id.confidenceText)
            val fishThumbnail = cardView.findViewById<ImageView>(R.id.fishThumbnail)

            fishName.text = prediction.fishName
            confidenceText.text = getString(R.string.percent_format, (prediction.confidence * 100).toInt())

            val fishInfo = FishInfoRepository.getByIndex(prediction.classIndex)
            fishDescription.text = fishInfo?.habitat ?: "Freshwater fish"

            val drawableId = getFishReferenceDrawable(prediction.classIndex)
            if (drawableId != 0) {
                fishThumbnail.setImageResource(drawableId)
            }

            val (bgColor, strokeColor, textColor) = when (index) {
                0 -> Triple(R.color.match_rank_1_bg, R.color.match_rank_1_border, R.color.match_rank_1_text)
                1 -> Triple(R.color.match_rank_2_bg, R.color.match_rank_2_border, R.color.match_rank_2_text)
                else -> Triple(R.color.match_rank_3_bg, R.color.match_rank_3_border, R.color.match_rank_3_text)
            }

            cardView.setCardBackgroundColor(ContextCompat.getColor(this, bgColor))
            cardView.strokeColor = ContextCompat.getColor(this, strokeColor)
            confidenceText.setTextColor(ContextCompat.getColor(this, textColor))

            cardView.setOnClickListener {
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra(ResultActivity.EXTRA_FISH_NAME, prediction.fishName)
                intent.putExtra(ResultActivity.EXTRA_CONFIDENCE, prediction.confidence)
                intent.putExtra(ResultActivity.EXTRA_FISH_INDEX, prediction.classIndex)
                val imagePath = getIntent().getStringExtra(ResultActivity.EXTRA_IMAGE_PATH)
                if (imagePath != null) {
                    intent.putExtra(ResultActivity.EXTRA_IMAGE_PATH, imagePath)
                }
                // Pass the predictions list for similar species display
                intent.putExtra(ResultActivity.EXTRA_PREDICTIONS, ArrayList(matches))
                resultLauncher.launch(intent)
            }

            binding.matchesContainer.addView(cardView)
        }
    }

    private fun getFishReferenceDrawable(index: Int): Int {
        val resourceName = "fish_$index"
        return try {
            resources.getIdentifier(resourceName, "drawable", packageName)
        } catch (e: Exception) {
            0
        }
    }

    companion object {
        const val EXTRA_PREDICTIONS = "extra_predictions"
    }
}
