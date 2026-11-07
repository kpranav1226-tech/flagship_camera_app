package com.clearshot.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.ui.theme.AccentAmber
import com.clearshot.ai.ui.theme.DarkGlass
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive Exposure Ruler Slider matching callout (13) from the Samsung Galaxy M15 5G UI design.
 * Features:
 * - Dynamic EV readout with center amber indicator: -2.0 to +2.0
 * - Horizontal ruler markings: -2.0, -1.0, 0.0, +1.0, +2.0
 * - Interactive dragging and tapping to fine-tune camera exposure value.
 */
@Composable
fun ExposureRulerSlider(
    evValue: Float,
    onEvChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkGlass)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount.x
                        val delta = dragAccumulator / 180f
                        if (kotlin.math.abs(delta) >= 0.1f) {
                            val newEv = (evValue + delta).coerceIn(-2.0f, 2.0f)
                            val rounded = (newEv * 10).roundToInt() / 10f
                            onEvChanged(rounded)
                            dragAccumulator = 0f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width
                    if (width > 0) {
                        val fraction = (offset.x / width).coerceIn(0f, 1f)
                        val targetEv = -2.0f + fraction * 4.0f
                        val rounded = (targetEv * 10).roundToInt() / 10f
                        onEvChanged(rounded)
                    }
                }
            }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // EV Header & Numeric Readout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "EV ",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                val formattedEv = if (evValue > 0f) {
                    String.format(Locale.US, "+%.1f", evValue)
                } else if (evValue == 0f) {
                    "0.0"
                } else {
                    String.format(Locale.US, "%.1f", evValue)
                }
                Text(
                    text = formattedEv,
                    color = if (evValue == 0f) Color.White else AccentAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Ruler Tick Marks Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tickValues = listOf(-2.0f, -1.5f, -1.0f, -0.5f, 0.0f, 0.5f, 1.0f, 1.5f, 2.0f)

                tickValues.forEach { tick ->
                    val isMajor = tick in listOf(-2.0f, -1.0f, 0.0f, 1.0f, 2.0f)
                    val isCenter = tick == 0.0f
                    val isCurrent = kotlin.math.abs(evValue - tick) < 0.25f

                    val tickColor = when {
                        isCurrent -> AccentAmber
                        isCenter -> AccentAmber.copy(alpha = 0.8f)
                        isMajor -> Color.White
                        else -> Color.White.copy(alpha = 0.35f)
                    }

                    Box(
                        modifier = Modifier
                            .width(if (isMajor) 2.dp else 1.dp)
                            .height(if (isMajor) 14.dp else 8.dp)
                            .background(tickColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Numeric Scale Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "-2.0", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(text = "-1.0", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(text = "0.0", color = AccentAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = "+1.0", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(text = "+2.0", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
