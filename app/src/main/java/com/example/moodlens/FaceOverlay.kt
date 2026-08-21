package com.example.moodlens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws bounding boxes over detected faces.
 *
 * Coordinates from ML Kit are in the image's coordinate system, so we scale
 * them to the composable's actual size. For front camera, the image is mirrored
 * so we flip the X axis.
 */
@Composable
fun FaceOverlay(
    faces: List<DetectedFace>,
    isFrontCamera: Boolean = true
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (face in faces) {
            // Determine the source image dimensions after rotation is applied.
            // When rotation is 90 or 270, width and height are swapped.
            val sourceWidth: Float
            val sourceHeight: Float
            if (face.rotationDegrees == 90 || face.rotationDegrees == 270) {
                sourceWidth = face.imageHeight.toFloat()
                sourceHeight = face.imageWidth.toFloat()
            } else {
                sourceWidth = face.imageWidth.toFloat()
                sourceHeight = face.imageHeight.toFloat()
            }

            val scaleX = canvasWidth / sourceWidth
            val scaleY = canvasHeight / sourceHeight

            val box = face.boundingBox

            // Scale bounding box to canvas
            var left = box.left * scaleX
            var right = box.right * scaleX
            val top = box.top * scaleY
            val bottom = box.bottom * scaleY

            // Mirror for front camera
            if (isFrontCamera) {
                val mirroredLeft = canvasWidth - right
                val mirroredRight = canvasWidth - left
                left = mirroredLeft
                right = mirroredRight
            }

            drawRect(
                color = Color.Green,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f)
            )
        }
    }
}
