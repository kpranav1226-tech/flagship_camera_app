package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Virtual horizon level indicator.
 * Displays dynamic rotating horizon guides, fixed level reference notches,
 * central alignment reticle lock, and real-time roll degree badge.
 * Features smooth color animations when within configurable tolerance (default ±1.0°).
 */
@Composable
fun HorizonLevelIndicator(
    rollAngle: Float,
    isEnabled: Boolean,
    toleranceDeg: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    val isLevel = abs(rollAngle) <= toleranceDeg
    val activeColor by animateColorAsState(
        targetValue = if (isLevel) Color(0xFF00E676) else Color.White.copy(alpha = 0.65f),
        animationSpec = tween(durationMillis = 200),
        label = "horizon_color"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val lineLength = 76.dp.toPx()
            val strokeWidth = 2.dp.toPx()
            val centerGap = 22.dp.toPx()

            // 1. Fixed horizontal reference ticks (stationary guides)
            val tickColor = Color.White.copy(alpha = 0.28f)
            drawLine(
                color = tickColor,
                start = Offset(cx - lineLength - 30.dp.toPx(), cy),
                end = Offset(cx - lineLength - 16.dp.toPx(), cy),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = tickColor,
                start = Offset(cx + lineLength + 16.dp.toPx(), cy),
                end = Offset(cx + lineLength + 30.dp.toPx(), cy),
                strokeWidth = 1.5.dp.toPx()
            )

            // 2. Rotating Horizon Guides
            rotate(-rollAngle, pivot = Offset(cx, cy)) {
                // Left dynamic horizon bar
                drawLine(
                    color = activeColor,
                    start = Offset(cx - lineLength - centerGap, cy),
                    end = Offset(cx - centerGap, cy),
                    strokeWidth = strokeWidth
                )

                // Right dynamic horizon bar
                drawLine(
                    color = activeColor,
                    start = Offset(cx + centerGap, cy),
                    end = Offset(cx + lineLength + centerGap, cy),
                    strokeWidth = strokeWidth
                )

                // Central reticle circle
                drawCircle(
                    color = activeColor,
                    radius = if (isLevel) 7.dp.toPx() else 5.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeWidth)
                )

                // Central lock alignment indicator (crosshair when perfectly level)
                if (isLevel) {
                    drawCircle(
                        color = Color(0xFF00E676),
                        radius = 2.5.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                    // Level wings
                    drawLine(
                        color = Color(0xFF00E676),
                        start = Offset(cx, cy - 8.dp.toPx()),
                        end = Offset(cx, cy - 3.dp.toPx()),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = Color(0xFF00E676),
                        start = Offset(cx, cy + 3.dp.toPx()),
                        end = Offset(cx, cy + 8.dp.toPx()),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }

        // Angle & Level Status Badge below center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 90.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isLevel) Color(0xCC00E676) else Color(0x99000000),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isLevel) "LEVEL 0°" else "%+.1f°".format(rollAngle),
                    color = if (isLevel) Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
