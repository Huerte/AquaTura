package com.example.aquatura.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import java.io.ByteArrayOutputStream
import kotlin.math.min

object BitmapUtils {
    
    private const val MAX_IMAGE_DIMENSION = 2048
    private const val MAX_FILE_SIZE_MB = 4
    private const val JPEG_QUALITY = 85
    
    fun loadOptimizedBitmap(context: Context, uri: Uri, maxDimension: Int = MAX_IMAGE_DIMENSION): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
            
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            println("Error loading bitmap: ${e.message}")
            null
        } catch (e: OutOfMemoryError) {
            println("Out of memory loading bitmap")
            null
        }
    }
    
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    fun compressBitmap(bitmap: Bitmap, maxSizeMB: Int = MAX_FILE_SIZE_MB): Bitmap {
        var quality = JPEG_QUALITY
        var outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        
        val maxSizeBytes = maxSizeMB * 1024 * 1024
        
        while (outputStream.toByteArray().size > maxSizeBytes && quality > 10) {
            outputStream = ByteArrayOutputStream()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }
        
        if (outputStream.toByteArray().size > maxSizeBytes) {
            val scaleFactor = kotlin.math.sqrt(maxSizeBytes.toDouble() / outputStream.toByteArray().size.toDouble())
            val newWidth = (bitmap.width * scaleFactor).toInt()
            val newHeight = (bitmap.height * scaleFactor).toInt()
            
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
        
        return bitmap
    }
    
    fun getOptimalDimensions(originalWidth: Int, originalHeight: Int, maxDimension: Int = MAX_IMAGE_DIMENSION): Pair<Int, Int> {
        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return originalWidth to originalHeight
        }
        
        val scale = min(
            maxDimension.toFloat() / originalWidth,
            maxDimension.toFloat() / originalHeight
        )
        
        return (originalWidth * scale).toInt() to (originalHeight * scale).toInt()
    }
    
    fun estimateBitmapSize(width: Int, height: Int): Long {
        return (width * height * 4L)
    }
    
    fun canLoadBitmap(width: Int, height: Int): Boolean {
        val estimatedSize = estimateBitmapSize(width, height)
        val maxMemory = Runtime.getRuntime().maxMemory()
        val usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val availableMemory = maxMemory - usedMemory
        
        return estimatedSize < availableMemory * 0.3
    }
    
    fun createBlurredBackground(bitmap: Bitmap): Bitmap {
        val scale = 0.05f // Downscale to 5%
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
