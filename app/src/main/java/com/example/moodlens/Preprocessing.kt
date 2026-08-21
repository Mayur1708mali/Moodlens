package com.example.moodlens

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Preprocessing utilities for the emotion classification model.
 *
 * Pipeline: ImageProxy + face bounding box → crop face → grayscale → resize 48x48 → normalize to [0,1]
 */
object Preprocessing {

    const val MODEL_INPUT_SIZE = 48

    /**
     * Converts an ImageProxy (YUV_420_888) to a full-frame Bitmap.
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val jpegBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    /**
     * Crops the face region from the bitmap using the bounding box.
     * Clamps coordinates to bitmap bounds to avoid crashes.
     */
    fun cropFace(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        val left = boundingBox.left.coerceIn(0, bitmap.width - 1)
        val top = boundingBox.top.coerceIn(0, bitmap.height - 1)
        val right = boundingBox.right.coerceIn(left + 1, bitmap.width)
        val bottom = boundingBox.bottom.coerceIn(top + 1, bitmap.height)

        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    /**
     * Converts a Bitmap to grayscale, resizes to 48x48, and normalizes pixel
     * values to [0, 1]. Returns a ByteBuffer ready for TFLite input.
     *
     * Output shape: [1, 48, 48, 1] as float32.
     */
    fun preprocessToByteBuffer(faceBitmap: Bitmap): ByteBuffer {
        // Resize to model input size
        val resized = Bitmap.createScaledBitmap(faceBitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)

        // Allocate buffer: 1 * 48 * 48 * 1 * 4 bytes (float32)
        val byteBuffer = ByteBuffer.allocateDirect(1 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 1 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        resized.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)

        for (pixel in pixels) {
            // Convert to grayscale using luminance formula
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val gray = (0.299f * r + 0.587f * g + 0.114f * b)

            // Normalize to [0, 1]
            byteBuffer.putFloat(gray / 255.0f)
        }

        byteBuffer.rewind()
        return byteBuffer
    }
}
