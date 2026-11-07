package com.clearshot.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 3x3 Rule of Thirds compositional viewfinder grid.
 */
@Composable
fun GridOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val gridColor = Color.White.copy(alpha = 0.22f)
        val strokeWidth = 1.dp.toPx()

        // Vertical lines
        val x1 = width / 3f
        val x2 = (width * 2f) / 3f
        drawLine(gridColor, Offset(x1, 0f), Offset(x1, height), strokeWidth)
        drawLine(gridColor, Offset(x2, 0f), Offset(x2, height), strokeWidth)

        // Horizontal lines
        val y1 = height / 3f
        val y2 = (height * 2f) / 3f
        drawLine(gridColor, Offset(0f, y1), Offset(width, y1), strokeWidth)
        drawLine(gridColor, Offset(0f, y2), Offset(width, y2), strokeWidth)
    }
}
