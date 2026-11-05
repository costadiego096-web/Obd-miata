package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen
import com.example.ui.theme.WarningYellow

/**
 * GT3 / F1 style sequential LED shift light bar.
 * Progresses from Green -> Yellow -> Red -> Flash Shift Warning!
 */
@Composable
fun ShiftLightBar(
    currentRpm: Float,
    shiftLightRpm: Float = 6800f,
    redlineRpm: Float = 7200f,
    modifier: Modifier = Modifier
) {
    val totalLeds = 12
    val startThreshold = shiftLightRpm * 0.70f // lights start turning on at 70% of shift rpm

    val isShiftFlash = currentRpm >= shiftLightRpm

    val infiniteTransition = rememberInfiniteTransition(label = "shift_flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash"
    )

    val activeLeds = if (currentRpm < startThreshold) {
        0
    } else {
        val fraction = ((currentRpm - startThreshold) / (shiftLightRpm - startThreshold)).coerceIn(0f, 1f)
        (fraction * totalLeds).toInt().coerceIn(1, totalLeds)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CockpitSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalLeds) {
                val isOn = i < activeLeds

                val ledColor = when {
                    isShiftFlash -> Color(0xFFE040FB).copy(alpha = flashAlpha) // Violet Flash
                    i < 4 -> TrackGreen
                    i < 8 -> WarningYellow
                    i < 10 -> Mx5Amber
                    else -> RaceRed
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isOn || isShiftFlash) ledColor else CockpitOutline.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

/**
 * Compact horizontal parameter progress bar with colored status thresholds.
 */
@Composable
fun HorizontalBarGauge(
    label: String,
    value: Float,
    unit: String,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    warningThreshold: Float? = null,
    dangerThreshold: Float? = null,
    modifier: Modifier = Modifier
) {
    val fraction = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)

    val barColor = when {
        dangerThreshold != null && value >= dangerThreshold -> RaceRed
        warningThreshold != null && value >= warningThreshold -> WarningYellow
        else -> Mx5Amber
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color(0xFFB0BCCB),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${String.format("%.1f", value)} $unit",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CockpitSurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}
