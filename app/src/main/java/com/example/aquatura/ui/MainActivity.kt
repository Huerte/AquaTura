package com.example.aquatura.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.GravityCompat
import androidx.core.content.ContextCompat
import com.example.aquatura.R
import com.example.aquatura.databinding.ActivityMainBinding
import com.example.aquatura.utils.BitmapUtils
import com.example.aquatura.utils.ImageQualityChecker
import com.example.aquatura.viewmodel.DetectionState
import com.example.aquatura.viewmodel.MainViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.io.File
import java.io.FileOutputStream
import com.example.aquatura.App
private const val PREF_ONBOARDING_SHOWN = "pref_onboarding_shown"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private var imageCapture: ImageCapture? = null
    private var processedImagePath: String? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedBitmap: Bitmap? = null
    private var isFlashOn = false
    private val hintFadeRunnable = Runnable { animateHintFadeOut() }
    
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                showPermissionDeniedDialog()
            }
        }
    
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            handleGalleryImage(uri)
        }
    }
    
    private val multipleMatchesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                // User clicked "Scan Again" - reset to camera
                capturedBitmap?.recycle()
                capturedBitmap = null
                processedImagePath = null
                hideCropSelection()
            }
            RESULT_CANCELED -> {
                // User pressed Back - return to crop if bitmap exists
                if (capturedBitmap != null) {
                    showCropSelection(capturedBitmap!!)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Back Press for Crop Mode
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.cropContainer.visibility == View.VISIBLE) {
                    hideCropSelection()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        observeViewModel()
    }
    

    


    private fun setupUI() {
        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_about -> {
                    val intent = android.content.Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }



        // Crop UI Controls
        // Crop UI Controls
        // binding.cropCloseButton removed in favor of Back navigation

        binding.cropRotateButton.setOnClickListener {
            rotateCapturedImage()
        }

        binding.cropConfirmButton.setOnClickListener {
            processCroppedImage()
        }
        
        binding.galleryButton.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        
        binding.flashButton.setOnClickListener {
            toggleFlash()
        }
    }
    
    private fun toggleFlash() {
        isFlashOn = !isFlashOn
        val iconRes = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        binding.flashButton.setImageResource(iconRes)
        
        val cameraControl = imageCapture?.camera?.cameraControl
        cameraControl?.enableTorch(isFlashOn)
    }
    
    private fun showHint() {
        binding.hintCard.visibility = View.VISIBLE
        binding.hintCard.alpha = 1f
        binding.hintCard.removeCallbacks(hintFadeRunnable)
        binding.hintCard.postDelayed(hintFadeRunnable, 4000)
    }
    
    private fun animateHintFadeOut() {
        binding.hintCard.animate()
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                binding.hintCard.visibility = View.GONE
            }
            .start()
    }

    


    private fun observeViewModel() {
        viewModel.detectionState.observe(this) { state ->
            when (state) {
                is DetectionState.Idle -> {
                    binding.loadingOverlay.visibility = View.GONE
                    binding.captureButton.visibility = View.VISIBLE
                    binding.viewFinder.visibility = View.VISIBLE
                    showHint()
                }
                is DetectionState.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                    binding.captureButton.visibility = View.GONE
                    binding.hintCard.visibility = View.GONE
                    binding.hintCard.removeCallbacks(hintFadeRunnable)
                }
                is DetectionState.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    
                    // Always direct to MultipleMatchesActivity even for high confidence
                    // Create a list with the top prediction and alternates
                    val predictions = ArrayList<com.example.aquatura.data.Prediction>()
                    predictions.add(state.prediction)
                    predictions.addAll(state.alternates)
                    
                    val intent = Intent(this, MultipleMatchesActivity::class.java)
                    intent.putExtra(MultipleMatchesActivity.EXTRA_PREDICTIONS, predictions)
                    if (processedImagePath != null) {
                        intent.putExtra(ResultActivity.EXTRA_IMAGE_PATH, processedImagePath)
                    }
                    multipleMatchesLauncher.launch(intent)
                    viewModel.resetState()
                }
                is DetectionState.MultipleMatches -> {
                    binding.loadingOverlay.visibility = View.GONE
                    
                    val intent = Intent(this, MultipleMatchesActivity::class.java)
                    intent.putExtra(MultipleMatchesActivity.EXTRA_PREDICTIONS, ArrayList(state.matches))
                    if (processedImagePath != null) {
                        intent.putExtra(ResultActivity.EXTRA_IMAGE_PATH, processedImagePath)
                    }
                    multipleMatchesLauncher.launch(intent)
                    viewModel.resetState()
                }
                is DetectionState.NotAFish -> {
                    binding.loadingOverlay.visibility = View.GONE
                    
                    // Hide camera controls during dialog
                    setCameraControlsVisibility(false)
                    
                    val dialog = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.not_fish_detected_title))
                        .setMessage(getString(R.string.not_fish_detected_message) + "\n\n" + getString(R.string.not_fish_tips))
                        .setPositiveButton(getString(R.string.try_again)) { dialog, _ ->
                            dialog.dismiss()
                            // Restore camera controls visibility
                            setCameraControlsVisibility(true)
                            viewModel.resetState()
                        }
                        .setCancelable(true)
                        .setOnCancelListener {
                            // Restore camera controls visibility
                            setCameraControlsVisibility(true)
                            viewModel.resetState()
                        }
                        .create()
                    styleDialog(dialog)
                    dialog.show()
                }
                is DetectionState.UnknownSpecies -> {
                    binding.loadingOverlay.visibility = View.GONE
                    
                    // Hide camera controls during dialog
                    setCameraControlsVisibility(false)
                    
                    val message = buildString {
                        append(getString(R.string.unknown_fish_message))
                        append("\n\n")
                        append(getString(R.string.unknown_fish_reasons))
                        if (state.bestGuess != null) {
                            append("\n\n")
                            append(getString(R.string.best_guess_label))
                            append("\n${state.bestGuess.fishName} (${(state.bestGuess.confidence * 100).toInt()}%)")
                        }
                    }
                    
                    val dialog = AlertDialog.Builder(this)
                        .setTitle(getString(R.string.unknown_fish_title))
                        .setMessage(message)
                        .setPositiveButton(getString(R.string.try_again)) { dialog, _ ->
                            dialog.dismiss()
                            // Restore camera controls visibility
                            setCameraControlsVisibility(true)
                            viewModel.resetState()
                        }
                        .setCancelable(true)
                        .setOnCancelListener {
                            // Restore camera controls visibility
                            setCameraControlsVisibility(true)
                            viewModel.resetState()
                        }
                        .create()
                    styleDialog(dialog)
                    dialog.show()
                }
                is DetectionState.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    
                    // Restore camera controls on error
                    setCameraControlsVisibility(true)
                    
                    val friendlyMessage = when {
                        state.message.contains("Model not loaded", ignoreCase = true) -> 
                            getString(R.string.error_model_load)
                        state.message.contains("Invalid", ignoreCase = true) -> 
                            getString(R.string.error_invalid_image)
                        else -> getString(R.string.error_generic)
                    }
                    
                    Toast.makeText(this, friendlyMessage, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        viewModel.resetPredictionBuffer()



        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    
                    if (bitmap != null) {
                        capturedBitmap = bitmap
                        showCropSelection(bitmap)
                    } else {
                        Toast.makeText(baseContext, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(baseContext, "Photo capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap: ${e.message}")
            null
        }
    }

    private fun showCropSelection(bitmap: Bitmap) {
        binding.capturedImageView.setImageBitmap(bitmap)
        binding.cropContainer.visibility = View.VISIBLE
        binding.viewFinder.visibility = View.GONE
        binding.hintCard.visibility = View.GONE
        
        // Hide all camera controls during crop mode
        setCameraControlsVisibility(false)
        
        // Clear any previous selection
        binding.cropOverlay.clearSelection()
        binding.hintCard.removeCallbacks(hintFadeRunnable)
        
        binding.cropConfirmButton.isEnabled = true
        binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun hideCropSelection() {
        binding.cropContainer.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        
        // Show all camera controls when exiting crop mode
        setCameraControlsVisibility(true)
        
        showHint()
        binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
        capturedBitmap?.recycle()
        capturedBitmap = null
    }

    private fun rotateCapturedImage() {
        val bitmap = capturedBitmap ?: return
        
        // Capture current selection state relative to view
        val currentSelection = binding.cropOverlay.getSelectionRect()
        val viewWidth = binding.cropOverlay.width.toFloat()
        val viewHeight = binding.cropOverlay.height.toFloat()
        
        // Show loading state
        binding.loadingOverlay.visibility = View.VISIBLE
        
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                // Rotate bitmap 90 degrees clockwise
                val matrix = android.graphics.Matrix()
                matrix.postRotate(90f)
                
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0, 0,
                    bitmap.width, bitmap.height,
                    matrix,
                    true
                )
                
                withContext(Dispatchers.Main) {
                    if (rotatedBitmap != bitmap) {
                        bitmap.recycle()
                    }
                    capturedBitmap = rotatedBitmap
                    binding.capturedImageView.setImageBitmap(rotatedBitmap)
                    
                    if (currentSelection != null && viewWidth > 0 && viewHeight > 0) {
                         // Normalize coordinates
                         val nLeft = currentSelection.left / viewWidth
                         val nTop = currentSelection.top / viewHeight
                         val nRight = currentSelection.right / viewWidth
                         val nBottom = currentSelection.bottom / viewHeight
                         
                         
                         val p1x = 1f - nTop
                         val p1y = nLeft
                         val p2x = 1f - nBottom
                         val p2y = nRight
                         
                         val newLeft = kotlin.math.min(p1x, p2x) * viewWidth
                         val newRight = kotlin.math.max(p1x, p2x) * viewWidth
                         val newTop = kotlin.math.min(p1y, p2y) * viewHeight
                         val newBottom = kotlin.math.max(p1y, p2y) * viewHeight
                         
                         val newRect = android.graphics.RectF(newLeft, newTop, newRight, newBottom)
                         
                         // Post to ensure view is ready (if layout changes, though it shouldn't here)
                         binding.cropOverlay.post {
                             binding.cropOverlay.setSelection(newRect)
                         }
                    } else {
                        binding.cropOverlay.clearSelection()
                    }
                    
                    binding.loadingOverlay.visibility = View.GONE
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.loadingOverlay.visibility = View.GONE
                    Log.e(TAG, "Error rotating image: ${e.message}")
                    Toast.makeText(this@MainActivity, "Failed to rotate image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun processCroppedImage() {
        val bitmap = capturedBitmap ?: return
        val selectionRect = binding.cropOverlay.getSelectionRect()
        
        try {
            val finalBitmap = if (selectionRect != null) {
                val viewWidth = binding.cropOverlay.width.toFloat()
                val viewHeight = binding.cropOverlay.height.toFloat()
                
                val scaleX = bitmap.width.toFloat() / viewWidth
                val scaleY = bitmap.height.toFloat() / viewHeight
                
                val cropX = (selectionRect.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
                val cropY = (selectionRect.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
                val cropWidth = (selectionRect.width() * scaleX).toInt().coerceIn(1, bitmap.width - cropX)
                val cropHeight = (selectionRect.height() * scaleY).toInt().coerceIn(1, bitmap.height - cropY)
                
                Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
            } else {
                // If no selection, use a copy of the original bitmap
                bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            }
            
            val blurredBg = BitmapUtils.createBlurredBackground(bitmap)
            binding.loadingBackgroundImage.setImageBitmap(blurredBg)
            
            binding.cropContainer.visibility = View.GONE
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
            
            // Save the processed bitmap to a file so we can pass it to ResultActivity
            processedImagePath = saveBitmapToFile(finalBitmap)
            if (processedImagePath == null) {
                Log.e(TAG, "Failed to save processed image to cache")
            }
            
            viewModel.processBitmapValidated(finalBitmap)
            
            // Keep capturedBitmap for potential return to crop mode
            // capturedBitmap?.recycle()
            // capturedBitmap = null
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}")
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleGalleryImage(uri: Uri) {
        try {
            viewModel.resetPredictionBuffer()
            
            binding.loadingOverlay.visibility = View.VISIBLE
            
            val bitmap = BitmapUtils.loadOptimizedBitmap(this, uri)
            
            if (bitmap == null) {
                binding.loadingOverlay.visibility = View.GONE
                showErrorDialog(
                    getString(R.string.image_too_large),
                    getString(R.string.image_too_large_message)
                )
                return
            }
            
            binding.loadingOverlay.visibility = View.GONE
            
            // Check image quality before showing crop
            val qualityResult = ImageQualityChecker.checkQuality(bitmap)
            if (!qualityResult.isGoodQuality) {
                showImageQualityDialog(qualityResult.message, bitmap)
                return
            }
            
            // Direct gallery image to crop section (same flow as camera capture)
            capturedBitmap = bitmap
            showCropSelection(bitmap)
            
        } catch (e: OutOfMemoryError) {
            binding.loadingOverlay.visibility = View.GONE
            showErrorDialog(
                getString(R.string.out_of_memory),
                getString(R.string.out_of_memory_message)
            )
            Log.e(TAG, "Out of memory loading gallery image", e)
        } catch (e: Exception) {
            binding.loadingOverlay.visibility = View.GONE
            showErrorDialog(
                "Error Loading Image",
                "Failed to load the selected image. Please try another image."
            )
            Log.e(TAG, "Error loading gallery image: ${e.message}", e)
        }
    }
    
    private fun showPermissionDeniedDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_denied_title))
            .setMessage(getString(R.string.permission_denied_message))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .create()
        styleDialog(dialog)
        dialog.show()
    }
    
    private fun showErrorDialog(title: String, message: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        styleDialog(dialog)
        dialog.show()
    }
    
    private fun showImageQualityDialog(message: String, bitmap: Bitmap) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.image_quality_poor))
            .setMessage(message)
            .setPositiveButton(getString(R.string.try_again)) { dialog, _ ->
                dialog.dismiss()
                capturedBitmap = bitmap
                showCropSelection(bitmap)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                bitmap.recycle()
            }
            .create()
        styleDialog(dialog)
        dialog.show()
    }
    
    private fun styleDialog(dialog: androidx.appcompat.app.AlertDialog) {
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(ContextCompat.getColor(this, R.color.ocean_blue))
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }
    
    private fun setCameraControlsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.captureButton.visibility = visibility
        binding.galleryButton.visibility = visibility
        binding.flashButton.visibility = visibility
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        capturedBitmap?.recycle()
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        return try {
            val filename = "fish_capture_${System.currentTimeMillis()}.jpg"
            val file = File(cacheDir, filename)
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.close()
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
