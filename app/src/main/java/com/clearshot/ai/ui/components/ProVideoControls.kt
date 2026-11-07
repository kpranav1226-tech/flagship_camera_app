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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.clearshot.ai.audio.AudioInputDevice
import com.clearshot.ai.camera.CameraSpecs
import com.clearshot.ai.camera.VideoStabilizationMode
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.OverlayPill

private enum class ProTab(val label: String) {
    CAMERA("CAM"),
    AUDIO("MIC"),
    EXPOSURE("EXP/ISO"),
    FOCUS("FOCUS"),
    WHITE_BALANCE("WB"),
    VIDEO_FORMAT("FORMAT")
}

/**
 * Professional Video Recording Interface.
 *
 * Default screen:
 * - Large live preview
 * - Recording button
 * - Recording timer
 * - Camera selection shortcut
 * - One Pro Controls button
 *
 * When Pro Controls is tapped:
 * - Slides open organized tabs for Camera, Mic, Exposure/ISO, Focus, WB, Format, VU meter
 * - Allows safe adjustment during active recording without stopping recording
 * - Dismisses with Done button to return to clean preview
 */
@Composable
fun ProVideoControls(
    isRecording: Boolean,
    isPaused: Boolean,
    durationSeconds: Long,
    quality: String,
    fps: Int,
    bitrateMbps: Int,
    stabilizationMode: VideoStabilizationMode,
    availableCameras: List<CameraSpecs>,
    activeCameraId: String,
    concurrentCameraSupported: Boolean,
    concurrentCameraNote: String?,
    availableAudioDevices: List<AudioInputDevice>,
    selectedAudioDevice: AudioInputDevice?,
    audioLevelDb: Float,
    isAudioClipping: Boolean,
    manualIso: Int?,
    manualShutterNanos: Long?,
    manualFocusDistance: Float?,
    whiteBalanceMode: Int,
    isFocusLocked: Boolean,
    isExposureLocked: Boolean,
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    isProSheetOpen: Boolean = false,
    onToggleProSheet: () -> Unit = {},
    onDismissProSheet: () -> Unit = {},
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSelectCameraId: (String) -> Unit,
    onSelectAudioDevice: (AudioInputDevice) -> Unit,
    onStabilizationModeChanged: (VideoStabilizationMode) -> Unit,
    onQualityChanged: (String) -> Unit,
    onFpsChanged: (Int) -> Unit,
    onBitrateChanged: (Int) -> Unit,
    onIsoChanged: (Int?) -> Unit,
    onShutterSpeedChanged: (Long?) -> Unit,
    onFocusDistanceChanged: (Float?) -> Unit,
    onWhiteBalanceChanged: (Int) -> Unit,
    onToggleFocusLock: () -> Unit,
    onToggleExposureLock: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ProTab.EXPOSURE) }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timerText = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Minimal Top status indicator for Pro Video
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xB310141D))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color(0xFFFF2D55) else Color.Gray)
            )
            Text(
                text = if (isRecording) "PRO REC $timerText" else "PRO STANDBY",
                color = if (isRecording) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• $quality ${fps}p",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Default Clean Action Bar: Camera Shortcut, Shutter/Record, Pro Controls Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera selection shortcut button
            val activeName = availableCameras.find { it.cameraId == activeCameraId }?.name ?: "CAM"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(OverlayPill)
                    .clickable {
                        // Quick switch between available rear cameras or front
                        val nextCam = if (activeCameraId == "0") "2" else if (activeCameraId == "2") "1" else "0"
                        onSelectCameraId(nextCam)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (activeCameraId == "2") "0.6x Ultra" else if (activeCameraId == "1") "Front" else "1x Wide",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Shutter / Pause / Stop controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isRecording) {
                    IconButton(
                        onClick = if (isPaused) onResumeRecording else onPauseRecording,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(OverlayPill)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                ShutterButton(
                    isCapturing = false,
                    isVideo = true,
                    isRecording = isRecording,
                    onClick = {
                        if (isRecording) onStopRecording() else onStartRecording()
                    }
                )

                if (isRecording) {
                    IconButton(
                        onClick = onStopRecording,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // One Pro Controls Button (🎛️)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isProSheetOpen) Color(0xFFFFB300) else OverlayPill)
                    .clickable { onToggleProSheet() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Pro Controls",
                        tint = if (isProSheetOpen) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Pro",
                        color = if (isProSheetOpen) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // On-Demand Pro Video Sheet
    AnimatedVisibility(
        visible = isProSheetOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismissProSheet() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xF010141D))
                    .clickable(enabled = false) {}
                    .padding(horizontal = 18.dp, vertical = 14.dp)
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
                            text = "Pro Video Settings",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Safe adjustment during active recording",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFB300))
                            .clickable { onDismissProSheet() }
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-Time Audio Level VU Meter
                AudioVuMeter(
                    audioLevelDb = audioLevelDb,
                    isClipping = isAudioClipping,
                    selectedDevice = selectedAudioDevice
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Professional Tool Tabs Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProTab.entries.forEach { tab ->
                        val isSelected = tab == activeTab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF))
                                .clickable { activeTab = tab }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tab.label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Tab Content Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF181D27))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (activeTab) {
                        ProTab.CAMERA -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    availableCameras.forEach { cam ->
                                        val isSelected = cam.cameraId == activeCameraId
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFFFB300) else Color(0xFF242B38))
                                                .clickable { onSelectCameraId(cam.cameraId) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = cam.name,
                                                    color = if (isSelected) Color.Black else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "%.1f MP • %s".format(
                                                        cam.resolutionMegapixels,
                                                        if (cam.apertures.isNotEmpty()) "f/%.1f".format(cam.apertures[0]) else "f/1.8"
                                                    ),
                                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else Color.Gray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                if (concurrentCameraNote != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = concurrentCameraNote,
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        ProTab.AUDIO -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Audio Source:",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    availableAudioDevices.forEach { dev ->
                                        val isSelected = dev.id == selectedAudioDevice?.id
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFFFB300) else Color(0xFF242B38))
                                                .clickable { onSelectAudioDevice(dev) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "${dev.type.label} (${dev.name})",
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ProTab.EXPOSURE -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ISO: ${manualIso?.toString() ?: "Auto"}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = onToggleExposureLock,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExposureLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "AE Lock",
                                            tint = if (isExposureLocked) Color(0xFFFFB300) else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(null, 50, 100, 200, 400, 800, 1600, 3200).forEach { iso ->
                                        val isSelected = manualIso == iso
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) Color(0xFFFFB300) else Color(0xFF242B38))
                                                .clickable { onIsoChanged(iso) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = iso?.toString() ?: "Auto",
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ProTab.FOCUS -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (manualFocusDistance == null) "AF Mode" else "MF: ${"%.2f".format(manualFocusDistance)}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = onToggleFocusLock,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isFocusLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "AF Lock",
                                            tint = if (isFocusLocked) Color(0xFFFFB300) else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Slider(
                                    value = manualFocusDistance ?: 0f,
                                    onValueChange = { onFocusDistanceChanged(it) },
                                    valueRange = 0f..10f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFB300),
                                        activeTrackColor = Color(0xFFFFB300)
                                    )
                                )
                            }
                        }

                        ProTab.WHITE_BALANCE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1 to "Auto", 5 to "Daylight", 6 to "Cloudy", 2 to "Incandescent", 3 to "Fluorescent").forEach { (wb, label) ->
                                    val isSelected = whiteBalanceMode == wb
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFFFB300) else Color(0xFF242B38))
                                            .clickable { onWhiteBalanceChanged(wb) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        ProTab.VIDEO_FORMAT -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Quality
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("1080p", "720p").forEach { q ->
                                            val isSel = quality == q
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSel) Color(0xFFFFB300) else Color(0xFF242B38))
                                                    .clickable { onQualityChanged(q) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(text = q, color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // FPS
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(30, 60).forEach { f ->
                                            val isSel = fps == f
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSel) Color(0xFFFFB300) else Color(0xFF242B38))
                                                    .clickable { onFpsChanged(f) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(text = "${f}fps", color = if (isSel) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Bitrate
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(6, 9, 14, 20).forEach { b ->
                                        val isSel = bitrateMbps == b
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFFFFB300) else Color(0xFF242B38))
                                                .clickable { onBitrateChanged(b) }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(text = "${b}M", color = if (isSel) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
