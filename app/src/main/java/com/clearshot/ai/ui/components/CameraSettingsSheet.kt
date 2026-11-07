package com.clearshot.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.StayPrimaryLandscape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.camera.OrientationLock
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.OverlayPill
import com.clearshot.ai.ui.theme.SurfaceDark

/**
 * Camera Settings Sheet providing dedicated controls for Horizon Lock and Orientation Lock.
 * Built with smooth animated slide-up transition, dimming backdrop, and touch outside dismissal.
 */
@Composable
fun CameraSettingsSheet(
    isVisible: Boolean,
    orientationLock: OrientationLock,
    isHorizonLevelEnabled: Boolean,
    isAutoHorizonCorrectionEnabled: Boolean,
    horizonToleranceDeg: Float,
    onOrientationLockChanged: (OrientationLock) -> Unit,
    onToggleHorizonLevel: () -> Unit,
    onToggleAutoHorizonCorrection: (Boolean) -> Unit,
    onHorizonToleranceChanged: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(SurfaceDark)
                    .clickable(enabled = false) {} // Prevent click-through dismissal
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAMERA SETTINGS",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OverlayPill)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close settings",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 1: ORIENTATION LOCK
                Text(
                    text = "VIEWFINDER ORIENTATION LOCK",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Locks interface posture to prevent accidental rotation while shooting.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Segmented Orientation Controls (Auto | Portrait | Landscape)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E2128))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OrientationSegment(
                        title = "Auto",
                        icon = Icons.Default.ScreenRotation,
                        isSelected = orientationLock == OrientationLock.AUTO,
                        onClick = { onOrientationLockChanged(OrientationLock.AUTO) },
                        modifier = Modifier.weight(1f)
                    )
                    OrientationSegment(
                        title = "Portrait",
                        icon = Icons.Default.StayCurrentPortrait,
                        isSelected = orientationLock == OrientationLock.PORTRAIT,
                        onClick = { onOrientationLockChanged(OrientationLock.PORTRAIT) },
                        modifier = Modifier.weight(1f)
                    )
                    OrientationSegment(
                        title = "Landscape",
                        icon = Icons.Default.StayPrimaryLandscape,
                        isSelected = orientationLock == OrientationLock.LANDSCAPE,
                        onClick = { onOrientationLockChanged(OrientationLock.LANDSCAPE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECTION 2: HORIZON LOCK & LEVELING
                Text(
                    text = "HORIZON STABILIZATION & LEVELING",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 1: Virtual Horizon Indicator Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2128))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Virtual Horizon Reticle",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Real-time leveling indicator on viewfinder (Rotation Vector + Accelerometer)",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isHorizonLevelEnabled,
                        onCheckedChange = { onToggleHorizonLevel() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AccentCyan,
                            uncheckedTrackColor = OverlayPill
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle 2: Auto Horizon Correction on Captured Photo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2128))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Auto Horizon Correction",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CropRotate,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Straightens tilted captures (±15°) with aspect-ratio preserving crop",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isAutoHorizonCorrectionEnabled,
                        onCheckedChange = { onToggleAutoHorizonCorrection(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFF00E676),
                            uncheckedTrackColor = OverlayPill
                        )
                    )
                }

                // Tolerance Selector (Visible when Horizon features active)
                AnimatedVisibility(
                    visible = isHorizonLevelEnabled || isAutoHorizonCorrectionEnabled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        Text(
                            text = "LEVELING TOLERANCE",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { tolerance ->
                                val isSelected = (horizonToleranceDeg == tolerance)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AccentCyan else Color(0xFF1E2128))
                                        .clickable { onHorizonToleranceChanged(tolerance) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "±${tolerance}°",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OrientationSegment(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) AccentCyan else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.Black else Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
