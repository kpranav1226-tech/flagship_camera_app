package com.clearshot.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.clearshot.ai.camera.FlashMode
import com.clearshot.ai.camera.VideoStabilizationMode
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.OverlayPill
import java.util.Locale

/**
 * On-Demand Camera Tools Bottom Sheet.
 * Kept completely hidden by default so the camera viewfinder remains 100% unobstructed.
 * Opens upon tapping the Tools (🛠️) button and offers 4 organized categories:
 * - Basic: Flash, HDR, Aspect Ratio, Timer, Grid, Horizon Level
 * - Image: Resolution, RAW Capture, AI Scene Optimizer, Processing Strength, Color Style
 * - Professional: ISO, Shutter Speed, Exposure (EV) Slider, Priority (A/M/S/I), White Balance
 * - Advanced: Histogram, Focus Peaking, Video EIS, Auto Horizon Correction
 */
@Composable
fun OnDemandToolsSheet(
    isVisible: Boolean,
    activeCategoryIndex: Int,
    onSelectCategory: (Int) -> Unit,
    onDismiss: () -> Unit,
    // Basic settings
    flashMode: FlashMode,
    onFlashModeChanged: (FlashMode) -> Unit,
    isHdrEnabled: Boolean,
    onToggleHdr: () -> Unit,
    aspectRatio: String,
    onToggleAspectRatio: () -> Unit,
    timerSeconds: Int,
    onCycleTimer: () -> Unit,
    isGridVisible: Boolean,
    onToggleGrid: () -> Unit,
    isLevelVisible: Boolean,
    onToggleLevel: () -> Unit,
    // Image settings
    photoResolution: String,
    onToggleResolution: () -> Unit,
    isRawEnabled: Boolean,
    onToggleRaw: () -> Unit,
    isSceneOptimizerEnabled: Boolean,
    onToggleSceneOptimizer: () -> Unit,
    processingStrength: String,
    onProcessingStrengthChanged: (String) -> Unit,
    colorStyle: String,
    onColorStyleChanged: (String) -> Unit,
    // Professional settings
    evValue: Float,
    onEvChanged: (Float) -> Unit,
    exposurePriority: String,
    onExposurePriorityChanged: (String) -> Unit,
    manualIso: Int?,
    onIsoChanged: (Int?) -> Unit,
    shutterSpeedText: String,
    onShutterSpeedChanged: (Long?) -> Unit,
    whiteBalanceMode: Int,
    onWhiteBalanceChanged: (Int) -> Unit,
    // Advanced settings
    isHistogramEnabled: Boolean,
    onToggleHistogram: () -> Unit,
    isFocusPeakingEnabled: Boolean,
    onToggleFocusPeaking: () -> Unit,
    stabilizationMode: VideoStabilizationMode,
    onStabilizationModeChanged: (VideoStabilizationMode) -> Unit,
    isAutoHorizonEnabled: Boolean,
    onToggleAutoHorizon: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Basic", "Image", "Professional", "Advanced")

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
                    .clickable(enabled = false) {} // Prevent click-through
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding()
            ) {
                // Header with title and Done button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Camera Tools",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "On-demand tuning • Tap Done to return to viewfinder",
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
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Tabs Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEachIndexed { index, name ->
                        val isSelected = activeCategoryIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                                .clickable { onSelectCategory(index) }
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

                // Active Category Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF181D27))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeCategoryIndex) {
                        0 -> BasicSettingsContent(
                            flashMode = flashMode,
                            onFlashModeChanged = onFlashModeChanged,
                            isHdrEnabled = isHdrEnabled,
                            onToggleHdr = onToggleHdr,
                            aspectRatio = aspectRatio,
                            onToggleAspectRatio = onToggleAspectRatio,
                            timerSeconds = timerSeconds,
                            onCycleTimer = onCycleTimer,
                            isGridVisible = isGridVisible,
                            onToggleGrid = onToggleGrid,
                            isLevelVisible = isLevelVisible,
                            onToggleLevel = onToggleLevel
                        )
                        1 -> ImageSettingsContent(
                            photoResolution = photoResolution,
                            onToggleResolution = onToggleResolution,
                            isRawEnabled = isRawEnabled,
                            onToggleRaw = onToggleRaw,
                            isSceneOptimizerEnabled = isSceneOptimizerEnabled,
                            onToggleSceneOptimizer = onToggleSceneOptimizer,
                            processingStrength = processingStrength,
                            onProcessingStrengthChanged = onProcessingStrengthChanged,
                            colorStyle = colorStyle,
                            onColorStyleChanged = onColorStyleChanged
                        )
                        2 -> ProfessionalSettingsContent(
                            evValue = evValue,
                            onEvChanged = onEvChanged,
                            exposurePriority = exposurePriority,
                            onExposurePriorityChanged = onExposurePriorityChanged,
                            manualIso = manualIso,
                            onIsoChanged = onIsoChanged,
                            shutterSpeedText = shutterSpeedText,
                            onShutterSpeedChanged = onShutterSpeedChanged,
                            whiteBalanceMode = whiteBalanceMode,
                            onWhiteBalanceChanged = onWhiteBalanceChanged
                        )
                        3 -> AdvancedSettingsContent(
                            isHistogramEnabled = isHistogramEnabled,
                            onToggleHistogram = onToggleHistogram,
                            isFocusPeakingEnabled = isFocusPeakingEnabled,
                            onToggleFocusPeaking = onToggleFocusPeaking,
                            stabilizationMode = stabilizationMode,
                            onStabilizationModeChanged = onStabilizationModeChanged,
                            isAutoHorizonEnabled = isAutoHorizonEnabled,
                            onToggleAutoHorizon = onToggleAutoHorizon
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicSettingsContent(
    flashMode: FlashMode,
    onFlashModeChanged: (FlashMode) -> Unit,
    isHdrEnabled: Boolean,
    onToggleHdr: () -> Unit,
    aspectRatio: String,
    onToggleAspectRatio: () -> Unit,
    timerSeconds: Int,
    onCycleTimer: () -> Unit,
    isGridVisible: Boolean,
    onToggleGrid: () -> Unit,
    isLevelVisible: Boolean,
    onToggleLevel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Flash Mode Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Flash Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(FlashMode.OFF, FlashMode.AUTO, FlashMode.ON, FlashMode.TORCH).forEach { mode ->
                    val isSelected = flashMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF))
                            .clickable { onFlashModeChanged(mode) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = mode.name,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // HDR & Aspect Ratio Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Auto HDR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isHdrEnabled,
                    onCheckedChange = { onToggleHdr() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFFFFB300)
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Ratio: ", color = Color.Gray, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFB300))
                        .clickable { onToggleAspectRatio() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = aspectRatio, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // Timer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Timer", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Off", 3 to "3s", 10 to "10s").forEach { (sec, label) ->
                    val isSelected = timerSeconds == sec
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF))
                            .clickable { if (!isSelected) onCycleTimer() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // Grid & Level Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Grid (3x3)", color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = isGridVisible,
                    onCheckedChange = { onToggleGrid() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFFFFB300)
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Horizon Level", color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = isLevelVisible,
                    onCheckedChange = { onToggleLevel() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFFFFB300)
                    )
                )
            }
        }
    }
}

