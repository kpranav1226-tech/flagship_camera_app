package com.clearshot.ai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearshot.ai.ui.theme.AccentAmber

/**
 * Pro Manual Telemetry Readout Row matching callout (14) in the Samsung Galaxy M15 5G UI design:
 * Features:
 * - Focus target icon with micro distance tick marks
 * - Real-time Shutter Speed (SS)
 * - Exposure Value (EV)
 * - ISO Sensitivity
 * - Pro manual tuning sliders button
 */
@Composable
fun ProManualReadoutRow(
    shutterSpeedText: String,
    evText: String,
    isoText: String,
    onTuneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Focus Reticle & Distance Ticks
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CenterFocusStrong,
                contentDescription = "Focus Distance",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "||||||||||||",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Shutter Speed (SS) Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = shutterSpeedText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SS",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Exposure Value (EV) Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = evText,
                color = if (evText == "0.0") Color.White else AccentAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "EV",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ISO Sensitivity Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ISO",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = isoText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tuning / Manual Controls Drawer Button
        Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "Manual Controls",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .size(20.dp)
                .clickable { onTuneClick() }
        )
    }
}
