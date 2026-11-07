package com.clearshot.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Modern QuickZoomBar providing:
 * - Minimal, non-intrusive default pill showing current zoom (e.g. 1.0X).
 * - Expandable quick presets: 0.5X, 1X, 2X, 4X, 10X, 30X, 50X, 100X.
 * - Logarithmically-spaced fine-tune slider up to 100X digital zoom.
 * - Visual stability warning hint when zoom >= 10X.
 */
@Composable
fun QuickZoomBar(
    currentZoom: Float,
    minZoom: Float = 0.5f,
    maxZoom: Float = 100.0f,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(0.5f, 1.0f, 2.0f, 4.0f, 10.0f, 30.0f, 50.0f, 100.0f)

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High zoom stability indicator (>= 10X)
        AnimatedVisibility(
            visible = currentZoom >= 9.9f,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC2A1F0D))
                    .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🔭 ${String.format(Locale.US, "%.0f", currentZoom)}X Digital Zoom • Hold phone steady",
                    color = Color(0xFFFFD54F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isExpanded) {
            // Expanded preset bar + continuous slider
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xD910141D))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preset buttons row
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { preset ->
                        val isSelected = kotlin.math.abs(currentZoom - preset) < 0.25f
                        val label = if (preset < 1.0f) "0.5X" else "${preset.toInt()}X"
                        val is100X = preset >= 99f
                        val btnWidth = if (is100X) 46.dp else 40.dp

                        Box(
                            modifier = Modifier
                                .width(btnWidth)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                                .clickable { onZoomSelected(preset) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = if (is100X) 10.sp else 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Collapse toggle button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x44FFFFFF))
                            .clickable { onToggleExpand() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Logarithmic continuous fine-tune slider (0.5X to 100X)
                val safeMin = minZoom.coerceAtLeast(0.1f).toDouble()
                val safeMax = maxZoom.coerceAtLeast(1.0f).toDouble()
                val logMin = kotlin.math.ln(safeMin)
                val logMax = kotlin.math.ln(safeMax)
                val currentLog = kotlin.math.ln(currentZoom.coerceIn(minZoom, maxZoom).toDouble())
                val sliderFraction = ((currentLog - logMin) / (logMax - logMin)).toFloat().coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0.5X",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Slider(
                        value = sliderFraction,
                        onValueChange = { fraction ->
                            val targetLog = logMin + fraction * (logMax - logMin)
                            val targetVal = kotlin.math.exp(targetLog).toFloat()
                            val rounded = if (targetVal < 10.0f) {
                                kotlin.math.round(targetVal * 10f) / 10f
                            } else {
                                kotlin.math.round(targetVal)
                            }
                            onZoomSelected(rounded.coerceIn(minZoom, maxZoom))
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB300),
                            activeTrackColor = Color(0xFFFFB300),
                            inactiveTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = "100X",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Current exact zoom readout pill
                val readoutText = if (currentZoom < 10.0f) {
                    String.format(Locale.US, "%.1fX", currentZoom)
                } else {
                    String.format(Locale.US, "%.0fX", currentZoom)
                }
                Text(
                    text = readoutText,
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Compact, minimal single pill showing current zoom
            val displayLabel = if (currentZoom < 1.0f) {
                String.format(Locale.US, "%.1fX", currentZoom)
            } else if (currentZoom >= 10.0f) {
                String.format(Locale.US, "%.0fX", currentZoom)
            } else {
                String.format(Locale.US, "%.1fX", currentZoom)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xB310141D))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayLabel,
                        color = if (currentZoom > 1.0f) Color(0xFFFFB300) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "›",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
