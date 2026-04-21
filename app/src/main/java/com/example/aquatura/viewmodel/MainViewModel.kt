package com.example.aquatura.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.aquatura.data.FishDetector
import com.example.aquatura.data.Prediction
import com.example.aquatura.data.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DetectionState {
    object Idle : DetectionState()
    object Loading : DetectionState()
    data class Success(
        val prediction: Prediction,
        val alternates: List<Prediction> = emptyList()
    ) : DetectionState()
    data class MultipleMatches(
        val matches: List<Prediction>
    ) : DetectionState()
    data class NotAFish(val reason: String) : DetectionState()
    data class UnknownSpecies(
        val reason: String,
        val topConfidence: Float,
        val bestGuess: Prediction? = null
    ) : DetectionState()
    data class Error(val message: String) : DetectionState()
}


class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val fishDetector = FishDetector(application.applicationContext)
    private val fishBinaryClassifier = com.example.aquatura.data.FishBinaryClassifier(application.applicationContext)
    
    
    private val _detectionState = MutableLiveData<DetectionState>(DetectionState.Idle)
    val detectionState: LiveData<DetectionState> = _detectionState
    
    
    private val _topPredictions = MutableLiveData<List<Prediction>>()
    val topPredictions: LiveData<List<Prediction>> = _topPredictions
    

    fun processImage(imageProxy: ImageProxy) {
        viewModelScope.launch {
            try {
                _detectionState.value = DetectionState.Loading
                
                val bitmap: Bitmap? = withContext(Dispatchers.Default) {
                    imageProxyToBitmap(imageProxy)
                }
                
                if (bitmap == null) {
                    _detectionState.value = DetectionState.Error("Failed to convert image")
                    imageProxy.close()
                    return@launch
                }
                
                val binaryResult = withContext(Dispatchers.Default) {
                    fishBinaryClassifier.classifyWithMultiInference(bitmap, 5)  // Increased from 3 to 5
                }
                
                if (!binaryResult.isFish) {
                    _detectionState.value = DetectionState.NotAFish("Object classified as non-fish")
                    bitmap.recycle()
                    imageProxy.close()
                    return@launch
                }
                
                val speciesResult = withContext(Dispatchers.Default) {
                    fishDetector.detectFishValidated(bitmap, 10)  // Increased from 5 to 10
                }
                
                when (speciesResult) {
                    is ValidationResult.ValidFish -> {
                        _detectionState.value = DetectionState.Success(
                            speciesResult.prediction,
                            speciesResult.alternates
                        )
                        _topPredictions.value = listOf(speciesResult.prediction) + speciesResult.alternates
                    }
                    is ValidationResult.MultipleMatches -> {
                        _detectionState.value = DetectionState.MultipleMatches(
                            speciesResult.matches
                        )
                        _topPredictions.value = speciesResult.matches
                    }
                    is ValidationResult.NotRecognized -> {
                        _detectionState.value = DetectionState.UnknownSpecies(
                            speciesResult.reason,
                            speciesResult.topConfidence,
                            speciesResult.bestGuess
                        )
                    }
                    is ValidationResult.Error -> {
                        _detectionState.value = DetectionState.Error(speciesResult.message)
                    }
                }
                
                bitmap.recycle()
                imageProxy.close()
                
            } catch (e: Exception) {
                _detectionState.value = DetectionState.Error(e.message ?: "Unknown error")
                imageProxy.close()
            }
        }
    }

    

    fun processBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                
                _detectionState.value = DetectionState.Loading
                
                
                val result = withContext(Dispatchers.Default) {
                    fishDetector.detectFish(bitmap)
                }
                
                
                if (result != null) {
                    _detectionState.value = DetectionState.Success(result)
                } else {
                    _detectionState.value = DetectionState.Error("Detection failed")
                }
                
            } catch (e: Exception) {
                _detectionState.value = DetectionState.Error(e.message ?: "Unknown error")
            }
        }
    }
    fun processBitmapValidated(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _detectionState.value = DetectionState.Loading
                
                withContext(Dispatchers.Default) {
                    val binaryResult = fishBinaryClassifier.classifyWithMultiInference(bitmap, 5)  // Increased from 3 to 5
                    
                    if (!binaryResult.isFish) {
                        withContext(Dispatchers.Main) {
                            _detectionState.value = DetectionState.NotAFish("Object classified as non-fish")
                        }
                    } else {
                        val speciesResult = fishDetector.detectFishValidated(bitmap, 10)  // Increased from 5 to 10
                        
                        withContext(Dispatchers.Main) {
                            when (speciesResult) {
                                is ValidationResult.ValidFish -> {
                                    _detectionState.value = DetectionState.Success(
                                        speciesResult.prediction,
                                        speciesResult.alternates
                                    )
                                    _topPredictions.value = listOf(speciesResult.prediction) + speciesResult.alternates
                                }
                                is ValidationResult.MultipleMatches -> {
                                    _detectionState.value = DetectionState.MultipleMatches(
                                        speciesResult.matches
                                    )
                                    _topPredictions.value = speciesResult.matches
                                }
                                is ValidationResult.NotRecognized -> {
                                    _detectionState.value = DetectionState.UnknownSpecies(
                                        speciesResult.reason,
                                        speciesResult.topConfidence,
                                        speciesResult.bestGuess
                                    )
                                }
                                is ValidationResult.Error -> {
                                    _detectionState.value = DetectionState.Error(speciesResult.message)
                                }
                            }
                        }
                    }
                }
                
                bitmap.recycle()
                
            } catch (e: Exception) {
                _detectionState.value = DetectionState.Error(e.message ?: "Unknown error")
            }
        }
    }


    fun processBitmapHighAccuracy(bitmap: Bitmap, inferenceCount: Int = 5) {
        viewModelScope.launch {
            try {
                
                _detectionState.value = DetectionState.Loading
                
                
                val result = withContext(Dispatchers.Default) {
                    fishDetector.detectFishHighAccuracy(bitmap, inferenceCount)
                }
                
                
                if (result != null) {
                    _detectionState.value = DetectionState.Success(result)
                } else {
                    _detectionState.value = DetectionState.Error("Detection failed")
                }
                
            } catch (e: Exception) {
                _detectionState.value = DetectionState.Error(e.message ?: "Unknown error")
            }
        }
    }
    

    fun processImageTopN(bitmap: Bitmap, topN: Int = 3) {
        viewModelScope.launch {
            try {
                
                _detectionState.value = DetectionState.Loading
                
                
                val results = withContext(Dispatchers.Default) {
                    fishDetector.detectFishTopN(bitmap, topN)
                }
                
                if (results.isNotEmpty()) {
                    
                    _detectionState.value = DetectionState.Success(results.first())
                    
                    _topPredictions.value = results
                } else {
                    _detectionState.value = DetectionState.Error("Detection failed")
                }
                
            } catch (e: Exception) {
                _detectionState.value = DetectionState.Error(e.message ?: "Unknown error")
            }
        }
    }
    

    fun processImageTopNHighAccuracy(bitmap: Bitmap, topN: Int = 3, inferenceCount: Int = 5) {
        viewModelScope.launch {
            try {
                
                _detectionState.value = DetectionState.Loading
                
                
                val results = withContext(Dispatchers.Default) {
                    fishDetector.detectFishTopNHighAccuracy(bitmap, topN, inferenceCount)
                }
                
                if (results.isNotEmpty()) {
                    
                    _detectionState.value = DetectionState.Success(results.first())
                    
                    _topPredictions.value = results
                } else {
                    _detectionState.value = DetectionState.Error("Detection failed")
                }
                
            } catch (e: Exception) {
                _detectionState.value = DetectionState.Error(e.message ?: "Unknown error")
            }
        }
    }
    

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            
            
            
            
        } catch (e: Exception) {
            println("Error converting ImageProxy to Bitmap: ${e.message}")
            null
        }
    }
    

    fun resetState() {
        _detectionState.value = DetectionState.Idle
        _topPredictions.value = emptyList()
    }
    

    fun isModelReady(): Boolean = fishDetector.isReady()
    

    fun getSupportedSpeciesCount(): Int = fishDetector.getClassCount()
    
    
    fun resetPredictionBuffer() {
        fishDetector.resetPredictionBuffer()
    }
    

    override fun onCleared() {
        super.onCleared()
        fishDetector.close()
        fishBinaryClassifier.close()
    }
}

