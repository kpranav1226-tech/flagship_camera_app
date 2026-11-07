package com.clearshot.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.clearshot.ai.camera.VideoStabilizationMode
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.OverlayPill

/**
 * Clean, accessible everyday video recording controls for Normal Video Mode.
 */
@Composable
fun NormalVideoControls(
    isRecording: Boolean,
    isPaused: Boolean,
    durationSeconds: Long,
    quality: String,
    fps: Int,
    stabilizationMode: VideoStabilizationMode,
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onQualityToggle: () -> Unit,
    onFpsToggle: () -> Unit,
    onStabilizationCycle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timerText = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording Status & Timer HUD
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDDCC0000))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timerText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Setting Badges (Resolution, FPS, Stabilization)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quality Toggle (1080p / 720p)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OverlayPill)
                    .clickable(enabled = !isRecording, onClick = onQualityToggle)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = quality,
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // FPS Toggle (30 / 60)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OverlayPill)
                    .clickable(enabled = !isRecording, onClick = onFpsToggle)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "${fps} FPS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Stabilization Selector
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (stabilizationMode != VideoStabilizationMode.OFF) Color(0x3300E5FF) else OverlayPill)
                    .clickable(enabled = !isRecording, onClick = onStabilizationCycle)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (stabilizationMode != VideoStabilizationMode.OFF) AccentCyan else Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stabilizationMode.title,
                        color = if (stabilizationMode != VideoStabilizationMode.OFF) AccentCyan else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Zoom Slider / Presets
        ZoomControls(
            currentZoom = currentZoom,
            minZoom = minZoom,
            maxZoom = maxZoom,
            onZoomChanged = onZoomChanged
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom Action Bar: Camera Switch / Record / Pause / Stop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action: Pause/Resume while recording, or Camera Switch when idle
            if (isRecording) {
                IconButton(
                    onClick = if (isPaused) onResumeRecording else onPauseRecording,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(OverlayPill)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(OverlayPill)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center: Big Shutter / Stop Trigger
            ShutterButton(
                isCapturing = false,
                isVideo = true,
                isRecording = isRecording,
                onClick = {
                    if (isRecording) onStopRecording() else onStartRecording()
                }
            )

            // Right Action: Stop button when recording, or spacer
            if (isRecording) {
                IconButton(
                    onClick = onStopRecording,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Recording",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.size(50.dp))
            }
        }
    }
}
