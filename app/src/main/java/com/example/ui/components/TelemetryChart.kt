package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TelemetrySampleEntity
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen

/**
 * High performance Canvas line graph plotting vehicle speed and RPM curves for lap telemetry analysis.
 */
@Composable
fun TelemetryChart(
    samples: List<TelemetrySampleEntity>,
    title: String = "Profilo Velocità e Giri Motore",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(CockpitSurface, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        if (samples.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nessun dato di telemetria registrato",
                    color = Color(0xFF6B7A8D),
                    fontSize = 12.sp
                )
            }
            return@Box
        }

        val maxSpeed = samples.maxOfOrNull { it.speedKmh }?.coerceAtLeast(120f) ?: 180f
        val maxRpm = 8000f

        Column(modifier = Modifier.fillMaxSize()) {
            // Chart legend and peaks
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Speed Legend
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                    Box(modifier = Modifier.height(3.dp).padding(end = 4.dp).background(TrackGreen))
                    Text(
                        text = "Vel. Max: ${String.format("%.0f", maxSpeed)} km/h",
                        color = TrackGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // RPM Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.height(3.dp).padding(end = 4.dp).background(Mx5Amber))
                    Text(
                        text = "RPM",
                        color = Mx5Amber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Canvas Plot Area
            Canvas(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
                val w = size.width
                val h = size.height

                // Draw background horizontal gridlines (e.g. 50 km/h, 100 km/h, 150 km/h)
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = h * (i.toFloat() / gridLines)
                    drawLine(
                        color = CockpitOutline.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Downsample if samples > width pixels
                val step = (samples.size / w.toInt()).coerceAtLeast(1)
                val sampledList = samples.filterIndexed { index, _ -> index % step == 0 }

                if (sampledList.size < 2) return@Canvas

                val speedPath = Path()
                val rpmPath = Path()

                sampledList.forEachIndexed { index, point ->
                    val x = (index.toFloat() / (sampledList.size - 1)) * w
                    val speedY = h - (point.speedKmh / maxSpeed) * h
                    val rpmY = h - (point.rpm / maxRpm) * h

                    if (index == 0) {
                        speedPath.moveTo(x, speedY)
                        rpmPath.moveTo(x, rpmY)
                    } else {
                        speedPath.lineTo(x, speedY)
                        rpmPath.lineTo(x, rpmY)
                    }
                }

                // Draw RPM curve (thinner amber)
                drawPath(
                    path = rpmPath,
                    color = Mx5Amber.copy(alpha = 0.65f),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                )

                // Draw Speed curve (bold emerald green)
                drawPath(
                    path = speedPath,
                    color = TrackGreen,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
