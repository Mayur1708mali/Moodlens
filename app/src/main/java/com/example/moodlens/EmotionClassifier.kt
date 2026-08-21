package com.example.moodlens

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Result of emotion classification.
 */
data class EmotionResult(
    val label: String,
    val confidence: Float,
    val allScores: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmotionResult) return false
        return label == other.label && confidence == other.confidence && allScores.contentEquals(other.allScores)
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + allScores.contentHashCode()
        return result
    }
}

/**
 * Loads the emotion TFLite model and runs inference on preprocessed face tensors.
 *
 * Expected input: [1, 48, 48, 1] float32 ByteBuffer (grayscale, normalized [0,1])
 * Output: [1, 7] float32 — probability scores for 7 emotion classes.
 */
class EmotionClassifier(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        val modelBuffer = loadModelFile(context, MODEL_FILENAME)
        val options = Interpreter.Options().setNumThreads(2)
        interpreter = Interpreter(modelBuffer, options)

        labels = context.assets.open(LABELS_FILENAME).bufferedReader().readLines()
            .filter { it.isNotBlank() }

        Log.d(TAG, "Model loaded. Labels: $labels")
    }

    /**
     * Runs inference on preprocessed input buffer.
     * Returns [EmotionResult] with top label, confidence, and all 7 scores.
     */
    fun classify(inputBuffer: ByteBuffer): EmotionResult {
        // Output: [1, 7]
        val outputArray = Array(1) { FloatArray(labels.size) }

        interpreter.run(inputBuffer, outputArray)

        val scores = outputArray[0]

        // Log all scores for debugging
        val scoreLog = labels.zip(scores.toList()).joinToString { "${it.first}: %.3f".format(it.second) }
        Log.d(TAG, "Scores: $scoreLog")

        // Find top prediction
        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
        val topLabel = labels[maxIndex]
        val topConfidence = scores[maxIndex]

        return EmotionResult(
            label = topLabel,
            confidence = topConfidence,
            allScores = scores
        )
    }

    fun close() {
        interpreter.close()
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    companion object {
        private const val TAG = "EmotionClassifier"
        private const val MODEL_FILENAME = "emotion_model.tflite"
        private const val LABELS_FILENAME = "emotion_labels.txt"
    }
}
