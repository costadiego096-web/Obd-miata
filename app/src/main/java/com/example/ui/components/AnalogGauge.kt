package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic Mazda MX-5 NBFL inspired analog dial gauge.
 * Features 240-degree sweep, amber needle with center boss,
 * radial calibration ticks, and redline zone.
 */
@Composable
fun AnalogGauge(
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 8000f,
    title: String = "RPM",
    unit: String = "x1000",
    redlineStart: Float? = 7000f,
    warningStart: Float? = null,
    isWarning: Boolean = false,
    majorTickStep: Float = 1000f,
    minorTicksPerMajor: Int = 4,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(minValue, maxValue),
        animationSpec = tween(durationMillis = 80),
        label = "gauge_needle"
    )

    val startAngle = 140f
    val sweepAngle = 260f

    val fraction = ((animatedValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    val needleAngle = startAngle + (fraction * sweepAngle)

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f) - 10f

            // 1. Dial Bezel & Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CockpitSurface, Color(0xFF0F1218)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer metallic rim
            drawCircle(
                color = CockpitOutline,
                radius = radius,
                center = center,
                style = Stroke(width = 4f)
            )

            // 2. Redline / Warning Arc
            if (redlineStart != null && redlineStart < maxValue) {
                val redlineFraction = ((redlineStart - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                val redlineAngle = startAngle + (redlineFraction * sweepAngle)
                val redlineSweep = sweepAngle * (1f - redlineFraction)

                drawArc(
                    color = RaceRed.copy(alpha = 0.85f),
                    startAngle = redlineAngle,
                    sweepAngle = redlineSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius + 12f, center.y - radius + 12f),
                    size = androidx.compose.ui.geometry.Size((radius - 12f) * 2f, (radius - 12f) * 2f),
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )
            }

            // 3. Calibration Ticks
            val totalMajorTicks = ((maxValue - minValue) / majorTickStep).toInt()
            val totalTicks = totalMajorTicks * (minorTicksPerMajor + 1)

            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val angle = Math.toRadians((startAngle + (tickFraction * sweepAngle)).toDouble())
                val isMajor = (i % (minorTicksPerMajor + 1)) == 0

                val tickLength = if (isMajor) 16f else 8f
                val strokeWidth = if (isMajor) 3.5f else 1.5f

                val isRedlineTick = redlineStart != null && (minValue + tickFraction * (maxValue - minValue)) >= redlineStart
                val tickColor = if (isRedlineTick) RaceRed else if (isMajor) Color.White else Color(0xFF8A99AC)

                val innerR = radius - 18f - tickLength
                val outerR = radius - 18f

                val p1 = Offset(
                    (center.x + innerR * cos(angle)).toFloat(),
                    (center.y + innerR * sin(angle)).toFloat()
                )
                val p2 = Offset(
                    (center.x + outerR * cos(angle)).toFloat(),
                    (center.y + outerR * sin(angle)).toFloat()
                )

                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 4. The Iconic Mazda MX-5 Amber Needle
            rotate(needleAngle - 90f, pivot = center) {
                // Needle shadow for 3D depth
                drawLine(
                    color = Color.Black.copy(alpha = 0.45f),
                    start = Offset(center.x + 3f, center.y + 3f),
                    end = Offset(center.x + 3f, center.y - radius + 25f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )

                // Needle glowing core
                drawLine(
                    color = if (isWarning) RaceRed else Mx5Amber,
                    start = Offset(center.x, center.y + 18f), // counter-weight tail
                    end = Offset(center.x, center.y - radius + 22f),
                    strokeWidth = 5.5f,
                    cap = StrokeCap.Round
                )

                // Needle bright tip highlight
                drawLine(
                    color = Color.White,
                    start = Offset(center.x, center.y - radius + 32f),
                    end = Offset(center.x, center.y - radius + 22f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            // 5. Center Hub / Chrome Boss (Classic NB instrument cluster style)
            drawCircle(
                color = Color(0xFF1E232E),
                radius = 18f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF808D9E), Color(0xFF333D4B)),
                    center = center,
                    radius = 12f
                ),
                radius = 12f,
                center = center
            )
        }

        // Digital Value & Title Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (maxValue >= 1000) String.format("%.0f", animatedValue) else String.format("%.1f", animatedValue),
                    color = if (isWarning) RaceRed else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "$title ($unit)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
