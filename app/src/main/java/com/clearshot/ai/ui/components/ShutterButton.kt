package com.clearshot.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.ShutterInner
import com.clearshot.ai.ui.theme.ShutterOuter
import com.clearshot.ai.ui.theme.ShutterRing

/**
 * Tactile, animated shutter button designed for premium camera ergonomics.
 * Dynamically switches between white photo shutter and red recording video trigger.
 */
@Composable
fun ShutterButton(
    isCapturing: Boolean,
    isVideo: Boolean = false,
    isRecording: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "shutter_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val outerBorderColor = when {
        isRecording -> Color(0xFFFF3B30).copy(alpha = pulseAlpha)
        isVideo -> Color(0xFFFF3B30)
        else -> ShutterOuter
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(width = 4.dp, color = outerBorderColor, shape = CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isCapturing,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow/ring buffer
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(if (isVideo) Color(0x33FF3B30) else ShutterRing)
        )

        // Inner solid shutter core
        val innerShape = if (isRecording) RoundedCornerShape(8.dp) else CircleShape
        val innerSize = if (isRecording) 32.dp else 62.dp
        val innerColor = when {
            isRecording -> Color(0xFFFF3B30)
            isVideo -> Color(0xFFFF3B30)
            isCapturing -> Color.LightGray
            else -> ShutterInner
        }

        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(innerShape)
                .background(innerColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = AccentCyan,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
