package com.clearshot.ai.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.ui.theme.AccentAmber
import kotlin.math.roundToInt

/**
 * Vivo X200-Inspired High-Magnification Zoom Finder Window.
 *
 * Automatically displays when zoom exceeds 10X (e.g. 15X, 30X, 50X, 100X).
 * Shows a compact picture-in-picture wide scene preview with a draggable golden
 * field-of-view bounding box tracking the main viewfinder's magnified crop.
 *
 * Features:
 * 1. Automatic appearance above 10X; auto-hide at <= 10X.
 * 2. Proportional FOV reticle scaling: rect = 1 / zoomRatio.
 * 3. Interactive dragging to reposition the zoomed area.
 * 4. Tap-to-center framing.
 * 5. Visual corner brackets and crosshair.
 */
@Composable
fun ZoomFinderView(
    zoomRatio: Float,
    previewBitmap: Bitmap?,
    panOffset: Offset, // Normalized -0.5f to +0.5f
    onPanOffsetChanged: (Offset) -> Unit,
    onResetPan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isVisible = zoomRatio > 10.0f
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        val windowWidthDp = 136.dp
        val windowHeightDp = 102.dp // 4:3 aspect ratio

        val windowWidthPx = with(density) { windowWidthDp.toPx() }
        val windowHeightPx = with(density) { windowHeightDp.toPx() }

        // Bounding box dimensions representing the exact field of view: 1 / zoomRatio
        val boxWidthFraction = (1.0f / zoomRatio).coerceIn(0.01f, 0.40f)
        val boxHeightFraction = (1.0f / zoomRatio).coerceIn(0.01f, 0.40f)

        // Ensure minimum grabbable touch target size (at least 18dp)
        val minBoxSizePx = with(density) { 18.dp.toPx() }
        val boxWidthPx = (windowWidthPx * boxWidthFraction).coerceAtLeast(minBoxSizePx)
        val boxHeightPx = (windowHeightPx * boxHeightFraction).coerceAtLeast(minBoxSizePx)

        // Maximum allowable pan displacement so bounding box stays inside window
        val maxDisplacementX = ((windowWidthPx - boxWidthPx) / 2f).coerceAtLeast(0f)
        val maxDisplacementY = ((windowHeightPx - boxHeightPx) / 2f).coerceAtLeast(0f)

        // Calculate reticle top-left coordinates inside the mini-map window
        val centerX = (windowWidthPx / 2f) + (panOffset.x * 2f * maxDisplacementX)
        val centerY = (windowHeightPx / 2f) + (panOffset.y * 2f * maxDisplacementY)

        val boxLeft = (centerX - boxWidthPx / 2f).coerceIn(0f, windowWidthPx - boxWidthPx)
        val boxTop = (centerY - boxHeightPx / 2f).coerceIn(0f, windowHeightPx - boxHeightPx)

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .shadow(12.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xE610141D))
                .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            // Header: "ZOOM FINDER" tag + Current Magnification
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .width(windowWidthDp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterCenterFocus,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ZOOM FINDER",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${zoomRatio.toInt()}X",
                    color = AccentAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Main Mini-map Viewport
            Box(
                modifier = Modifier
                    .size(windowWidthDp, windowHeightDp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .pointerInput(windowWidthPx, windowHeightPx, maxDisplacementX, maxDisplacementY) {
                        // Drag gesture: drag the golden reticle to navigate around the scene
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (maxDisplacementX > 0f && maxDisplacementY > 0f) {
                                val deltaNormX = dragAmount.x / (2f * maxDisplacementX)
                                val deltaNormY = dragAmount.y / (2f * maxDisplacementY)
                                val newPan = Offset(
                                    x = (panOffset.x + deltaNormX).coerceIn(-0.5f, 0.5f),
                                    y = (panOffset.y + deltaNormY).coerceIn(-0.5f, 0.5f)
                                )
                                onPanOffsetChanged(newPan)
                            }
                        }
                    }
                    .pointerInput(windowWidthPx, windowHeightPx) {
                        // Tap gesture: tap anywhere in the mini-map to instantly re-center the crop box
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val normX = ((tapOffset.x - windowWidthPx / 2f) / (windowWidthPx / 2f))
                                    .coerceIn(-0.5f, 0.5f)
                                val normY = ((tapOffset.y - windowHeightPx / 2f) / (windowHeightPx / 2f))
                                    .coerceIn(-0.5f, 0.5f)
                                onPanOffsetChanged(Offset(normX, normY))
                            },
                            onDoubleTap = {
                                onResetPan()
                            }
                        )
                    }
            ) {
                // 1. Live wide-field scene thumbnail
                if (previewBitmap != null && !previewBitmap.isRecycled) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Zoom Finder Wide Scene",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback radar grid background while frame streams initialize
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 1.dp.toPx()
                        val gridColor = Color.White.copy(alpha = 0.10f)
                        drawLine(gridColor, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), stroke)
                        drawLine(gridColor, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), stroke)
                        drawLine(gridColor, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), stroke)
                        drawLine(gridColor, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), stroke)
                    }
                }

                // Subtle dark vignette border inside the mini-map
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                )

                // 2. Interactive Golden FOV Crop Reticle with Corner Brackets & Crosshair
                Canvas(
                    modifier = Modifier
                        .offset { IntOffset(boxLeft.roundToInt(), boxTop.roundToInt()) }
                        .size(with(density) { boxWidthPx.toDp() }, with(density) { boxHeightPx.toDp() })
                ) {
                    val w = size.width
                    val h = size.height
                    val strokeW = 1.8.dp.toPx()
                    val bracketLen = (w.coerceAtMost(h) * 0.35f).coerceIn(4.dp.toPx(), 8.dp.toPx())

                    // Semi-transparent glowing fill
                    drawRect(color = AccentAmber.copy(alpha = 0.18f))

                    // Solid bounding border
                    drawRect(
                        color = AccentAmber,
                        style = Stroke(width = strokeW)
                    )

                    // Corner Bracket Overlays for high-contrast visibility
                    // Top-Left
                    drawLine(Color.White, Offset(0f, 0f), Offset(bracketLen, 0f), strokeW + 0.5f, StrokeCap.Round)
                    drawLine(Color.White, Offset(0f, 0f), Offset(0f, bracketLen), strokeW + 0.5f, StrokeCap.Round)
                    // Top-Right
                    drawLine(Color.White, Offset(w, 0f), Offset(w - bracketLen, 0f), strokeW + 0.5f, StrokeCap.Round)
                    drawLine(Color.White, Offset(w, 0f), Offset(w, bracketLen), strokeW + 0.5f, StrokeCap.Round)
                    // Bottom-Left
                    drawLine(Color.White, Offset(0f, h), Offset(bracketLen, h), strokeW + 0.5f, StrokeCap.Round)
                    drawLine(Color.White, Offset(0f, h), Offset(0f, h - bracketLen), strokeW + 0.5f, StrokeCap.Round)
                    // Bottom-Right
                    drawLine(Color.White, Offset(w, h), Offset(w - bracketLen, h), strokeW + 0.5f, StrokeCap.Round)
                    drawLine(Color.White, Offset(w, h), Offset(w, h - bracketLen), strokeW + 0.5f, StrokeCap.Round)

                    // Center Crosshair (+)
                    val crosshairSize = 3.dp.toPx()
                    val cX = w / 2f
                    val cY = h / 2f
                    drawLine(AccentAmber, Offset(cX - crosshairSize, cY), Offset(cX + crosshairSize, cY), strokeW)
                    drawLine(AccentAmber, Offset(cX, cY - crosshairSize), Offset(cX, cY + crosshairSize), strokeW)
                }
            }
        }
    }
}
