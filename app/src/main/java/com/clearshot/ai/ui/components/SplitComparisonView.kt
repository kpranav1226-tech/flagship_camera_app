package com.clearshot.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.OverlayPill
import java.io.File

/**
 * Interactive Before / After split-screen slider for photograph clarity evaluation.
 */
@Composable
fun SplitComparisonView(
    originalFile: File,
    enhancedFile: File,
    modifier: Modifier = Modifier
) {
    var sliderRatio by remember { mutableFloatStateOf(0.5f) }
    var containerSize by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it.width.toFloat() }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (containerSize > 0f) {
                        sliderRatio = (sliderRatio + dragAmount.x / containerSize).coerceIn(0.05f, 0.95f)
                    }
                }
            }
    ) {
        // Layer 1: Enhanced image (underneath, full width)
        AsyncImage(
            model = enhancedFile,
            contentDescription = "Enhanced Photograph",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Layer 2: Original image (clipped to sliderRatio on the left)
        AsyncImage(
            model = originalFile,
            contentDescription = "Original Photograph",
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val clipWidth = size.width * sliderRatio
                    clipRect(left = 0f, top = 0f, right = clipWidth, bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                },
            contentScale = ContentScale.Fit
        )

        // Top labels
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OverlayPill)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "ORIGINAL",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OverlayPill)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "ENHANCED",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Layer 3: Vertical slider divider line & thumb
        val dividerX = containerSize * sliderRatio

        // Vertical line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.5.dp)
                .offset { IntOffset(dividerX.toInt(), 0) }
                .background(Color.White)
        )

        // Center drag handle thumb
        Box(
            modifier = Modifier
                .size(42.dp)
                .offset { IntOffset((dividerX - 21.dp.toPx()).toInt(), 0) }
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White)
                .border(width = 2.dp, color = AccentCyan, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                contentDescription = "Comparison slider",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
