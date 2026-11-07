package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.ui.theme.AccentAmber
import com.clearshot.ai.ui.theme.PillGlass

/**
 * Vertical Zoom Selector matching callout (12) in the Samsung Galaxy M15 5G UI design:
 * Features:
 * - 4x
 * - 2x
 * - 1x (Main Wide Lens)
 * - 0.5x (Ultra-Wide Lens)
 * Active step highlights with a solid amber/gold circular badge with black text.
 */
@Composable
fun VerticalZoomSelector(
    currentZoom: Float,
    activeCameraId: String,
    isFrontCamera: Boolean,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // 0.5x (Ultra-Wide), 1x (Main Wide), 2x (Sensor crop), 4x (Digital crop)
    val zoomSteps = listOf(4.0f, 2.0f, 1.0f, 0.5f)

    Box(
        modifier = modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(PillGlass)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            zoomSteps.forEach { step ->
                val isSelected = when {
                    step == 0.5f -> activeCameraId == "2" && !isFrontCamera
                    activeCameraId == "2" && !isFrontCamera -> false
                    step == 1.0f -> currentZoom in 0.9f..1.4f
                    step == 2.0f -> currentZoom in 1.5f..2.9f
                    step == 4.0f -> currentZoom >= 3.0f
                    else -> false
                }

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) AccentAmber else Color.Transparent,
                    label = "zoom_step_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.Black else Color.White,
                    label = "zoom_step_text"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1.0f,
                    label = "zoom_step_scale"
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onZoomSelected(step) },
                    contentAlignment = Alignment.Center
                ) {
                    val label = when (step) {
                        0.5f -> "0.5x"
                        1.0f -> "1x"
                        2.0f -> "2x"
                        4.0f -> "4x"
                        else -> "${step.toInt()}x"
                    }
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
