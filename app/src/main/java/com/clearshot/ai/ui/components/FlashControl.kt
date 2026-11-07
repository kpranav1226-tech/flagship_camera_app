package com.clearshot.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearshot.ai.camera.FlashMode
import com.clearshot.ai.ui.theme.FocusGold
import com.clearshot.ai.ui.theme.OverlayPill

import androidx.compose.material.icons.filled.Highlight

/**
 * Clean camera flash control cycling between OFF, AUTO, ON, and TORCH.
 */
@Composable
fun FlashControl(
    flashMode: FlashMode,
    onFlashModeChanged: (FlashMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val nextMode = when (flashMode) {
        FlashMode.OFF -> FlashMode.AUTO
        FlashMode.AUTO -> FlashMode.ON
        FlashMode.ON -> FlashMode.TORCH
        FlashMode.TORCH -> FlashMode.OFF
    }

    val icon = when (flashMode) {
        FlashMode.OFF -> Icons.Filled.FlashOff
        FlashMode.ON -> Icons.Filled.FlashOn
        FlashMode.AUTO -> Icons.Filled.FlashAuto
        FlashMode.TORCH -> Icons.Filled.Highlight
    }

    val tint = when (flashMode) {
        FlashMode.OFF -> Color.White
        FlashMode.ON, FlashMode.AUTO, FlashMode.TORCH -> FocusGold
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(OverlayPill)
            .clickable { onFlashModeChanged(nextMode) }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Flash mode: ${flashMode.name}",
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