@Composable
private fun ImageSettingsContent(
    photoResolution: String,
    onToggleResolution: () -> Unit,
    isRawEnabled: Boolean,
    onToggleRaw: () -> Unit,
    isSceneOptimizerEnabled: Boolean,
    onToggleSceneOptimizer: () -> Unit,
    processingStrength: String,
    onProcessingStrengthChanged: (String) -> Unit,
    colorStyle: String,
    onColorStyleChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Resolution & RAW Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Photo Resolution", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Sensor output resolution", color = Color.Gray, fontSize = 10.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFB300))
                    .clickable { onToggleResolution() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = photoResolution, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // RAW Capture
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "RAW (DNG) Capture", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Save uncompressed sensor frames", color = Color.Gray, fontSize = 10.sp)
            }

            Switch(
                checked = isRawEnabled,
                onCheckedChange = { onToggleRaw() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFFB300)
                )
            )
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // AI Scene Optimizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Scene Optimizer", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Auto-detect scenery, sky, food & night", color = Color.Gray, fontSize = 10.sp)
            }

            Switch(
                checked = isSceneOptimizerEnabled,
                onCheckedChange = { onToggleSceneOptimizer() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFFB300)
                )
            )
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // Processing Strength
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Clarity Profile", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Natural", "Balanced", "Max Clarity").forEach { strength ->
                    val isSelected = processingStrength == strength
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF))
                            .clickable { onProcessingStrengthChanged(strength) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = strength,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalSettingsContent(
    evValue: Float,
    onEvChanged: (Float) -> Unit,
    exposurePriority: String,
    onExposurePriorityChanged: (String) -> Unit,
    manualIso: Int?,
    onIsoChanged: (Int?) -> Unit,
    shutterSpeedText: String,
    onShutterSpeedChanged: (Long?) -> Unit,
    whiteBalanceMode: Int,
    onWhiteBalanceChanged: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Exposure Priority Selector (A, M, S, I)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Priority Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("A" to "Auto", "M" to "Manual", "S" to "Shutter", "I" to "ISO").forEach { (code, _) ->
                    val isSelected = exposurePriority == code
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF))
                            .clickable { onExposurePriorityChanged(code) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = code,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // EV Slider (-2.0 to +2.0)
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

        HorizontalDivider(color = Color(0x22FFFFFF))

        // ISO Row
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
                val isoList = listOf(null, 50, 100, 200, 400, 800, 1600, 3200)
                isoList.forEach { iso ->
                    val isSelected = manualIso == iso
                    val label = iso?.toString() ?: "Auto"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF))
                            .clickable { onIsoChanged(iso) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // White Balance Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "White Balance", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1 to "Auto", 5 to "Daylight", 6 to "Cloudy", 2 to "Incandescent", 3 to "Fluorescent").forEach { (wb, label) ->
                    val isSelected = whiteBalanceMode == wb
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF))
                            .clickable { onWhiteBalanceChanged(wb) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsContent(
    isHistogramEnabled: Boolean,
    onToggleHistogram: () -> Unit,
    isFocusPeakingEnabled: Boolean,
    onToggleFocusPeaking: () -> Unit,
    stabilizationMode: VideoStabilizationMode,
    onStabilizationModeChanged: (VideoStabilizationMode) -> Unit,
    isAutoHorizonEnabled: Boolean,
    onToggleAutoHorizon: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Live Histogram & Focus Peaking
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Histogram", color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = isHistogramEnabled,
                    onCheckedChange = { onToggleHistogram() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFFFFB300)
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Focus Peaking", color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = isFocusPeakingEnabled,
                    onCheckedChange = { onToggleFocusPeaking() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFFFFB300)
                    )
                )
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // Video Stabilization EIS
        Column {
            Text(text = "Video EIS Stabilization", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoStabilizationMode.entries.forEach { mode ->
                    val isSelected = stabilizationMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF))
                            .clickable { onStabilizationModeChanged(mode) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.title,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))

        // Auto Horizon Correction
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Auto Horizon Correction", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Automatic leveling on photo capture", color = Color.Gray, fontSize = 10.sp)
            }

            Switch(
                checked = isAutoHorizonEnabled,
                onCheckedChange = onToggleAutoHorizon,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFFFB300)
                )
            )
        }
    }
}
