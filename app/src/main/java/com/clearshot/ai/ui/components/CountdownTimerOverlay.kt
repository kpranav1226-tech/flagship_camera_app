package com.clearshot.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.ui.theme.AccentCyan

/**
 * Animated on-screen countdown indicator (3, 2, 1) before shutter activation.
 */
@Composable
fun CountdownTimerOverlay(
    secondsRemaining: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = secondsRemaining > 0,
        enter = fadeIn(tween(150)) + scaleIn(tween(150)),
        exit = fadeOut(tween(150)) + scaleOut(tween(150)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = secondsRemaining.toString(),
                    color = AccentCyan,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
