package com.example.aquatura.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Canvas
import android.graphics.Paint
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.LinkedList
import kotlin.math.ln
import kotlin.math.min


data class Prediction(
    val fishName: String,
    val confidence: Float,
    val classIndex: Int = -1  
) : Serializable


sealed class ValidationResult {

    data class ValidFish(
        val prediction: Prediction,
        val alternates: List<Prediction> = emptyList()
    ) : ValidationResult()
    
    data class MultipleMatches(
        val matches: List<Prediction>,  // Top 3 predictions
        val topConfidence: Float
    ) : ValidationResult()
    
    data class NotRecognized(
        val reason: String,
        val topConfidence: Float = 0f,
        val bestGuess: Prediction? = null
    ) : ValidationResult()
    
    data class Error(val message: String) : ValidationResult()
}


class FishDetector(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val modelFileName = "fish_model.tflite"
    
    // Temporal averaging for live camera mode
    private val PREDICTION_BUFFER_SIZE = 5
    private val recentPredictions = LinkedList<FloatArray>()
    
    private var lastInferenceTime = 0L
    private val INFERENCE_INTERVAL_MS = 333L
    
    
    private val inputWidth = 224
    private val inputHeight = 224
    private val inputChannels = 3
    
    
    private val fishLabels = listOf(
        "Bangus",                  
        "Big Head Carp",           
        "Black Spotted Barb",      
        "Catfish",                 
        "Climbing Perch",          
        "Fourfinger Threadfin",    
        "Freshwater Eel",          
        "Glass Perchlet",          
        "Goby",                    
        "Gold Fish",               
        "Gourami",                 
        "Grass Carp",              
        "Green Spotted Puffer",    
        "Indian Carp",             
        "Indo-Pacific Tarpon",     
        "Jaguar Gapote",           
        "Janitor Fish",            
        "Knifefish",               
        "Long-Snouted Pipefish",   
        "Mosquito Fish",           
        "Mudfish",                 
        "Mullet",                  
        "Pangasius",               
        "Perch",                   
        "Scat Fish",               
        "Silver Barb",             
        "Silver Carp",             
        "Silver Perch",            
        "Snakehead",               
        "Tenpounder",              
        "Tilapia"                  
    )
    
    private var isModelLoaded = false

    init {
        loadModel()
    }
    

    private fun loadModel() {
        try {
            println("FishDetector: Attempting to load model: $modelFileName")
            android.util.Log.d("FishDetector", "Attempting to load model: $modelFileName")
            val modelBuffer = loadModelFile(modelFileName)
            println("FishDetector: Model buffer loaded, size: ${modelBuffer.capacity()} bytes")
            android.util.Log.d("FishDetector", "Model buffer loaded, size: ${modelBuffer.capacity()} bytes")
            
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            
            val inputShape = interpreter!!.getInputTensor(0).shape()
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            println("FishDetector loaded. Input: ${inputShape.contentToString()}, Output: ${outputShape.contentToString()}")
            android.util.Log.d("FishDetector", "Input shape: ${inputShape.contentToString()}")
            android.util.Log.d("FishDetector", "Output shape: ${outputShape.contentToString()}")
            
            if (outputShape[1] != 31) {
                val errorMsg = "Model output size is ${outputShape[1]}, expected 31 classes. Wrong model file?"
                println("FishDetector ERROR: $errorMsg")
                throw IllegalStateException(errorMsg)
            }
            
            isModelLoaded = true
            println("FishDetector: Model loaded successfully - 31 fish classes")
            android.util.Log.d("FishDetector", "TFLite model loaded successfully - 31 fish classes")
        } catch (e: Exception) {
            println("FishDetector CRITICAL ERROR loading model: ${e.message}")
            android.util.Log.e("FishDetector", "Error loading model: ${e.message}", e)
            e.printStackTrace()
            isModelLoaded = false
        }
    }
    

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            inputWidth,
            inputHeight,
            true
        )
        
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * inputChannels)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(inputWidth * inputHeight)
        resizedBitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        
        var pixel = 0
        for (i in 0 until inputHeight) {
            for (j in 0 until inputWidth) {
                val value = intValues[pixel++]
                
                byteBuffer.putFloat(((value shr 16) and 0xFF) / 255.0f)  
                byteBuffer.putFloat(((value shr 8) and 0xFF) / 255.0f)   
                byteBuffer.putFloat((value and 0xFF) / 255.0f)           
            }
        }
        
        byteBuffer.rewind()
        
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        
        return byteBuffer
    }
    

    private fun adjustBrightness(bitmap: Bitmap, factor: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            setScale(factor, factor, factor, 1f)
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    

    private fun runSingleInference(bitmap: Bitmap): FloatArray? {
        if (!isModelLoaded || interpreter == null) {
            return null
        }
        
        try {
            val inputBuffer = preprocessBitmap(bitmap)
            
            val inputTensor = interpreter!!.getInputTensor(0)
            val outputTensor = interpreter!!.getOutputTensor(0)
            
            android.util.Log.d("FishDetector", "Input tensor expects: ${inputTensor.numBytes()} bytes, shape: ${inputTensor.shape().contentToString()}")
            android.util.Log.d("FishDetector", "ByteBuffer has: ${inputBuffer.capacity()} bytes, position: ${inputBuffer.position()}")
            
            val outputShape = outputTensor.shape()
            val outputSize = outputShape[1]
            
            val outputBuffer = ByteBuffer.allocateDirect(4 * outputSize)
            outputBuffer.order(ByteOrder.nativeOrder())
            
            interpreter!!.run(inputBuffer, outputBuffer)
            
            outputBuffer.rewind()
            val probabilities = FloatArray(outputSize)
            outputBuffer.asFloatBuffer().get(probabilities)
            
            return probabilities
        } catch (e: Exception) {
            android.util.Log.e("FishDetector", "Error during inference: ${e.message}", e)
            println("Error during single inference: ${e.message}")
            return null
        }
    }
    

    fun detectFish(bitmap: Bitmap): Prediction? {
        if (!isModelLoaded || interpreter == null) {
            println("Interpreter not initialized or model missing")
            return Prediction("Model not found - Please add fish_model.tflite", 0.0f)
        }
        
        val probabilities = runSingleInference(bitmap) ?: return null
        
        
        var maxConfidence = 0f
        var maxIndex = 0
        
        probabilities.forEachIndexed { index, confidence ->
            if (confidence > maxConfidence) {
                maxConfidence = confidence
                maxIndex = index
            }
        }
        
        
        val fishName = if (maxIndex < fishLabels.size) {
            fishLabels[maxIndex]
        } else {
            "Unknown Fish #$maxIndex"
        }
        
        return Prediction(fishName, maxConfidence)
    }
    

    fun detectFishHighAccuracy(bitmap: Bitmap, inferenceCount: Int = 5): Prediction? {
        if (!isModelLoaded || interpreter == null) {
            println("Interpreter not initialized or model missing")
            return Prediction("Model not found - Please add fish_model.tflite", 0.0f)
        }
        
        // Resize bitmap once to input size to avoid memory issues with large bitmaps during augmentation
        val scaledBitmap = if (bitmap.width == inputWidth && bitmap.height == inputHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        }
        
        try {
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val outputSize = outputShape[1]
            
            
            val accumulatedProbs = FloatArray(outputSize) { 0f }
            var successfulInferences = 0
            
            
            val brightnessFactors = listOf(1.0f, 0.9f, 1.1f, 0.95f, 1.05f, 0.85f, 1.15f)
            
            for (i in 0 until min(inferenceCount, brightnessFactors.size)) {
                val augmentedBitmap = if (i == 0) {
                    scaledBitmap 
                } else {
                    adjustBrightness(scaledBitmap, brightnessFactors[i])
                }
                
                val probs = runSingleInference(augmentedBitmap)
                
                
                if (i != 0) {
                    augmentedBitmap.recycle()
                }
                
                if (probs != null) {
                    for (j in probs.indices) {
                        accumulatedProbs[j] += probs[j]
                    }
                    successfulInferences++
                }
            }
            
            if (successfulInferences == 0) {
                return null
            }
            
            
            for (i in accumulatedProbs.indices) {
                accumulatedProbs[i] /= successfulInferences
            }
            
            
            var maxConfidence = 0f
            var maxIndex = 0
            
            accumulatedProbs.forEachIndexed { index, confidence ->
                if (confidence > maxConfidence) {
                    maxConfidence = confidence
                    maxIndex = index
                }
            }
            
            
            val fishName = if (maxIndex < fishLabels.size) {
                fishLabels[maxIndex]
            } else {
                "Unknown Fish #$maxIndex"
            }
            
            return Prediction(fishName, maxConfidence)
            
        } catch (e: Exception) {
            println("Error during high accuracy inference: ${e.message}")
            e.printStackTrace()
            return null
        } finally {
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
        }
    }
    

    fun detectFishTopN(bitmap: Bitmap, topN: Int = 3): List<Prediction> {
        if (!isModelLoaded || interpreter == null) {
            println("Interpreter not initialized")
            return listOf(Prediction("Model not found", 0.0f))
        }
        
        val probabilities = runSingleInference(bitmap) ?: return emptyList()
        
        
        val predictions = probabilities
            .mapIndexed { index, confidence -> index to confidence }
            .sortedByDescending { it.second }
            .take(topN)
            .map { (index, confidence) ->
                val fishName = if (index < fishLabels.size) {
                    fishLabels[index]
                } else {
                    "Unknown Fish #$index"
                }
                Prediction(fishName, confidence)
            }
        
        return predictions
    }
    

    fun detectFishTopNHighAccuracy(bitmap: Bitmap, topN: Int = 3, inferenceCount: Int = 5): List<Prediction> {
        if (!isModelLoaded || interpreter == null) {
            println("Interpreter not initialized")
            return listOf(Prediction("Model not found", 0.0f))
        }
        
        // Resize bitmap once to input size to avoid memory issues with large bitmaps during augmentation
        val scaledBitmap = if (bitmap.width == inputWidth && bitmap.height == inputHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        }
        
        try {
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val outputSize = outputShape[1]
            
            
            val accumulatedProbs = FloatArray(outputSize) { 0f }
            var successfulInferences = 0
            
            val brightnessFactors = listOf(1.0f, 0.9f, 1.1f, 0.95f, 1.05f, 0.85f, 1.15f)
            
            for (i in 0 until min(inferenceCount, brightnessFactors.size)) {
                val augmentedBitmap = if (i == 0) {
                    scaledBitmap
                } else {
                    adjustBrightness(scaledBitmap, brightnessFactors[i])
                }
                
                val probs = runSingleInference(augmentedBitmap)
                
                if (i != 0) {
                    augmentedBitmap.recycle()
                }
                
                if (probs != null) {
                    for (j in probs.indices) {
                        accumulatedProbs[j] += probs[j]
                    }
                    successfulInferences++
                }
            }
            
            if (successfulInferences == 0) {
                return emptyList()
            }
            
            
            for (i in accumulatedProbs.indices) {
                accumulatedProbs[i] /= successfulInferences
            }
            
            
            val predictions = accumulatedProbs
                .mapIndexed { index, confidence -> index to confidence }
                .sortedByDescending { it.second }
                .take(topN)
                .map { (index, confidence) ->
                    val fishName = if (index < fishLabels.size) {
                        fishLabels[index]
                    } else {
                        "Unknown Fish #$index"
                    }
                    Prediction(fishName, confidence)
                }
            
            return predictions
            
        } catch (e: Exception) {
            println("Error during high accuracy inference: ${e.message}")
            e.printStackTrace()
            return emptyList()
        } finally {
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
        }
    }
    

    fun isReady(): Boolean = isModelLoaded && interpreter != null
    

    fun getClassCount(): Int = fishLabels.size
    

    fun getAllFishNames(): List<String> = fishLabels.toList()
    
    
    // Temporal averaging for live camera mode
    private fun averagePredictions(currentProbs: FloatArray): FloatArray {
        recentPredictions.add(currentProbs.clone())
        if (recentPredictions.size > PREDICTION_BUFFER_SIZE) {
            recentPredictions.removeFirst()
        }
        
        val averaged = FloatArray(currentProbs.size)
        for (probs in recentPredictions) {
            for (i in probs.indices) {
                averaged[i] += probs[i]
            }
        }
        for (i in averaged.indices) {
            averaged[i] /= recentPredictions.size
        }
        return averaged
    }
    
    fun resetPredictionBuffer() {
        recentPredictions.clear()
        lastInferenceTime = 0L
    }
    
    
    companion object {
        // High confidence: Show single result
        const val HIGH_CONFIDENCE_THRESHOLD = 0.65f
        
        // Medium confidence: Show multiple matches
        const val MEDIUM_CONFIDENCE_THRESHOLD = 0.45f
        
        // Below this: Unknown species
        const val UNKNOWN_THRESHOLD = 0.45f
    }
    

    private fun calculateNormalizedEntropy(probabilities: FloatArray): Float {
        if (probabilities.isEmpty()) return 1f
        
        val n = probabilities.size
        if (n == 1) return 0f
        
        var entropy = 0.0
        for (p in probabilities) {
            if (p > 0) {
                entropy -= p * ln(p.toDouble())
            }
        }
        
        
        val maxEntropy = ln(n.toDouble())
        return if (maxEntropy > 0) (entropy / maxEntropy).toFloat() else 0f
    }
    

    private fun validatePrediction(probabilities: FloatArray): ValidationResult {
        if (probabilities.isEmpty()) {
            return ValidationResult.Error("No predictions available")
        }
        
        // Sort predictions by confidence
        val sortedPredictions = probabilities
            .mapIndexed { index, confidence -> index to confidence }
            .sortedByDescending { it.second }
        
        val (topIndex, topConfidence) = sortedPredictions[0]
        
        // Get top 3 predictions
        val top3 = sortedPredictions
            .take(3)
            .map { (index, confidence) ->
                val name = if (index < fishLabels.size) fishLabels[index] else "Unknown #$index"
                Prediction(name, confidence, index)
            }
        
        // High confidence: Show single result
        if (topConfidence >= HIGH_CONFIDENCE_THRESHOLD) {
            return ValidationResult.ValidFish(
                prediction = top3[0],
                alternates = top3.drop(1)
            )
        }
        
        // Medium confidence: Show multiple matches
        if (topConfidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return ValidationResult.MultipleMatches(
                matches = top3,
                topConfidence = topConfidence
            )
        }
        
        // Low confidence: Not recognized
        return ValidationResult.NotRecognized(
            reason = "Confidence too low for reliable identification",
            topConfidence = topConfidence,
            bestGuess = top3[0]
        )
    }
    
    
    // Live camera mode with temporal averaging
    fun detectFishLiveCamera(bitmap: Bitmap): ValidationResult {
        if (!isModelLoaded || interpreter == null) {
            return ValidationResult.Error("Model not loaded")
        }
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInferenceTime < INFERENCE_INTERVAL_MS) {
            return ValidationResult.Error("Throttled")
        }
        
        lastInferenceTime = currentTime
        val probs = runSingleInference(bitmap) ?: return ValidationResult.Error("Inference failed")
        val averaged = averagePredictions(probs)
        return validatePrediction(averaged)
    }
    

    fun detectFishValidated(bitmap: Bitmap, inferenceCount: Int = 5): ValidationResult {
        if (!isModelLoaded || interpreter == null) {
            return ValidationResult.Error("Model not loaded - Please add fish_model.tflite")
        }
        
        // Resize bitmap once to input size to avoid memory issues with large bitmaps during augmentation
        // This is critical for preventing OutOfMemoryError and native crashes
        val scaledBitmap = if (bitmap.width == inputWidth && bitmap.height == inputHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        }
        
        try {
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val outputSize = outputShape[1]
            
            
            val accumulatedProbs = FloatArray(outputSize) { 0f }
            var successfulInferences = 0
            
            
            val brightnessFactors = listOf(1.0f, 0.85f, 0.9f, 0.95f, 1.05f, 1.1f, 1.15f, 0.8f, 1.2f, 0.92f)  // Enhanced range
            
            for (i in 0 until min(inferenceCount, brightnessFactors.size)) {
                val augmentedBitmap = if (i == 0) {
                    scaledBitmap 
                } else {
                    adjustBrightness(scaledBitmap, brightnessFactors[i])
                }
                
                val probs = runSingleInference(augmentedBitmap)
                
                
                if (i != 0) {
                    augmentedBitmap.recycle()
                }
                
                if (probs != null) {
                    for (j in probs.indices) {
                        accumulatedProbs[j] += probs[j]
                    }
                    successfulInferences++
                }
            }
            
            if (successfulInferences == 0) {
                return ValidationResult.Error("Inference failed")
            }
            
            
            for (i in accumulatedProbs.indices) {
                accumulatedProbs[i] /= successfulInferences
            }
            
            
            return validatePrediction(accumulatedProbs)
            
        } catch (e: Exception) {
            println("Error during validated inference: ${e.message}")
            e.printStackTrace()
            return ValidationResult.Error(e.message ?: "Unknown error")
        } finally {
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
        }
    }
    

    fun getFishIndex(fishName: String): Int {
        return fishLabels.indexOfFirst { it.equals(fishName, ignoreCase = true) }
    }
    

    fun getFishName(index: Int): String? {
        return if (index in fishLabels.indices) fishLabels[index] else null
    }
    

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
