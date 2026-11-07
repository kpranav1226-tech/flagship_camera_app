package com.clearshot.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.clearshot.ai.ui.theme.ReticleYellow

/**
 * Viewfinder Center Framing Reticle with yellow brackets and crosshairs
 * matching the Samsung Galaxy M15 5G camera HUD in the mockup.
 */
@Composable
fun CenterFramingReticle(
    modifier: Modifier = Modifier,
    sizeDp: Int = 74
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val strokeWidth = 2.dp.toPx()
            val bracketLength = 16.dp.toPx()
            val cornerRadius = 6.dp.toPx()
            val w = size.width
            val h = size.height

            // Top-Left Corner Bracket
            drawLine(
                color = ReticleYellow,
                start = Offset(0f, bracketLength),
                end = Offset(0f, cornerRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ReticleYellow,
                start = Offset(cornerRadius, 0f),
                end = Offset(bracketLength, 0f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Top-Right Corner Bracket
            drawLine(
                color = ReticleYellow,
                start = Offset(w - bracketLength, 0f),
                end = Offset(w - cornerRadius, 0f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ReticleYellow,
                start = Offset(w, cornerRadius),
                end = Offset(w, bracketLength),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Left Corner Bracket
            drawLine(
                color = ReticleYellow,
                start = Offset(0f, h - bracketLength),
                end = Offset(0f, h - cornerRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ReticleYellow,
                start = Offset(cornerRadius, h),
                end = Offset(bracketLength, h),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-Right Corner Bracket
            drawLine(
                color = ReticleYellow,
                start = Offset(w - bracketLength, h),
                end = Offset(w - cornerRadius, h),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ReticleYellow,
                start = Offset(w, h - bracketLength),
                end = Offset(w, h - cornerRadius),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Center Crosshair (+)
            val crossSize = 8.dp.toPx()
            val cx = w / 2f
            val cy = h / 2f
            drawLine(
                color = ReticleYellow,
                start = Offset(cx - crossSize, cy),
                end = Offset(cx + crossSize, cy),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = ReticleYellow,
                start = Offset(cx, cy - crossSize),
                end = Offset(cx, cy + crossSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
