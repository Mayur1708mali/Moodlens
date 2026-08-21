package com.example.moodlens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodlens.data.SessionEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Returns a distinct aesthetic color for each emotion label.
 */
fun getEmotionColor(emotionLabel: String): Color {
    return when (emotionLabel.lowercase(Locale.ROOT)) {
        "happy" -> Color(0xFF2E7D32) // Emerald Green
        "surprise" -> Color(0xFFE65100) // Vibrant Orange
        "neutral" -> Color(0xFF455A64) // Slate Grey
        "sad" -> Color(0xFF1565C0) // Soft Indigo/Blue
        "angry" -> Color(0xFFC62828) // Deep Crimson
        "fear" -> Color(0xFF6A1B9A) // Purple
        "disgust" -> Color(0xFF00695C) // Deep Teal
        "contempt" -> Color(0xFFAD1457) // Magenta Pink
        else -> Color(0xFF37474F)
    }
}

/**
 * Formats a Unix epoch timestamp into a human-readable string.
 */
fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val sameDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).let {
        it.format(date) == it.format(now)
    }
    return if (sameDay) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        "Today, ${timeFormat.format(date)}"
    } else {
        val fullFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        fullFormat.format(date)
    }
}

@Composable
fun JournalItemCard(
    entry: SessionEntry,
    loadThumbnail: suspend (String?) -> Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbnailBitmap by remember(entry.thumbnailPath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(entry.thumbnailPath) {
        thumbnailBitmap = loadThumbnail(entry.thumbnailPath)
    }

    val emotionColor = getEmotionColor(entry.emotionLabel)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail or Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                thumbnailBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Face thumbnail for ${entry.emotionLabel}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                } ?: run {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(emotionColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.emotionLabel.take(1).uppercase(),
                            color = emotionColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Confidence badge overlay on thumbnail
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "%.0f%%".format(entry.confidence * 100),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(emotionColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.emotionLabel.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
