package com.qrbarcode.scanner.ui.scanner

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.qrbarcode.scanner.ui.components.PrivacyDialog
import com.qrbarcode.scanner.ui.components.ScanResultBottomSheet
import com.qrbarcode.scanner.ui.theme.ScanCornerColor
import com.qrbarcode.scanner.ui.theme.ScanLineColor
import com.qrbarcode.scanner.ui.theme.ScanOverlayColor
import com.qrbarcode.scanner.util.BarcodeAnalyzer
import com.qrbarcode.scanner.util.HapticHelper
import com.qrbarcode.scanner.util.SoundHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { scanImageFromGallery(context, it, viewModel) }
    }

    // Vibrate + beep on successful scan
    LaunchedEffect(uiState.showResult) {
        if (uiState.showResult) {
            HapticHelper.vibrate(context)
            if (uiState.soundEnabled) SoundHelper.playBeep(context)
        }
    }

    // Flashlight control
    LaunchedEffect(uiState.flashlightOn) {
        camera?.cameraControl?.enableTorch(uiState.flashlightOn)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            SoundHelper.release()
        }
    }

    // Privacy dialog (shown once on first launch)
    if (uiState.showPrivacyDialog) {
        PrivacyDialog(onDismiss = { viewModel.dismissPrivacyDialog() })
    }

    if (!cameraPermission.status.isGranted) {
        CameraPermissionScreen(
            shouldShowRationale = cameraPermission.status.shouldShowRationale,
            onRequestPermission = { cameraPermission.launchPermissionRequest() }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes ->
                                    viewModel.onBarcodeDetected(barcodes)
                                })
                            }
                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalyzer
                            )
                        } catch (e: Exception) {
                            Log.e("ScannerScreen", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dark overlay + scanner frame
        ScannerViewfinderOverlay(modifier = Modifier.fillMaxSize())

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QR & Barcode Scanner",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            IconButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode chips
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScannerChip(
                    selected = uiState.continuousScan,
                    label = "Continuous",
                    onClick = { viewModel.toggleContinuousScan() }
                )
                ScannerChip(
                    selected = uiState.soundEnabled,
                    label = "Sound",
                    onClick = { viewModel.toggleSound() }
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScannerIconButton(
                    icon = Icons.Default.PhotoLibrary,
                    label = "Gallery",
                    active = false,
                    onClick = { galleryLauncher.launch("image/*") }
                )
                ScannerIconButton(
                    icon = if (uiState.flashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    label = "Flash",
                    active = uiState.flashlightOn,
                    onClick = { viewModel.toggleFlashlight() }
                )
            }
        }

        // Result bottom sheet
        if (uiState.showResult && uiState.lastScan != null) {
            ScanResultBottomSheet(
                scan = uiState.lastScan!!,
                onDismiss = { viewModel.dismissResult() },
                onScanAgain = { viewModel.resumeScanning() }
            )
        }

        // No barcode found snackbar
        if (uiState.noCodeFound) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2500)
                viewModel.dismissNoCodeFound()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp, start = 16.dp, end = 16.dp)
            ) {
                Snackbar {
                    Text("No barcode found in that image")
                }
            }
        }
    }
}

@Composable
private fun ScannerViewfinderOverlay(modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_y"
    )

    Canvas(modifier = modifier) {
        val frameSize = minOf(size.width, size.height) * 0.7f
        val frameLeft = (size.width - frameSize) / 2f
        val frameTop = (size.height - frameSize) / 2f
        val frameRight = frameLeft + frameSize
        val frameBottom = frameTop + frameSize
        val cornerLength = frameSize * 0.1f
        val cornerRadius = 16.dp.toPx()
        val strokeWidth = 3.dp.toPx()

        // Dark overlay as 4 rectangles surrounding the clear viewfinder area
        drawRect(color = ScanOverlayColor, topLeft = Offset.Zero, size = Size(size.width, frameTop))
        drawRect(color = ScanOverlayColor, topLeft = Offset(0f, frameBottom), size = Size(size.width, size.height - frameBottom))
        drawRect(color = ScanOverlayColor, topLeft = Offset(0f, frameTop), size = Size(frameLeft, frameSize))
        drawRect(color = ScanOverlayColor, topLeft = Offset(frameRight, frameTop), size = Size(size.width - frameRight, frameSize))

        // Corner brackets
        val corners = listOf(
            // Top-left
            listOf(
                Offset(frameLeft, frameTop + cornerLength) to Offset(frameLeft, frameTop + cornerRadius),
                Offset(frameLeft, frameTop) to Offset(frameLeft + cornerLength, frameTop)
            ),
            // Top-right
            listOf(
                Offset(frameRight, frameTop + cornerLength) to Offset(frameRight, frameTop + cornerRadius),
                Offset(frameRight, frameTop) to Offset(frameRight - cornerLength, frameTop)
            ),
            // Bottom-left
            listOf(
                Offset(frameLeft, frameBottom - cornerLength) to Offset(frameLeft, frameBottom - cornerRadius),
                Offset(frameLeft, frameBottom) to Offset(frameLeft + cornerLength, frameBottom)
            ),
            // Bottom-right
            listOf(
                Offset(frameRight, frameBottom - cornerLength) to Offset(frameRight, frameBottom - cornerRadius),
                Offset(frameRight, frameBottom) to Offset(frameRight - cornerLength, frameBottom)
            )
        )

        for (corner in corners) {
            for ((start, end) in corner) {
                drawLine(
                    color = ScanCornerColor,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth * 2,
                    cap = StrokeCap.Round
                )
            }
        }

        // Scanning line
        val lineY = frameTop + (frameSize * scanLineY)
        drawLine(
            color = ScanLineColor.copy(alpha = 0.8f),
            start = Offset(frameLeft + 8.dp.toPx(), lineY),
            end = Offset(frameRight - 8.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ScannerChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun ScannerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else Color.Black.copy(alpha = 0.5f),
                    CircleShape
                )
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CameraPermissionScreen(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Camera Permission Required",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (shouldShowRationale)
                    "Camera access is needed to scan QR codes and barcodes. No images are stored or transmitted."
                else
                    "Please grant camera permission to use the scanner.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission) {
                Text("Grant Camera Permission")
            }
        }
    }
}

private fun scanImageFromGallery(context: Context, uri: Uri, viewModel: ScannerViewModel) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    viewModel.onBarcodeDetected(barcodes)
                } else {
                    viewModel.onGalleryNoBarcodeFound()
                }
            }
            .addOnFailureListener {
                viewModel.onGalleryNoBarcodeFound()
            }
    } catch (e: Exception) {
        viewModel.onGalleryNoBarcodeFound()
    }
}
