package com.clearshot.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * On-demand control panel for PRO PHOTO mode.
 * Kept hidden by default for an unobstructed viewfinder.
 * When opened by the "Pro Controls" button, presents organized tabs:
 * - EXPOSURE: ISO, Shutter Speed, EV slider
 * - FOCUS: Manual Focus Distance slider / Auto
 * - COLOR: White Balance presets
 * - CAPTURE: RAW (DNG), HDR, Grid, Horizon Level, Histogram, Focus Peaking
 */
@Composable
fun ProPhotoControlsSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    manualIso: Int?,
    onIsoChanged: (Int?) -> Unit,
    shutterSpeedText: String,
    onShutterSpeedChanged: (Long?) -> Unit,
    evValue: Float,
    onEvChanged: (Float) -> Unit,
    manualFocusDistance: Float?,
    onFocusDistanceChanged: (Float?) -> Unit,
    whiteBalanceMode: Int,
    onWhiteBalanceChanged: (Int) -> Unit,
    isRawEnabled: Boolean,
    onToggleRaw: () -> Unit,
    isHdrEnabled: Boolean,
    onToggleHdr: () -> Unit,
    isFocusPeakingEnabled: Boolean,
    onToggleFocusPeaking: () -> Unit,
    isHistogramEnabled: Boolean,
    onToggleHistogram: () -> Unit,
    isGridVisible: Boolean,
    onToggleGrid: () -> Unit,
    isLevelVisible: Boolean,
    onToggleLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Exposure", "Focus", "White Balance", "Overlays")

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xF010141D))
                    .clickable(enabled = false) {}
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pro Photo Controls",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manual exposure & sensor parameters",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFB300))
                            .clickable { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Done",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Done",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { idx, name ->
                        val isSelected = activeTabIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                                .clickable { activeTabIndex = idx }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF181D27))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeTabIndex) {
                        0 -> {
                            // Exposure Tab: ISO, Shutter, EV
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "ISO Sensitivity", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(null, 50, 100, 200, 400, 800, 1600, 3200).forEach { iso ->
                                            val isSel = manualIso == iso
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) Color(0xFFFFB300) else Color(0x22FFFFFF))
                                                    .clickable { onIsoChanged(iso) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = iso?.toString() ?: "Auto",
                                                    color = if (isSel) Color.Black else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0x22FFFFFF))

                                // EV Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Exposure (EV)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = String.format(Locale.US, "%+.1f EV", evValue),
                                            color = Color(0xFFFFB300),
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = evValue,
                                        onValueChange = onEvChanged,
                                        valueRange = -2.0f..2.0f,
                                        steps = 15,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFFB300),
                                            activeTrackColor = Color(0xFFFFB300),
                                            inactiveTrackColor = Color.DarkGray
                                        )
                                    )
                                }
                            }
                        }

                        1 -> {
                            // Focus Tab: Manual Focus Distance slider / Auto
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Manual Focus", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (manualFocusDistance == null) Color(0xFFFFB300) else Color(0x22FFFFFF))
                                            .clickable { onFocusDistanceChanged(null) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Auto Focus",
                                            color = if (manualFocusDistance == null) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = if (manualFocusDistance != null) "Focus Distance: ${String.format(Locale.US, "%.2f m", 1.0f / (manualFocusDistance + 0.001f))}" else "Auto Focus Active (Tap to lock)",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )

                                Slider(
                                    value = manualFocusDistance ?: 0f,
                                    onValueChange = { onFocusDistanceChanged(it) },
                                    valueRange = 0f..10f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFB300),
                                        activeTrackColor = Color(0xFFFFB300),
                                        inactiveTrackColor = Color.DarkGray
                                    )
                                )
                            }
                        }

                        2 -> {
                            // White Balance Tab
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "White Balance Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(1 to "Auto", 5 to "Daylight", 6 to "Cloudy", 2 to "Incandescent", 3 to "Fluorescent").forEach { (wb, label) ->
                                        val isSel = whiteBalanceMode == wb
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) Color(0xFFFFB300) else Color(0x22FFFFFF))
                                                .clickable { onWhiteBalanceChanged(wb) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSel) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Overlays & Advanced: RAW, HDR, Peaking, Histogram, Grid, Level
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "RAW (DNG)", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = isRawEnabled,
                                        onCheckedChange = { onToggleRaw() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFFFFB300))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "HDR", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = isHdrEnabled,
                                        onCheckedChange = { onToggleHdr() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFFFFB300))
                                    )
                                }

                                HorizontalDivider(color = Color(0x22FFFFFF))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Focus Peaking", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = isFocusPeakingEnabled,
                                        onCheckedChange = { onToggleFocusPeaking() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFFFFB300))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Histogram", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = isHistogramEnabled,
                                        onCheckedChange = { onToggleHistogram() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFFFFB300))
                                    )
                                }

                                HorizontalDivider(color = Color(0x22FFFFFF))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Grid Lines", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = isGridVisible,
                                        onCheckedChange = { onToggleGrid() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFFFFB300))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = "Horizon Level", color = Color.White, fontSize = 13.sp)
                                    Switch(
                                        checked = isLevelVisible,
                                        onCheckedChange = { onToggleLevel() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFFFFB300))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
