package com.example.aquatura.utils

import android.graphics.Bitmap
import android.graphics.Color

data class ImageQualityResult(
    val isGoodQuality: Boolean,
    val brightness: Float,
    val blurScore: Double,
    val message: String
)

object ImageQualityChecker {
    
    private const val MIN_BRIGHTNESS = 30f
    private const val MIN_BLUR_THRESHOLD = 100.0
    
    fun checkQuality(bitmap: Bitmap): ImageQualityResult {
        val brightness = calculateBrightness(bitmap)
        val blurScore = calculateBlurScore(bitmap)
        
        val isTooDark = brightness < MIN_BRIGHTNESS
        val isTooBlurry = blurScore < MIN_BLUR_THRESHOLD
        
        val message = when {
            isTooDark && isTooBlurry -> "Image is too dark and blurry. Please use better lighting and hold phone steady."
            isTooDark -> "Image is too dark. Please turn on lights or use flash."
            isTooBlurry -> "Image is too blurry. Please hold phone steady and tap to focus."
            else -> "Image quality is good"
        }
        
        return ImageQualityResult(
            isGoodQuality = !isTooDark && !isTooBlurry,
            brightness = brightness,
            blurScore = blurScore,
            message = message
        )
    }
    
    private fun calculateBrightness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        var totalBrightness = 0L
        var pixelCount = 0
        
        val sampleStep = maxOf(1, width / 50)
        
        for (x in 0 until width step sampleStep) {
            for (y in 0 until height step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                
                val brightness = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                totalBrightness += brightness
                pixelCount++
            }
        }
        
        return if (pixelCount > 0) totalBrightness.toFloat() / pixelCount else 0f
    }
    
    private fun calculateBlurScore(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        
        val sampleWidth = minOf(width, 400)
        val sampleHeight = minOf(height, 400)
        val sampledBitmap = if (width > sampleWidth || height > sampleHeight) {
            Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)
        } else {
            bitmap
        }
        
        val grayValues = Array(sampleHeight) { IntArray(sampleWidth) }
        
        for (y in 0 until sampleHeight) {
            for (x in 0 until sampleWidth) {
                val pixel = sampledBitmap.getPixel(x, y)
                val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                grayValues[y][x] = gray
            }
        }
        
        var variance = 0.0
        var count = 0
        
        for (y in 1 until sampleHeight - 1) {
            for (x in 1 until sampleWidth - 1) {
                val laplacian = kotlin.math.abs(
                    -grayValues[y-1][x-1] - grayValues[y-1][x] - grayValues[y-1][x+1] -
                    grayValues[y][x-1] + 8 * grayValues[y][x] - grayValues[y][x+1] -
                    grayValues[y+1][x-1] - grayValues[y+1][x] - grayValues[y+1][x+1]
                )
                variance += laplacian * laplacian
                count++
            }
        }
        
        if (sampledBitmap != bitmap) {
            sampledBitmap.recycle()
        }
        
        return if (count > 0) variance / count else 0.0
    }
}
