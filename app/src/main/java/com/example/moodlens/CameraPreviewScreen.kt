package com.example.moodlens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CameraPreviewScreen(
    cameraViewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        val frameAnalyzer = remember { FrameAnalyzer(context.applicationContext) }
        val detectedFaces by frameAnalyzer.detectedFaces.collectAsStateWithLifecycle()
        val emotionResult by frameAnalyzer.emotionResult.collectAsStateWithLifecycle()
        val latestCroppedFace by frameAnalyzer.latestCroppedFace.collectAsStateWithLifecycle()
        val saveState by cameraViewModel.saveState.collectAsStateWithLifecycle()

        DisposableEffect(Unit) {
            onDispose {
                frameAnalyzer.close()
            }
        }

        LaunchedEffect(saveState) {
            if (saveState is SaveState.Success || saveState is SaveState.Error) {
                kotlinx.coroutines.delay(2500)
                cameraViewModel.resetSaveState()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        cameraViewModel.bindCamera(
                            lifecycleOwner = lifecycleOwner,
                            previewView = previewView,
                            analyzer = frameAnalyzer
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Bounding box overlay
            FaceOverlay(
                faces = detectedFaces,
                isFrontCamera = true
            )

            // Emotion label overlay & save action
            emotionResult?.let { result ->
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Feedback banner
                    when (val state = saveState) {
                        is SaveState.Success -> {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .background(
                                        color = Color(0xFF2E7D32).copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "✓ Mood saved to Journal!",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        is SaveState.Error -> {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .background(
                                        color = Color(0xFFC62828).copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = state.message,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        else -> Unit
                    }

                    // Main Emotion & Save Card
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = result.label.replaceFirstChar { it.uppercase() },
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "%.1f%% confidence".format(result.confidence * 100),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (saveState !is SaveState.Saving) {
                                        cameraViewModel.saveCurrentMood(result, latestCroppedFace)
                                    }
                                },
                                enabled = saveState !is SaveState.Saving,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (saveState is SaveState.Saving) {
                                    Text("Saving...")
                                } else {
                                    Text("Save Check-In")
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Camera permission is required",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
