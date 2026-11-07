package com.clearshot.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
 * Non-blocking status pill displayed at the top of the viewfinder while background
 * AI enhancement executes. Allows uninterrupted photo framing and shooting.
 */
@Composable
fun BackgroundEnhancementIndicator(
    isEnhancing: Boolean,
    progress: Float,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isEnhancing,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xDD0D1B2A))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(14.dp),
                    color = AccentCyan,
                    strokeWidth = 2.dp,
                    trackColor = Color(0x3300E5FF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (statusMessage.isNotBlank()) statusMessage else "AI Enhancing...",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
