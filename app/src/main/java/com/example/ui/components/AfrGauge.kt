package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MixtureStatus
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.ShiftCyan
import com.example.ui.theme.TrackGreen
import com.example.ui.theme.TurboBlue
import com.example.ui.theme.WarningYellow

/**
 * Visual gauge for Air-Fuel Ratio (AFR) & Narrowband Lambda O2 Sensor
 * specifically tuned for the Mazda MX-5 NBFL OEM Lambda sensor.
 */
@Composable
fun AfrGauge(
    afr: Float,
    lambda: Float,
    o2Voltage: Float,
    mixtureStatus: MixtureStatus,
    modifier: Modifier = Modifier
) {
    val animatedAfr by animateFloatAsState(
        targetValue = afr.coerceIn(10.0f, 19.0f),
        animationSpec = tween(70),
        label = "afr_val"
    )

    val (statusColor, statusBg) = when (mixtureStatus) {
        MixtureStatus.RICH -> Pair(TurboBlue, TurboBlue.copy(alpha = 0.2f))
        MixtureStatus.STOICHIOMETRIC -> Pair(TrackGreen, TrackGreen.copy(alpha = 0.2f))
        MixtureStatus.LEAN -> Pair(WarningYellow, WarningYellow.copy(alpha = 0.2f))
        MixtureStatus.CUTOFF -> Pair(RaceRed, RaceRed.copy(alpha = 0.2f))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RAPPORTO STECHIOMETRICO (AFR)",
                        color = Color(0xFF8A99AC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sonda Lambda Originale O2 NBFL",
                        color = Color(0xFF6B7A8D),
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg)
                        .border(1.dp, statusColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = mixtureStatus.label,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Big Numeric AFR Readout & Lambda / Voltage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%.2f", animatedAfr),
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " : 1",
                        color = Mx5Amber,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                }

                // Lambda and Voltage Pill
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "λ = ${String.format("%.3f", lambda)}",
                        color = statusColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Sonda: ${String.format("%.3f", o2Voltage)} V",
                        color = Color(0xFFB0BCCB),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Continuous Spectrum Bar: Rich (Blue) -> Stoich (Green) -> Lean (Yellow) -> Cutoff (Red)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CockpitSurfaceVariant)
            ) {
                // Gradient bar: 11.0 AFR to 18.0 AFR
                Canvas(modifier = Modifier.fillMaxWidth().height(14.dp).align(Alignment.Center)) {
                    val w = size.width
                    val h = size.height

                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TurboBlue,     // 11.0 - 13.0 Rich
                                TrackGreen,    // 14.7 Stoichiometric
                                WarningYellow, // 15.5 Lean
                                RaceRed        // 17.0+ Cutoff
                            )
                        ),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )

                    // Target stoichiometric line at 14.7 AFR
                    // fraction = (14.7 - 11.0) / (18.0 - 11.0) = 3.7 / 7.0 = ~0.528
                    val stoichX = ((14.7f - 11.0f) / 7.0f) * w
                    drawLine(
                        color = Color.White,
                        start = Offset(stoichX, -2f),
                        end = Offset(stoichX, h + 2f),
                        strokeWidth = 2f
                    )

                    // Current needle indicator
                    val currentFraction = ((animatedAfr - 11.0f) / 7.0f).coerceIn(0f, 1f)
                    val indicatorX = currentFraction * w

                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = Offset(indicatorX, h / 2f)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 4f,
                        center = Offset(indicatorX, h / 2f)
                    )
                }
            }

            // Legend Ticks
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "11.0 (RICCA)", color = TurboBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "14.7 (STECH)", color = TrackGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "16.0 (MAGRA)", color = WarningYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(text = "18.0 (CUT-OFF)", color = RaceRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
