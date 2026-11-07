package com.clearshot.ai.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.camera.CameraMode
import com.clearshot.ai.processing.PipelineResult
import com.clearshot.ai.ui.components.SplitComparisonView
import com.clearshot.ai.ui.theme.AccentCyan
import com.clearshot.ai.ui.theme.BackgroundDark
import com.clearshot.ai.ui.theme.OverlayPill
import com.clearshot.ai.ui.theme.SurfaceDark
import java.io.File
import java.util.Locale

/**
 * Screen presenting interactive Before vs After slider comparison
 * alongside computational metrics and separate gallery export.
 */
@Composable
fun ComparisonScreen(
    originalFile: File,
    enhancedResult: PipelineResult,
    isSavedToGallery: Boolean,
    onSaveEnhancedClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(OverlayPill)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to camera",
                        tint = Color.White
                    )
                }

                Text(
                    text = "CLARITY COMPARISON",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.size(42.dp))
            }

            // Split Comparison View (weight 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                SplitComparisonView(
                    originalFile = originalFile,
                    enhancedFile = enhancedResult.enhancedFile,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Metrics & Action Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Metric Chips Row 1: Technical System Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricChip(
                        label = "RESOLUTION",
                        value = "${enhancedResult.originalWidth}x${enhancedResult.originalHeight}"
                    )
                    MetricChip(
                        label = "LATENCY",
                        value = "${enhancedResult.durationMs} ms"
                    )
                    MetricChip(
                        label = "MODE",
                        value = enhancedResult.cameraMode.title
                    )
                    MetricChip(
                        label = "BURST",
                        value = if (enhancedResult.burstFramesMerged > 1) "${enhancedResult.burstFramesMerged}x Burst" else "Single"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metric Chips Row 2: Computational Image Quality Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricChip(
                        label = "SNR (QUALITY)",
                        value = String.format(Locale.US, "%.1f dB", enhancedResult.analysis.snrDb)
                    )
                    MetricChip(
                        label = "DYN RANGE",
                        value = String.format(Locale.US, "%.1f EV", enhancedResult.analysis.dynamicRangeStops)
                    )
                    MetricChip(
                        label = "TENENGRAD",
                        value = String.format(Locale.US, "%.0f", enhancedResult.analysis.tenengradScore)
                    )
                    MetricChip(
                        label = "CLIPPING",
                        value = String.format(Locale.US, "H:%.0f%% S:%.0f%%", enhancedResult.analysis.highlightClippingPercent, enhancedResult.analysis.shadowClippingPercent)
                    )
                }

                if (enhancedResult.analysis.isHighDynamicRangeScene) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2E2713))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SMART HDR TRIGGERED • Scene DR %.1f EV exceeds 7.4 EV threshold".format(enhancedResult.analysis.dynamicRangeStops),
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (enhancedResult.motionDetected) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF143026))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ANTI-GHOSTING ACTIVE • Moving subject frozen (${(enhancedResult.motionRatio * 100).toInt()}% motion)",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (enhancedResult.horizonCorrected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B2A3D))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HORIZON STRAIGHTENED • %+.1f° tilt corrected (aspect preserved)".format(-enhancedResult.horizonCorrectionAngle),
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (enhancedResult.cameraMode == CameraMode.PORTRAIT) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF381E40))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFFE086FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PORTRAIT BOKEH • Optical lens depth blur & facial clarity applied",
                            color = Color(0xFFE086FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // EXIF Hardware Sheet
                com.clearshot.ai.ui.components.ExifDetailSheet(file = originalFile)

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onBackClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = OverlayPill),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Back to Camera",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = onSaveEnhancedClicked,
                        enabled = !isSavedToGallery,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSavedToGallery) Color.DarkGray else AccentCyan
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            imageVector = if (isSavedToGallery) Icons.Filled.Check else Icons.Filled.SaveAlt,
                            contentDescription = null,
                            tint = if (isSavedToGallery) Color.LightGray else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSavedToGallery) "Saved in Enhanced" else "Save Enhanced",
                            color = if (isSavedToGallery) Color.LightGray else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(OverlayPill)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
