package com.example.moodlens

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

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
 * ImageAnalysis.Analyzer that runs ML Kit face detection on each camera frame,
 * then crops, preprocesses, and classifies emotions using the TFLite model.
 *
 * Includes frame throttling (skips frames while previous is still processing)
 * and performance timing logged to Logcat.
 */
class FrameAnalyzer(context: Context) : ImageAnalysis.Analyzer {

    private val _detectedFaces = MutableStateFlow<List<DetectedFace>>(emptyList())
    val detectedFaces: StateFlow<List<DetectedFace>> = _detectedFaces.asStateFlow()

    private val _emotionResult = MutableStateFlow<EmotionResult?>(null)
    val emotionResult: StateFlow<EmotionResult?> = _emotionResult.asStateFlow()

    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val detector: FaceDetector
    private val emotionClassifier: EmotionClassifier?

    /** Frame throttle: skip frame if previous analysis is still in-flight. */
    private val isProcessing = AtomicBoolean(false)

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.2f)
            .build()

        detector = FaceDetection.getClient(options)
        emotionClassifier = EmotionClassifier.create(context)

        if (emotionClassifier == null) {
            Log.w(TAG, "EmotionClassifier failed to initialize — emotion detection disabled")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // Frame throttling: drop frame if still processing the previous one
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val frameStartMs = System.currentTimeMillis()

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing.set(false)
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val detectionEndMs = System.currentTimeMillis()
                val detectionMs = detectionEndMs - frameStartMs

                val detectedList = faces.map { face ->
                    DetectedFace(
                        boundingBox = face.boundingBox,
                        imageWidth = imageProxy.width,
                        imageHeight = imageProxy.height,
                        rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    )
                }
                _detectedFaces.value = detectedList

                // Classify emotion for the first (largest/closest) detected face
                if (faces.isNotEmpty() && emotionClassifier != null) {
                    try {
                        val classifyStartMs = System.currentTimeMillis()

                        val bitmap = Preprocessing.imageProxyToBitmap(imageProxy)
                        val faceBoundingBox = faces[0].boundingBox
                        val croppedFace = Preprocessing.cropFace(bitmap, faceBoundingBox)
                        val inputBuffer = Preprocessing.preprocessToByteBuffer(croppedFace)
                        val result = emotionClassifier.classify(inputBuffer)
                        _emotionResult.value = result

                        val classifyEndMs = System.currentTimeMillis()
                        val totalMs = classifyEndMs - frameStartMs
                        _latencyMs.value = totalMs

                        Log.d(
                            TAG,
                            "Frame pipeline: detection=${detectionMs}ms, " +
                                    "classify=${classifyEndMs - classifyStartMs}ms, " +
                                    "total=${totalMs}ms | " +
                                    "${result.label} (${(result.confidence * 100).toInt()}%)"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Emotion classification failed", e)
                        _emotionResult.value = null
                    }
                } else {
                    _emotionResult.value = null
                    if (faces.isEmpty()) {
                        Log.d(TAG, "No face detected (detection=${detectionMs}ms)")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                _detectedFaces.value = emptyList()
                _emotionResult.value = null
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing.set(false)
            }
    }

    fun close() {
        detector.close()
        emotionClassifier?.close()
    }

    companion object {
        private const val TAG = "FrameAnalyzer"
    }
}
