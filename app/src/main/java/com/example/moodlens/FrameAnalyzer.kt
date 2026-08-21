package com.example.moodlens

import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing a detected face's bounding box and image dimensions.
 */
data class DetectedFace(
    val boundingBox: Rect,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int
)

/**
 * ImageAnalysis.Analyzer that runs ML Kit face detection on each camera frame.
 * Emits detected faces via [detectedFaces] StateFlow.
 */
class FrameAnalyzer : ImageAnalysis.Analyzer {

    private val _detectedFaces = MutableStateFlow<List<DetectedFace>>(emptyList())
    val detectedFaces: StateFlow<List<DetectedFace>> = _detectedFaces.asStateFlow()

    private val detector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.2f)
            .build()

        detector = FaceDetection.getClient(options)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                _detectedFaces.value = faces.map { face ->
                    DetectedFace(
                        boundingBox = face.boundingBox,
                        imageWidth = imageProxy.width,
                        imageHeight = imageProxy.height,
                        rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                _detectedFaces.value = emptyList()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun close() {
        detector.close()
    }

    companion object {
        private const val TAG = "FrameAnalyzer"
    }
}
