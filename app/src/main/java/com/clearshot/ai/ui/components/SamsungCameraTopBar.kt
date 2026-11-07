package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
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
import com.clearshot.ai.ui.theme.AccentBlue
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.PillGlass

/**
 * Top bar header controls matching the Samsung Galaxy M15 5G full camera design.
 * Features:
 * (1) Settings (Gear)
 * (2) Flash Control (Off / Auto / On / Torch)
 * (3) Aspect Ratio (1:1 / 3:4 / 9:16 / Full)
 * (4) Photo Resolution (12M / 50M / 5M / 2M)
 * (5) HDR (High Dynamic Range)
 * (6) AI Scene Optimization (Sparkles)
 * (7) Overflow menu (Three dots)
 */
@Composable
fun SamsungCameraTopBar(
    flashMode: FlashMode,
    aspectRatio: String,
    photoResolution: String,
    isHdrEnabled: Boolean,
    isAiOptimizationEnabled: Boolean,
    onSettingsClick: () -> Unit,
    onFlashToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onResolutionToggle: () -> Unit,
    onHdrToggle: () -> Unit,
    onAiOptimizationToggle: () -> Unit,
    onMoreOptionsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // (1) Settings Icon Button
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // (2) Flash Control Button
        val flashIcon = when (flashMode) {
            FlashMode.AUTO -> Icons.Filled.FlashAuto
            FlashMode.ON -> Icons.Filled.FlashOn
            FlashMode.TORCH -> Icons.Filled.Highlight
            FlashMode.OFF -> Icons.Filled.FlashOff
        }
        val flashColor by animateColorAsState(
            targetValue = if (flashMode != FlashMode.OFF) AccentAmber else Color.White,
            label = "flash_tint"
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .clickable { onFlashToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = flashIcon,
                contentDescription = "Flash",
                tint = flashColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // (3) Aspect Ratio Pill [3:4]
        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(PillGlass)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(15.dp))
                .clickable { onAspectRatioToggle() }
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = aspectRatio,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // (4) Photo Resolution Pill [12M]
        val is50M = photoResolution == "50M"
        val resBg by animateColorAsState(
            targetValue = if (is50M) AccentAmber.copy(alpha = 0.25f) else PillGlass,
            label = "res_bg"
        )
        val resBorder by animateColorAsState(
            targetValue = if (is50M) AccentAmber else Color.White.copy(alpha = 0.15f),
            label = "res_border"
        )
        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(resBg)
                .border(1.dp, resBorder, RoundedCornerShape(15.dp))
                .clickable { onResolutionToggle() }
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = photoResolution,
                color = if (is50M) AccentAmber else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // (5) HDR Pill [HDR]
        val hdrColor by animateColorAsState(
            targetValue = if (isHdrEnabled) AccentAmber else Color.White.copy(alpha = 0.7f),
            label = "hdr_tint"
        )
        val hdrBorder by animateColorAsState(
            targetValue = if (isHdrEnabled) AccentAmber.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
            label = "hdr_border"
        )
        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(PillGlass)
                .border(1.dp, hdrBorder, RoundedCornerShape(15.dp))
                .clickable { onHdrToggle() }
                .padding(horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "HDR",
                color = hdrColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // (6) AI Optimization Icon [✨]
        val aiTint by animateColorAsState(
            targetValue = if (isAiOptimizationEnabled) AccentBlue else Color.White,
            label = "ai_tint"
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .clickable { onAiOptimizationToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "AI Optimization",
                tint = aiTint,
                modifier = Modifier.size(17.dp)
            )
        }

        // (7) Overflow Menu [⋮]
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .clickable { onMoreOptionsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More Options",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
