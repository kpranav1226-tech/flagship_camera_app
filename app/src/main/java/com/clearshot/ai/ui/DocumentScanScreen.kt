package com.clearshot.ai.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.processing.modes.DocumentCorners
import com.clearshot.ai.processing.modes.DocumentFilterMode
import com.clearshot.ai.ui.theme.OverlayPill
import java.io.File
import kotlin.math.hypot

/**
 * Interactive Document Review, Corner Adjustment, and Perspective Dewarping Screen.
 *
 * Features:
 * 1. Automatic boundary & corner detection visualization.
 * 2. Draggable 4-corner handles (TL, TR, BR, BL) to adjust crop boundaries.
 * 3. 90-degree clockwise rotation.
 * 4. Filter modes: Color, Grayscale, and crisp B&W binary text.
 * 5. High-resolution export as JPEG and PDF.
 */
@Composable
fun DocumentScanScreen(
    capturedFile: File,
    initialCorners: DocumentCorners,
    isProcessing: Boolean = false,
    onRetake: () -> Unit,
    onRotate: () -> Unit,
    onAutoDetect: () -> Unit,
    onSaveJpeg: (DocumentCorners, DocumentFilterMode, Int) -> Unit,
    onSavePdf: (DocumentCorners, DocumentFilterMode, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var corners by remember(initialCorners) { mutableStateOf(initialCorners) }
    var selectedFilter by remember { mutableStateOf(DocumentFilterMode.COLOR) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var activeHandleIndex by remember { mutableIntStateOf(-1) }

    // Decode display bitmap efficiently
    var bitmap by remember(capturedFile, rotationDegrees) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(capturedFile, rotationDegrees) {
        if (capturedFile.exists()) {
            val rawBmp = BitmapFactory.decodeFile(capturedFile.absolutePath)
            if (rawBmp != null) {
                if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    bitmap = Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true)
                    rawBmp.recycle()
                } else {
                    bitmap = rawBmp
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0F14))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top Bar: Title, Retake (✕), Rotate (🔄), Reset Auto (⚡)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OverlayPill)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Retake", tint = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Document Scanner",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (corners.isAutoDetected) "Auto boundary detected • Drag to adjust" else "Manual adjustment active",
                        color = if (corners.isAutoDetected) Color(0xFFFFB300) else Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OverlayPill)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate", tint = Color.White)
                    }

                    IconButton(
                        onClick = onAutoDetect,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OverlayPill)
                    ) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Auto Detect", tint = Color(0xFFFFB300))
                    }
                }
            }

            // 2. Interactive Document Canvas with Draggable 4-Corner Overlay
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    val containerWidth = constraints.maxWidth.toFloat()
                    val containerHeight = constraints.maxHeight.toFloat()

                    // Compute letterboxed display aspect ratio for image
                    val imgAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    val containerAspect = containerWidth / containerHeight

                    val displayW: Float
                    val displayH: Float
                    val offsetX: Float
                    val offsetY: Float

                    if (imgAspect > containerAspect) {
                        displayW = containerWidth
                        displayH = containerWidth / imgAspect
                        offsetX = 0f
                        offsetY = (containerHeight - displayH) / 2f
                    } else {
                        displayH = containerHeight
                        displayW = containerHeight * imgAspect
                        offsetX = (containerWidth - displayW) / 2f
                        offsetY = 0f
                    }

                    // Render base image
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Document Frame",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )

                    // Draw Interactive Corner Overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(displayW, displayH, offsetX, offsetY) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        val points = listOf(
                                            Offset(offsetX + corners.topLeft.x * displayW, offsetY + corners.topLeft.y * displayH),
                                            Offset(offsetX + corners.topRight.x * displayW, offsetY + corners.topRight.y * displayH),
                                            Offset(offsetX + corners.bottomRight.x * displayW, offsetY + corners.bottomRight.y * displayH),
                                            Offset(offsetX + corners.bottomLeft.x * displayW, offsetY + corners.bottomLeft.y * displayH)
                                        )

                                        // Find closest handle within 48dp touch radius
                                        val maxTouchDist = 48.dp.toPx()
                                        var closestIdx = -1
                                        var minDist = Float.MAX_VALUE
                                        points.forEachIndexed { idx, pt ->
                                            val dist = hypot(pt.x - startOffset.x, pt.y - startOffset.y)
                                            if (dist < maxTouchDist && dist < minDist) {
                                                minDist = dist
                                                closestIdx = idx
                                            }
                                        }
                                        activeHandleIndex = closestIdx
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (activeHandleIndex in 0..3) {
                                            change.consume()
                                            val currentNorm = when (activeHandleIndex) {
                                                0 -> corners.topLeft
                                                1 -> corners.topRight
                                                2 -> corners.bottomRight
                                                else -> corners.bottomLeft
                                            }

                                            val newNormX = (currentNorm.x + dragAmount.x / displayW).coerceIn(0f, 1f)
                                            val newNormY = (currentNorm.y + dragAmount.y / displayH).coerceIn(0f, 1f)

                                            corners = when (activeHandleIndex) {
                                                0 -> corners.copy(topLeft = PointF(newNormX, newNormY), isAutoDetected = false)
                                                1 -> corners.copy(topRight = PointF(newNormX, newNormY), isAutoDetected = false)
                                                2 -> corners.copy(bottomRight = PointF(newNormX, newNormY), isAutoDetected = false)
                                                else -> corners.copy(bottomLeft = PointF(newNormX, newNormY), isAutoDetected = false)
                                            }
                                        }
                                    },
                                    onDragEnd = { activeHandleIndex = -1 },
                                    onDragCancel = { activeHandleIndex = -1 }
                                )
                            }
                    ) {
                        val tl = Offset(offsetX + corners.topLeft.x * displayW, offsetY + corners.topLeft.y * displayH)
                        val tr = Offset(offsetX + corners.topRight.x * displayW, offsetY + corners.topRight.y * displayH)
                        val br = Offset(offsetX + corners.bottomRight.x * displayW, offsetY + corners.bottomRight.y * displayH)
                        val bl = Offset(offsetX + corners.bottomLeft.x * displayW, offsetY + corners.bottomLeft.y * displayH)

                        // 1. Draw glowing crop boundary polygon
                        val polygonPath = Path().apply {
                            moveTo(tl.x, tl.y)
                            lineTo(tr.x, tr.y)
                            lineTo(br.x, br.y)
                            lineTo(bl.x, bl.y)
                            close()
                        }

                        // Shaded interior overlay
                        drawPath(polygonPath, color = Color(0x33FFB300))
                        // High-contrast boundary stroke
                        drawPath(polygonPath, color = Color(0xFFFFB300), style = Stroke(width = 3.dp.toPx()))

                        // 2. Draw circular corner handles
                        val handles = listOf(tl, tr, br, bl)
                        handles.forEachIndexed { idx, pt ->
                            val isActive = activeHandleIndex == idx
                            val radius = if (isActive) 16.dp.toPx() else 12.dp.toPx()
                            // Outer ring
                            drawCircle(color = Color.Black, radius = radius + 2.dp.toPx(), center = pt)
                            // Inner amber circle
                            drawCircle(color = if (isActive) Color.White else Color(0xFFFFB300), radius = radius, center = pt)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFFB300))
                    }
                }
            }

            // 3. Filter Mode Selector (Color, Grayscale, B&W)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF181D27))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DocumentFilterMode.entries.forEach { mode ->
                        val isSel = selectedFilter == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0xFFFFB300) else Color.Transparent)
                                .clickable { selectedFilter = mode }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Action Buttons (Save JPEG & Export PDF)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save JPEG Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF242B38))
                        .clickable(enabled = !isProcessing) {
                            onSaveJpeg(corners, selectedFilter, rotationDegrees)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save JPEG", tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(text = "Save JPEG", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Export PDF Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFB300))
                        .clickable(enabled = !isProcessing) {
                            onSavePdf(corners, selectedFilter, rotationDegrees)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = Color.Black, modifier = Modifier.size(18.dp))
                            Text(text = "Export PDF", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
