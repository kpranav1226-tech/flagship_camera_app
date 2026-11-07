package com.clearshot.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.camera.CameraMode
import com.clearshot.ai.ui.theme.AccentBlue
import com.clearshot.ai.ui.theme.DarkGlass

data class ExtraModeItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val targetMode: CameraMode
)

/**
 * More Capture Modes grid sheet matching callout (16) in the Samsung Galaxy M15 5G design.
 * Features: Food, Panorama, Macro, Slow-mo, Hyperlapse, Document, Pro Video.
 */
@Composable
fun MoreModesSheet(
    onSelectMode: (CameraMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extraModes = listOf(
        ExtraModeItem("Macro", "2MP close-up detail", Icons.Filled.ZoomIn, CameraMode.MACRO),
        ExtraModeItem("Document", "High-contrast text", Icons.Filled.Description, CameraMode.DOCUMENT),
        ExtraModeItem("Pro Video", "Manual cine & multi-mic", Icons.Filled.Videocam, CameraMode.PRO_VIDEO),
        ExtraModeItem("Food", "Vivid color saturation", Icons.Filled.LocalDining, CameraMode.PHOTO),
        ExtraModeItem("Panorama", "Wide landscape sweep", Icons.Filled.Panorama, CameraMode.PHOTO),
        ExtraModeItem("Slow Motion", "High-speed 120 FPS", Icons.Filled.SlowMotionVideo, CameraMode.VIDEO),
        ExtraModeItem("Hyperlapse", "Time-lapse smoothing", Icons.Filled.FastForward, CameraMode.VIDEO)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(DarkGlass)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More Capture Modes",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(extraModes) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectMode(item.targetMode)
                                onDismiss()
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2430))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = AccentBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
