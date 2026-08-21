package com.example.moodlens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class AppTab(val title: String) {
    CAMERA("Camera"),
    JOURNAL("Journal"),
    SUMMARY("Summary")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule daily check-in reminder background job (8:00 PM)
        CheckInReminderWorker.scheduleDailyReminder(this)

        setContent {
            MaterialTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.CAMERA) }

    // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.CAMERA,
                        onClick = { selectedTab = AppTab.CAMERA },
                        icon = { Icon(Icons.Default.Face, contentDescription = "Camera") },
                        label = { Text("Detect") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.JOURNAL,
                        onClick = { selectedTab = AppTab.JOURNAL },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Journal") },
                        label = { Text("Journal") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.SUMMARY,
                        onClick = { selectedTab = AppTab.SUMMARY },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Daily Summary") },
                        label = { Text("Summary") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    AppTab.CAMERA -> CameraPreviewScreen()
                    AppTab.JOURNAL -> JournalScreen()
                    AppTab.SUMMARY -> DailySummaryScreen()
                }
            }
        }
    }
}
