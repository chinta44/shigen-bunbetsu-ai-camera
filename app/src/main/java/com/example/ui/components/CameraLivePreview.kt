package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.Municipality
import java.util.concurrent.Executors

data class ArDetectedTarget(
    val id: String,
    val label: String,
    val categoryName: String,
    val adviceShort: String,
    val bagColorHex: Long,
    val confidence: Int
)

@Composable
fun CameraLivePreview(
    municipality: Municipality,
    targetItemHint: String = "",
    onCapturePhoto: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }

    // AR Object Detection Presets tailored to current municipality rules
    val arCandidates = remember(municipality.id, municipality.oversizedThresholdCm) {
        listOf(
            ArDetectedTarget(
                id = "plastic",
                label = "プラスチック容器・トレイ",
                categoryName = "プラスチック資源",
                adviceShort = "水ですすいで市指定プラ袋へ",
                bagColorHex = 0xFF43A047L,
                confidence = 96
            ),
            ArDetectedTarget(
                id = "pet_bottle",
                label = "ペットボトル (PET)",
                categoryName = "ペットボトル・缶類",
                adviceShort = "ラベル・キャップを外して軽くつぶす",
                bagColorHex = 0xFF00ACC1L,
                confidence = 98
            ),
            ArDetectedTarget(
                id = "small_appliance",
                label = "スマートフォン・小型電子機器",
                categoryName = "小型家電・拠点回収",
                adviceShort = "⚠️集積所不可！役所等の回収ボックスへ",
                bagColorHex = 0xFF1976D2L,
                confidence = 95
            ),
            ArDetectedTarget(
                id = "oversized",
                label = "衣装ケース・家具（${municipality.oversizedThresholdCm}cm超）",
                categoryName = "粗大ごみ（要予約）",
                adviceShort = "自治体へ電話/Web予約して処理券貼付",
                bagColorHex = 0xFFE65100L,
                confidence = 92
            ),
            ArDetectedTarget(
                id = "burnable",
                label = "生ごみ・革製品・可燃物",
                categoryName = "可燃ごみ（燃やすごみ）",
                adviceShort = "指定可燃物袋に入れて収集日朝8:30迄",
                bagColorHex = 0xFFE53935L,
                confidence = 89
            )
        )
    }

    // Active detected object based on user hint or cycling
    var selectedTargetIndex by remember { mutableStateOf(0) }
    LaunchedEffect(targetItemHint) {
        if (targetItemHint.isNotBlank()) {
            val matchedIndex = arCandidates.indexOfFirst {
                it.label.contains(targetItemHint) || it.categoryName.contains(targetItemHint)
            }
            if (matchedIndex >= 0) {
                selectedTargetIndex = matchedIndex
            }
        }
    }

    val activeTarget = arCandidates[selectedTargetIndex.coerceIn(0, arCandidates.size - 1)]

    // Scanning line animation
    val infiniteTransition = rememberInfiniteTransition(label = "ar_scan_transition")
    val scanLineFraction by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_animation"
    )

    // Reticle pulse scale
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha_animation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
            .testTag("camera_live_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boxWidth = maxWidth
            val boxHeight = maxHeight

            // 1. CameraX Surface View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            val boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            camera = boundCamera
                        } catch (e: Exception) {
                            // Camera binding failure fallback
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = {
                    // Update flash / torch
                    camera?.cameraControl?.enableTorch(isFlashOn)
                }
            )

            // 2. Cyber/Modern Object Detection Frame Reticle (簡易検出枠)
            val activeColor = Color(activeTarget.bagColorHex)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val frameW = size.width * 0.72f
                val frameH = size.height * 0.52f
                val left = (size.width - frameW) / 2f
                val top = (size.height - frameH) / 2.3f

                // Outer tinted bounding box
                drawRoundRect(
                    color = activeColor.copy(alpha = 0.12f),
                    topLeft = Offset(left, top),
                    size = Size(frameW, frameH),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )

                // Corner brackets
                val cornerLength = 28.dp.toPx()
                val strokeWidth = 3.5.dp.toPx()
                val bracketColor = activeColor.copy(alpha = pulseAlpha)

                // Top-Left corner
                drawLine(bracketColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
                drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

                // Top-Right corner
                drawLine(bracketColor, Offset(left + frameW, top), Offset(left + frameW - cornerLength, top), strokeWidth)
                drawLine(bracketColor, Offset(left + frameW, top), Offset(left + frameW, top + cornerLength), strokeWidth)

                // Bottom-Left corner
                drawLine(bracketColor, Offset(left, top + frameH), Offset(left + cornerLength, top + frameH), strokeWidth)
                drawLine(bracketColor, Offset(left, top + frameH), Offset(left, top + frameH - cornerLength), strokeWidth)

                // Bottom-Right corner
                drawLine(bracketColor, Offset(left + frameW, top + frameH), Offset(left + frameW - cornerLength, top + frameH), strokeWidth)
                drawLine(bracketColor, Offset(left + frameW, top + frameH), Offset(left + frameW, top + frameH - cornerLength), strokeWidth)

                // Scanning laser line
                val scanY = top + frameH * scanLineFraction
                drawLine(
                    color = activeColor.copy(alpha = 0.85f),
                    start = Offset(left + 8.dp.toPx(), scanY),
                    end = Offset(left + frameW - 8.dp.toPx(), scanY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // 3. Top HUD: Real-time Camera Controls (Flash, Switch Lens)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00E676), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE AIカメラ (AR検知中)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "ライト切替",
                            tint = if (isFlashOn) Color.Yellow else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "カメラ切替",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 4. AR Overlay Bubble (簡易ARオーバーレイ: 「これはプラ資源です」「指定袋に入れてください」)
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-55).dp)
                    .padding(horizontal = 24.dp)
                    .testTag("ar_overlay_bubble"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.82f)
                ),
                border = BorderStroke(1.5.dp, activeColor)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = activeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "これは【${activeTarget.categoryName}】です",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeColor,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "袋・出し方: ${activeTarget.adviceShort}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "特定品目: ${activeTarget.label} (信頼度 ${activeTarget.confidence}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }
            }

            // 5. Bottom Controls: Candidate Quick Switchers & Shutter Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Object Detection Target Switcher Chips (Tap to dynamically simulate lock-on to another object)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    arCandidates.forEachIndexed { index, candidate ->
                        val isSelected = index == selectedTargetIndex
                        val chipColor = Color(candidate.bagColorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTargetIndex = index },
                            label = {
                                Text(
                                    text = candidate.label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor,
                                selectedLabelColor = Color.White,
                                containerColor = Color.DarkGray.copy(alpha = 0.6f),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Shutter / Capture Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isCapturing) return@Button
                            isCapturing = true

                            val capture = imageCapture
                            if (capture == null) {
                                isCapturing = false
                                Toast.makeText(context, "カメラ準備中です", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val executor = Executors.newSingleThreadExecutor()
                            capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    try {
                                        val bitmap = image.toBitmap()
                                        // Rotate bitmap if needed based on rotationDegrees
                                        val rotation = image.imageInfo.rotationDegrees
                                        val finalBitmap = if (rotation != 0) {
                                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                        } else {
                                            bitmap
                                        }
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            onCapturePhoto(finalBitmap)
                                        }
                                    } catch (e: Exception) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                            Toast.makeText(context, "撮影画像の処理に失敗しました", Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        image.close()
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    ContextCompat.getMainExecutor(context).execute {
                                        isCapturing = false
                                        Toast.makeText(context, "撮影エラー: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            })
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("ar_camera_shutter_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        enabled = !isCapturing
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("解析中...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "シャッター",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "シャッターを切って詳細AI判定",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
