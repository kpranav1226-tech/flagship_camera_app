package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.OverlayPill
import java.util.Locale

/**
 * Zoom pill bar allowing quick stepping between primary focal lengths (1x, 2x, etc.).
 */
@Composable
fun ZoomControls(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    isFrontCamera: Boolean = false,
    activeCameraId: String = "0",
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetSteps = if (isFrontCamera) {
        listOf(1.0f, 2.0f).filter { it in minZoom..maxZoom }
    } else {
        // Rear multi-camera system: 0.6x corresponds to Ultra-Wide physical sensor (ID 2),
        // 1.0x corresponds to Main Wide sensor (ID 0), and 2.0x is 2x crop from 50MP sensor.
        listOf(0.6f, 1.0f, 2.0f)
    }
    val displayPresets = if (presetSteps.isEmpty()) listOf(1.0f) else presetSteps

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(OverlayPill)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            displayPresets.forEach { step ->
                val isSelected = if (!isFrontCamera && activeCameraId == "2") {
                    step == 0.6f
                } else if (!isFrontCamera && step == 0.6f) {
                    false
                } else {
                    kotlin.math.abs(currentZoom - step) < 0.4f
                }

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) AccentCyan else Color.Transparent,
                    label = "zoom_pill_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.Black else Color.White,
                    label = "zoom_pill_text"
                )

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onZoomChanged(step) },
                    contentAlignment = Alignment.Center
                ) {
                    val label = if (step == 0.6f) {
                        "0.6x"
                    } else if (step == step.toInt().toFloat()) {
                        "${step.toInt()}x"
                    } else {
                        String.format(Locale.US, "%.1fx", step)
                    }
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
