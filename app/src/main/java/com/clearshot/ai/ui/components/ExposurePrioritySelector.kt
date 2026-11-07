package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.ui.theme.ActiveBlue
import com.clearshot.ai.ui.theme.PillGlass

/**
 * Exposure Priority Selector matching callout (15) in the Samsung Galaxy M15 5G UI design:
 * Modes:
 * - A: Aperture Priority (portrait blur)
 * - M: Manual (full parameter control)
 * - S: Shutter Priority (motion clarity)
 * - I: ISO Priority (low light stability)
 */
@Composable
fun ExposurePrioritySelector(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf("A", "M", "S", "I")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(PillGlass)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { mode ->
                val isSelected = mode.equals(selectedPriority, ignoreCase = true)

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) ActiveBlue else Color.Transparent,
                    label = "priority_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                    label = "priority_text"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    label = "priority_scale"
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onPrioritySelected(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                    )
                }
            }
        }
    }
}
