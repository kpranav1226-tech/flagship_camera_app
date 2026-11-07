package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
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
import com.clearshot.ai.camera.FlashMode
import com.clearshot.ai.ui.theme.AccentAmber
import com.clearshot.ai.ui.theme.OverlayPill

/**
 * Minimal, clean top bar showing ONLY essential camera controls:
 * - Settings (Gear icon)
 * - Flash Shortcut (Off / Auto / On / Torch)
 * - Tools button for on-demand controls
 */
@Composable
fun MinimalCameraTopBar(
    flashMode: FlashMode,
    aspectRatio: String = "3:4",
    photoResolution: String = "12M",
    isToolsOpen: Boolean,
    onSettingsClick: () -> Unit,
    onFlashToggle: () -> Unit,
    onAspectRatioClick: () -> Unit = {},
    onToolsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Settings Shortcut Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OverlayPill)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Flash Shortcut Toggle Button
        val flashIcon = when (flashMode) {
            FlashMode.AUTO -> Icons.Filled.FlashAuto
            FlashMode.ON -> Icons.Filled.FlashOn
            FlashMode.TORCH -> Icons.Filled.Highlight
            FlashMode.OFF -> Icons.Filled.FlashOff
        }
        val flashColor by animateColorAsState(
            targetValue = if (flashMode != FlashMode.OFF) AccentAmber else Color.White,
            label = "flashColor"
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OverlayPill)
                .clickable { onFlashToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = flashIcon,
                contentDescription = "Flash: ${flashMode.name}",
                tint = flashColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // 3. Aspect Ratio & Resolution Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(OverlayPill)
                .clickable { onAspectRatioClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$aspectRatio • $photoResolution",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 4. Single "Tools" (🛠️) Button for On-Demand Controls
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isToolsOpen) Color(0xFFFFB300) else OverlayPill)
                .clickable { onToolsClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = "Tools",
                    tint = if (isToolsOpen) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Tools",
                    color = if (isToolsOpen) Color.Black else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
