package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Vibration
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
import com.clearshot.ai.ui.theme.AccentAmber
import com.clearshot.ai.ui.theme.AccentBlue
import com.clearshot.ai.ui.theme.PillGlass

/**
 * Left vertical quick-access toolbar as specified in the Samsung Galaxy M15 5G UI design:
 * (7) Scene Optimizer (Mountain/Landscape icon)
 * (8) RAW Capture (RAW text pill)
 * (9) Video Stabilization EIS (Vibration/Wave icon)
 * (10) Grid Lines (Grid 3x3 icon)
 * (11) Level Indicator (Horizontal line in circle icon)
 */
@Composable
fun CameraLeftToolbar(
    isSceneOptimizerEnabled: Boolean,
    isRawEnabled: Boolean,
    isEisEnabled: Boolean,
    isGridEnabled: Boolean,
    isLevelEnabled: Boolean,
    onToggleSceneOptimizer: () -> Unit,
    onToggleRaw: () -> Unit,
    onToggleEis: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(start = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // (7) Scene Optimizer Button
        val sceneTint by animateColorAsState(
            targetValue = if (isSceneOptimizerEnabled) AccentAmber else Color.White,
            label = "scene_tint"
        )
        val sceneBorder by animateColorAsState(
            targetValue = if (isSceneOptimizerEnabled) AccentAmber.copy(alpha = 0.6f) else Color.Transparent,
            label = "scene_border"
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .border(1.dp, sceneBorder, CircleShape)
                .clickable { onToggleSceneOptimizer() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Landscape,
                contentDescription = "Scene Optimizer",
                tint = sceneTint,
                modifier = Modifier.size(19.dp)
            )
        }

        // (8) RAW Capture Button
        val rawTint by animateColorAsState(
            targetValue = if (isRawEnabled) AccentAmber else Color.White,
            label = "raw_tint"
        )
        val rawBorder by animateColorAsState(
            targetValue = if (isRawEnabled) AccentAmber.copy(alpha = 0.6f) else Color.Transparent,
            label = "raw_border"
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .border(1.dp, rawBorder, CircleShape)
                .clickable { onToggleRaw() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "RAW",
                color = rawTint,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // (9) Video Stabilization (EIS) Button
        val eisTint by animateColorAsState(
            targetValue = if (isEisEnabled) AccentAmber else Color.White,
            label = "eis_tint"
        )
        val eisBorder by animateColorAsState(
            targetValue = if (isEisEnabled) AccentAmber.copy(alpha = 0.6f) else Color.Transparent,
            label = "eis_border"
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .border(1.dp, eisBorder, CircleShape)
                .clickable { onToggleEis() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Vibration,
                contentDescription = "Video Stabilization EIS",
                tint = eisTint,
                modifier = Modifier.size(19.dp)
            )
        }

        // (10) Grid Lines Button
        val gridTint by animateColorAsState(
            targetValue = if (isGridEnabled) AccentAmber else Color.White,
            label = "grid_tint"
        )
        val gridBorder by animateColorAsState(
            targetValue = if (isGridEnabled) AccentAmber.copy(alpha = 0.6f) else Color.Transparent,
            label = "grid_border"
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .border(1.dp, gridBorder, CircleShape)
                .clickable { onToggleGrid() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.GridOn,
                contentDescription = "Grid Lines",
                tint = gridTint,
                modifier = Modifier.size(18.dp)
            )
        }

        // (11) Level Indicator Button (Horizon)
        val levelTint by animateColorAsState(
            targetValue = if (isLevelEnabled) AccentAmber else Color.White,
            label = "level_tint"
        )
        val levelBorder by animateColorAsState(
            targetValue = if (isLevelEnabled) AccentAmber.copy(alpha = 0.6f) else Color.Transparent,
            label = "level_border"
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PillGlass)
                .border(1.dp, levelBorder, CircleShape)
                .clickable { onToggleLevel() },
            contentAlignment = Alignment.Center
        ) {
            // Circle with horizon bar symbol
            Text(
                text = "—",
                color = levelTint,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
