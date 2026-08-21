package com.example.moodlens

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.moodlens.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data class Success(val sessionId: Long) : SaveState
    data class Error(val message: String) : SaveState
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SessionRepository.getInstance(application)

    private val _isCameraBound = MutableStateFlow(false)
    val isCameraBound: StateFlow<Boolean> = _isCameraBound.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private var imageAnalysis: ImageAnalysis? = null

    fun saveCurrentMood(
        emotionResult: EmotionResult,
        faceBitmap: Bitmap?,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val id = repository.saveSession(
                    emotionLabel = emotionResult.label,
                    confidence = emotionResult.confidence,
                    faceBitmap = faceBitmap,
                    notes = notes
                )
                _saveState.value = SaveState.Success(id)
                Log.i(TAG, "Mood entry saved successfully with ID: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save mood entry", e)
                _saveState.value = SaveState.Error(e.message ?: "Failed to save mood")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                _isCameraBound.value = true
                Log.i(TAG, "Camera bound successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                _isCameraBound.value = false
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    override fun onCleared() {
        super.onCleared()
        analysisExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraViewModel"
    }
}
