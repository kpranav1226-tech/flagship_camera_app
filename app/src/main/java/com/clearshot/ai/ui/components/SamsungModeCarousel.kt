package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.clearshot.ai.camera.CameraMode
import com.clearshot.ai.ui.theme.ActiveBlue

enum class DisplayMode(val label: String, val cameraMode: CameraMode?) {
    PRO("PRO", CameraMode.PRO),
    PHOTO("PHOTO", CameraMode.PHOTO),
    VIDEO("VIDEO", CameraMode.VIDEO),
    PORTRAIT("PORTRAIT", CameraMode.PORTRAIT),
    NIGHT("NIGHT", CameraMode.NIGHT),
    MORE("MORE", null)
}

/**
 * Bottom Capture Modes Carousel matching callout (16) in the Samsung Galaxy M15 5G UI design.
 * Features:
 * - PRO, PHOTO, VIDEO, PORTRAIT, NIGHT, MORE
 * - Vibrant blue capsule background (#2D7FF9) highlighting the selected mode.
 * - Smooth scrollable row.
 */
@Composable
fun SamsungModeCarousel(
    currentMode: CameraMode,
    isMoreActive: Boolean,
    onModeSelected: (CameraMode) -> Unit,
    onMoreClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DisplayMode.values().forEach { mode ->
            val isSelected = when {
                mode == DisplayMode.MORE -> isMoreActive
                isMoreActive -> false
                mode.cameraMode == currentMode -> true
                currentMode == CameraMode.PRO_VIDEO && mode == DisplayMode.PRO -> true
                currentMode == CameraMode.MACRO && mode == DisplayMode.PHOTO -> false
                currentMode == CameraMode.DOCUMENT && mode == DisplayMode.PHOTO -> false
                else -> false
            }

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) ActiveBlue else Color.Transparent,
                label = "mode_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                label = "mode_text"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .clickable {
                        if (mode == DisplayMode.MORE) {
                            onMoreClicked()
                        } else {
                            mode.cameraMode?.let { onModeSelected(it) }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}
