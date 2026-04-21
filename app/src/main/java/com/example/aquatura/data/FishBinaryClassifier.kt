package com.example.aquatura.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min


data class BinaryClassificationResult(
    val isFish: Boolean,
    val confidence: Float    
)

class FishBinaryClassifier(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val modelFileName = "fish_identifier.tflite"
    
    private var isModelLoaded = false
    
    
    private val inputWidth = 224
    private val inputHeight = 224
    private val inputChannels = 3
    
    
    private val CLASS_NOT_FISH = 0
    private val CLASS_FISH = 1
    private val FISH_CONFIDENCE_THRESHOLD = 0.70f  // Lowered from 0.87f for fewer false rejections
    
    init {
        loadModel()
    }
    
    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile(modelFileName)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            
            // Validate dimensions to prevent runtime errors
            val inputTensor = interpreter!!.getInputTensor(0)
            val outputTensor = interpreter!!.getOutputTensor(0)
            println("FishBinaryClassifier loaded. Input: ${inputTensor.shape().contentToString()}, Output: ${outputTensor.shape().contentToString()}")
            
            isModelLoaded = true
            println("Fish identifier model loaded successfully")
        } catch (e: Exception) {
            println("Error loading fish identifier model: ${e.message}")
            isModelLoaded = false
        }
    }

    fun isFish(bitmap: Bitmap): Boolean {
        return classify(bitmap).isFish
    }
    
    fun classify(bitmap: Bitmap): BinaryClassificationResult {
        if (!isModelLoaded || interpreter == null) {
            
            println("Fish identifier model not loaded - defaulting to fish")
            return BinaryClassificationResult(isFish = true, confidence = 0.0f)
        }
        
        try {
            
            val inputBuffer = preprocessBitmap(bitmap)
            
            
            val outputBuffer = ByteBuffer.allocateDirect(4 * 2) 
            outputBuffer.order(ByteOrder.nativeOrder())
            
            
            interpreter!!.run(inputBuffer, outputBuffer)
            
            
            outputBuffer.rewind()
            val probabilities = FloatArray(2)
            outputBuffer.asFloatBuffer().get(probabilities)
            
            
            val isFish = probabilities[CLASS_FISH] > FISH_CONFIDENCE_THRESHOLD
            val confidence = if (isFish) probabilities[CLASS_FISH] else probabilities[CLASS_NOT_FISH]
            
            println("Fish identifier result: isFish=$isFish, confidence=$confidence")
            println("  Not-Fish prob: ${probabilities[CLASS_NOT_FISH]}, Fish prob: ${probabilities[CLASS_FISH]}")
            
            return BinaryClassificationResult(isFish = isFish, confidence = confidence)
            
        } catch (e: Exception) {
            println("Error during fish identification: ${e.message}")
            e.printStackTrace()
            
            return BinaryClassificationResult(isFish = true, confidence = 0.0f)
        }
    }
    
    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        
        
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
        
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        
        byteBuffer.rewind()
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
            
            val outputBuffer = ByteBuffer.allocateDirect(4 * 2)
            outputBuffer.order(ByteOrder.nativeOrder())
            
            interpreter!!.run(inputBuffer, outputBuffer)
            
            outputBuffer.rewind()
            val probabilities = FloatArray(2)
            outputBuffer.asFloatBuffer().get(probabilities)
            
            return probabilities
        } catch (e: Exception) {
            println("Error during single inference: ${e.message}")
            return null
        }
    }

    fun classifyWithMultiInference(bitmap: Bitmap, inferenceCount: Int = 3): BinaryClassificationResult {
        if (!isModelLoaded || interpreter == null) {
            println("Fish identifier model not loaded - defaulting to fish")
            return BinaryClassificationResult(isFish = true, confidence = 0.0f)
        }
        
        // Resize bitmap once to input size to avoid memory issues with large bitmaps during augmentation
        val scaledBitmap = if (bitmap.width == inputWidth && bitmap.height == inputHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        }
        
        try {
            val accumulatedProbs = FloatArray(2) { 0f }
            var successfulInferences = 0
            
            val brightnessFactors = listOf(1.0f, 0.85f, 0.9f, 1.1f, 1.15f)  // Expanded from 3 to 5 passes
            
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
                return BinaryClassificationResult(isFish = true, confidence = 0.0f)
            }
            
            for (i in accumulatedProbs.indices) {
                accumulatedProbs[i] /= successfulInferences
            }
            
            val isFish = accumulatedProbs[CLASS_FISH] > FISH_CONFIDENCE_THRESHOLD
            val confidence = if (isFish) accumulatedProbs[CLASS_FISH] else accumulatedProbs[CLASS_NOT_FISH]
            
            println("Fish identifier multi-inference result: isFish=$isFish, confidence=$confidence")
            println("  Averaged Not-Fish prob: ${accumulatedProbs[CLASS_NOT_FISH]}, Fish prob: ${accumulatedProbs[CLASS_FISH]}")
            
            return BinaryClassificationResult(isFish = isFish, confidence = confidence)
            
        } catch (e: Exception) {
            println("Error during multi-inference fish identification: ${e.message}")
            e.printStackTrace()
            return BinaryClassificationResult(isFish = true, confidence = 0.0f)
        } finally {
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
        }
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
