package com.example.moodlens

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

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
class EmotionClassifier private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>
) {

    /**
     * Runs inference on preprocessed input buffer.
     * Returns [EmotionResult] with top label, confidence, and all 8 scores.
     */
    fun classify(inputBuffer: ByteBuffer): EmotionResult {
        // Output: [1, 8]
        val outputArray = Array(1) { FloatArray(labels.size) }

        interpreter.run(inputBuffer, outputArray)

        val rawScores = outputArray[0]
        val probabilities = softmax(rawScores)

        // Log all probabilities for debugging
        val scoreLog = labels.zip(probabilities.toList()).joinToString { "${it.first}: %.3f".format(it.second) }
        Log.i(TAG, "Probabilities: $scoreLog")

        // Find top prediction
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val topLabel = labels[maxIndex]
        val topConfidence = probabilities[maxIndex]

        return EmotionResult(
            label = topLabel,
            confidence = topConfidence,
            allScores = probabilities
        )
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { exp(it - maxLogit) }
        val sumExpValues = expValues.sum()
        return expValues.map { it / sumExpValues }.toFloatArray()
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        private const val TAG = "EmotionClassifier"
        private const val MODEL_FILENAME = "emotion_model.tflite"
        private const val LABELS_FILENAME = "emotion_labels.txt"

        /**
         * Creates an EmotionClassifier, or returns null if initialization fails.
         */
        fun create(context: Context): EmotionClassifier? {
            return try {
                val appContext = context.applicationContext
                val modelBuffer = loadModelFile(appContext, MODEL_FILENAME)
                val options = Interpreter.Options().setNumThreads(2)
                val interpreter = Interpreter(modelBuffer, options)

                val labels = appContext.assets.open(LABELS_FILENAME).bufferedReader().readLines()
                    .filter { it.isNotBlank() }

                Log.i(TAG, "Model loaded successfully. Labels: $labels")
                EmotionClassifier(interpreter, labels)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize EmotionClassifier", e)
                null
            }
        }

        private fun loadModelFile(context: Context, filename: String = MODEL_FILENAME): MappedByteBuffer {
            val assetFileDescriptor = context.assets.openFd(filename)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }
}
