package com.example.moodlens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodlens.data.StreakData

/**
 * Aesthetic pill-shaped badge displaying the user's active check-in streak.
 */
@Composable
fun StreakBadge(
    streakData: StreakData,
    modifier: Modifier = Modifier,
    showDetailsOnClick: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    val isActive = streakData.currentStreak > 0
    val flameBrush = Brush.horizontalGradient(
        colors = if (isActive) {
            listOf(Color(0xFFFF6F00), Color(0xFFFF8F00), Color(0xFFFFB300))
        } else {
            listOf(Color(0xFF757575), Color(0xFF9E9E9E))
        }
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = showDetailsOnClick) { showDialog = true },
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(brush = flameBrush, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak Flame",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "${streakData.currentStreak} ${if (streakData.currentStreak == 1) "Day" else "Days"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog && showDetailsOnClick) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check-In Streak")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Current Streak: ${streakData.currentStreak} ${if (streakData.currentStreak == 1) "day" else "days"}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personal Best: ${streakData.bestStreak} ${if (streakData.bestStreak == 1) "day" else "days"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Text(
                        text = "Keep your momentum! Record at least one mood check-in each day to maintain and grow your streak.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
