package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.TrackGreen

/**
 * Live real-time waveform sparkline card for monitoring trends
 * (such as AFR dithering, IAT warm-up, or RPM sweeps).
 */
@Composable
fun SparklineGauge(
    title: String,
    unit: String,
    currentValue: Float,
    history: List<Float>,
    lineColor: Color = TrackGreen,
    modifier: Modifier = Modifier
) {
    val minVal = (history.minOrNull() ?: (currentValue - 1f)).coerceAtLeast(0f)
    val maxVal = (history.maxOrNull() ?: (currentValue + 1f)).coerceAtLeast(minVal + 0.1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFFB0BCCB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${if (currentValue >= 100) String.format("%.0f", currentValue) else String.format("%.2f", currentValue)} $unit",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Canvas Waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CockpitSurfaceVariant)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp)) {
                    val w = size.width
                    val h = size.height

                    if (history.size >= 2) {
                        val path = Path()
                        val range = (maxVal - minVal).coerceAtLeast(0.1f)

                        history.forEachIndexed { index, value ->
                            val x = (index.toFloat() / (history.size - 1)) * w
                            val y = h - ((value - minVal) / range) * h

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        // Draw smooth line
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                        )

                        // Draw live pulse dot on last point
                        val lastY = h - ((currentValue - minVal) / range) * h
                        drawCircle(
                            color = lineColor.copy(alpha = 0.4f),
                            radius = 6f,
                            center = Offset(w, lastY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(w, lastY)
                        )
                    }
                }

                // Min and Max labels overlay
                Text(
                    text = "Max: ${String.format("%.1f", maxVal)}",
                    color = Color(0xFF6B7A8D),
                    fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
                Text(
                    text = "Min: ${String.format("%.1f", minVal)}",
                    color = Color(0xFF6B7A8D),
                    fontSize = 8.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                )
            }
        }
    }
}
