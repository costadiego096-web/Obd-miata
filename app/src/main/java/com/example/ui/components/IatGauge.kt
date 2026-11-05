package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen
import com.example.ui.theme.TurboBlue
import com.example.ui.theme.WarningYellow

/**
 * Visual gauge for Intake Air Temperature (IAT - PID 010F)
 * Monitors heat-soak and intake air density on Mazda MX-5 NBFL.
 */
@Composable
fun IatGauge(
    iatTempC: Float,
    coolantTempC: Float,
    warningThresholdC: Float = 50f,
    modifier: Modifier = Modifier
) {
    val animatedIat by animateFloatAsState(
        targetValue = iatTempC,
        animationSpec = tween(80),
        label = "iat_temp"
    )

    val isHeatSoak = animatedIat >= warningThresholdC
    val (statusLabel, statusColor) = when {
        animatedIat < 28f -> Pair("ARIA FRESCA DENSITÀ OTTIMALE", TurboBlue)
        animatedIat < 45f -> Pair("TEMPERATURA NORMALE", TrackGreen)
        animatedIat < warningThresholdC -> Pair("ATTENZIONE RISCALDAMENTO", WarningYellow)
        else -> Pair("ALLARME HEAT-SOAK (CALO CV)", RaceRed)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isHeatSoak) RaceRed else CockpitOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Air,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TEMPERATURA ARIA ASPIRATA (IAT)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Big Numeric Value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.1f", animatedIat),
                        color = if (isHeatSoak) RaceRed else Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " °C",
                        color = statusColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Fahrenheit and Delta vs Coolant ECT
                Column(horizontalAlignment = Alignment.End) {
                    val tempF = (animatedIat * 9f / 5f) + 32f
                    Text(
                        text = "${String.format("%.1f", tempF)} °F",
                        color = Color(0xFFB0BCCB),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    val deltaEct = coolantTempC - animatedIat
                    Text(
                        text = "Delta Acqua: +${String.format("%.0f", deltaEct)}°C",
                        color = Color(0xFF8A99AC),
                        fontSize = 11.sp
                    )
                }
            }

            // Temperature Progress Bar
            val fraction = ((animatedIat - 10f) / (80f - 10f)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CockpitSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                )
            }

            // Heat-soak explanation warning
            if (isHeatSoak) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(RaceRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RaceRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aria calda nel vano motore: la centralina ritarda l'anticipo per evitare battito in testa.",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
