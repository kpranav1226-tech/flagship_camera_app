package com.clearshot.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.clearshot.ai.ui.theme.SurfaceDark

/**
 * Pro Mode parametric tuning sheet.
 * Exposes manual multi-frame burst length, exposure bias, and computational clarity parameters.
 */
@Composable
fun ProControlsPanel(
    isProMode: Boolean,
    burstCount: Int,
    exposureIndex: Int,
    minExposure: Int,
    maxExposure: Int,
    customSharpen: Float,
    customDenoise: Float,
    customClahe: Float,
    onBurstCountChanged: (Int) -> Unit,
    onExposureChanged: (Int) -> Unit,
    onSharpenChanged: (Float) -> Unit,
    onDenoiseChanged: (Float) -> Unit,
    onClaheChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isProMode,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(SurfaceDark.copy(alpha = 0.94f))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "PRO CLARITY PARAMETERS",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Burst Fusion Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Burst Mode",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1 to "1x Single", 3 to "3x HDR", 5 to "5x Ultra").forEach { (count, label) ->
                        FilterChip(
                            selected = burstCount == count,
                            onClick = { onBurstCountChanged(count) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.White.copy(alpha = 0.08f),
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Exposure Bias Slider
            TuningSlider(
                label = "EV Bias",
                valueText = if (exposureIndex > 0) "+$exposureIndex" else "$exposureIndex",
                value = exposureIndex.toFloat(),
                valueRange = minExposure.toFloat()..maxExposure.toFloat(),
                steps = maxExposure - minExposure - 1,
                onValueChange = { onExposureChanged(it.toInt()) }
            )

            // Sharpness Gain Slider
            TuningSlider(
                label = "Sharpness Amount",
                valueText = "%.2fx".format(customSharpen),
                value = customSharpen,
                valueRange = 0.3f..1.4f,
                onValueChange = onSharpenChanged
            )

            // Denoise Sigma Slider
            TuningSlider(
                label = "Denoise Strength",
                valueText = "σ %.0f".format(customDenoise),
                value = customDenoise,
                valueRange = 10f..45f,
                onValueChange = onDenoiseChanged
            )

            // CLAHE Clip Limit Slider
            TuningSlider(
                label = "CLAHE Contrast",
                valueText = "%.1f".format(customClahe),
                value = customClahe,
                valueRange = 1.0f..2.6f,
                onValueChange = onClaheChanged
            )
        }
    }
}

@Composable
private fun TuningSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(text = valueText, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
