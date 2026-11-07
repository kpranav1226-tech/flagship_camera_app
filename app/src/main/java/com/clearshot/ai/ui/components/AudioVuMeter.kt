package com.clearshot.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.audio.AudioInputDevice
import com.clearshot.ai.audio.AudioInputType

/**
 * Professional real-time stereo/mono audio VU level meter for Pro Video mode.
 * Features dBFS amplitude gradient, dynamic clip threshold alert, and active mic badge.
 */
@Composable
fun AudioVuMeter(
    audioLevelDb: Float,
    isClipping: Boolean,
    selectedDevice: AudioInputDevice?,
    modifier: Modifier = Modifier
) {
    // Normalize dB from [-60, 0] to [0.0, 1.0]
    val normalized = ((audioLevelDb + 60f) / 60f).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = normalized,
        animationSpec = tween(durationMillis = 60),
        label = "vu_bar"
    )

    val clipColor by animateColorAsState(
        targetValue = if (isClipping) Color(0xFFFF2D55) else Color.Transparent,
        animationSpec = tween(durationMillis = 100),
        label = "clip_color"
    )

    val micIcon = when (selectedDevice?.type) {
        AudioInputType.WIRED_HEADSET -> Icons.Default.Headset
        AudioInputType.BLUETOOTH_SCO -> Icons.Default.Bluetooth
        AudioInputType.USB_AUDIO -> Icons.Default.Usb
        else -> Icons.Default.Mic
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xCC121418))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = micIcon,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedDevice?.name ?: "Internal Mic",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isClipping) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(clipColor)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "CLIP",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = "%.0f dBFS".format(audioLevelDb),
                    color = if (isClipping) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Gradient Level Bar (-60 dB to 0 dB)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF22262E))
        ) {
            val vuBrush = Brush.horizontalGradient(
                listOf(
                    Color(0xFF00E676), // Green (-60 to -18)
                    Color(0xFFFFD600), // Yellow (-18 to -6)
                    Color(0xFFFF1744)  // Red (-6 to 0)
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(vuBrush)
            )
        }
    }
}
