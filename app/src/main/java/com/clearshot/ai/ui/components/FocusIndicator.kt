package com.clearshot.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.clearshot.ai.ui.theme.FocusGold

/**
 * Animated focus and metering reticle rendered at the user's tap location.
 */
@Composable
fun FocusIndicator(
    focusPoint: Offset?,
    modifier: Modifier = Modifier
) {
    if (focusPoint == null) return

    val scale = remember(focusPoint) { Animatable(1.5f) }
    val alpha = remember(focusPoint) { Animatable(1f) }

    LaunchedEffect(focusPoint) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
        // Hold visible for a moment then fade out
        kotlinx.coroutines.delay(1200)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350)
        )
    }

    if (alpha.value > 0f) {
        Box(modifier = modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringRadius = 36.dp.toPx() * scale.value
                val strokeWidth = 2.dp.toPx()

                // Outer circle
                drawCircle(
                    color = FocusGold.copy(alpha = alpha.value),
                    radius = ringRadius,
                    center = focusPoint,
                    style = Stroke(width = strokeWidth)
                )

                // 4 Reticle tick marks
                val tickLength = 8.dp.toPx()
                // Top tick
                drawLine(
                    color = FocusGold.copy(alpha = alpha.value),
                    start = Offset(focusPoint.x, focusPoint.y - ringRadius - tickLength),
                    end = Offset(focusPoint.x, focusPoint.y - ringRadius + 2.dp.toPx()),
                    strokeWidth = strokeWidth
                )
                // Bottom tick
                drawLine(
                    color = FocusGold.copy(alpha = alpha.value),
                    start = Offset(focusPoint.x, focusPoint.y + ringRadius + tickLength),
                    end = Offset(focusPoint.x, focusPoint.y + ringRadius - 2.dp.toPx()),
                    strokeWidth = strokeWidth
                )
                // Left tick
                drawLine(
                    color = FocusGold.copy(alpha = alpha.value),
                    start = Offset(focusPoint.x - ringRadius - tickLength, focusPoint.y),
                    end = Offset(focusPoint.x - ringRadius + 2.dp.toPx(), focusPoint.y),
                    strokeWidth = strokeWidth
                )
                // Right tick
                drawLine(
                    color = FocusGold.copy(alpha = alpha.value),
                    start = Offset(focusPoint.x + ringRadius + tickLength, focusPoint.y),
                    end = Offset(focusPoint.x + ringRadius - 2.dp.toPx(), focusPoint.y),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}
