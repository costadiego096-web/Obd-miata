package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.nativeCanvas
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
 * Features:
 * - Real radial background dial numbers (e.g. 0 to 8 for RPM, 0 to 240 for Speed)
 * - Calibrated 270-degree sweep from 135° to 405°
 * - Precision needle that accurately tracks and points directly to the real scale numbers
 * - Mazda amber glowing needle with counterweight and center chrome boss
 * - Redline and warning zone color arc
 */
@Composable
fun AnalogGauge(
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 8000f,
    title: String = "RPM",
    unit: String = "x1000",
    redlineStart: Float? = null,
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

    // Standard automotive 270° sweep starting at 135° (bottom-left) to 405° (bottom-right)
    val startAngle = 135f
    val sweepAngle = 270f

    val fraction = ((animatedValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    val needleAngle = startAngle + (fraction * sweepAngle)

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f) - 8f

            // 1. Dial Bezel & Background Face (Classic NBFL dark cockpit)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CockpitSurface, Color(0xFF0A0D12)),
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
                style = Stroke(width = 3.5f)
            )

            // 2. Redline Zone Arc
            if (redlineStart != null && redlineStart < maxValue) {
                val redlineFraction = ((redlineStart - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                val redlineAngle = startAngle + (redlineFraction * sweepAngle)
                val redlineSweep = sweepAngle * (1f - redlineFraction)

                drawArc(
                    color = RaceRed.copy(alpha = 0.85f),
                    startAngle = redlineAngle,
                    sweepAngle = redlineSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius + 10f, center.y - radius + 10f),
                    size = androidx.compose.ui.geometry.Size((radius - 10f) * 2f, (radius - 10f) * 2f),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            // 3. Calibration Ticks and Dial Background Numbers
            val totalMajorTicks = ((maxValue - minValue) / majorTickStep).toInt().coerceAtLeast(1)
            val totalTicks = totalMajorTicks * (minorTicksPerMajor + 1)

            val paint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = if (radius > 110f) (radius * 0.155f) else (radius * 0.17f)
            }

            val isRpmDial = (maxValue >= 1000f) && (title.contains("RPM", ignoreCase = true) || title.contains("Giri", ignoreCase = true))

            for (i in 0..totalTicks) {
                val tickFraction = i.toFloat() / totalTicks
                val angleDeg = startAngle + (tickFraction * sweepAngle)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = (i % (minorTicksPerMajor + 1)) == 0

                val currentTickVal = minValue + tickFraction * (maxValue - minValue)
                val isRedlineTick = redlineStart != null && currentTickVal >= redlineStart

                val tickLength = if (isMajor) (radius * 0.13f) else (radius * 0.07f)
                val strokeWidth = if (isMajor) 3f else 1.2f

                val tickColor = if (isRedlineTick) RaceRed else if (isMajor) Color.White else Color(0xFF7F8E9F)

                val outerR = radius - (radius * 0.07f)
                val innerR = outerR - tickLength

                val cosA = cos(angleRad).toFloat()
                val sinA = sin(angleRad).toFloat()

                val p1 = Offset(center.x + innerR * cosA, center.y + innerR * sinA)
                val p2 = Offset(center.x + outerR * cosA, center.y + outerR * sinA)

                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Draw Dial Numbers on Background on Major Ticks
                if (isMajor) {
                    val numberText = when {
                        isRpmDial -> "${(currentTickVal / 1000f).toInt()}"
                        maxValue >= 100f -> String.format("%.0f", currentTickVal)
                        maxValue >= 10f -> String.format("%.0f", currentTickVal)
                        else -> String.format("%.1f", currentTickVal)
                    }

                    paint.color = if (isRedlineTick) {
                        android.graphics.Color.parseColor("#FF2A3C")
                    } else {
                        android.graphics.Color.parseColor("#E8EDF2")
                    }

                    // Place numbers inside the ticks
                    val textRadius = innerR - (radius * 0.16f)
                    val textX = center.x + textRadius * cosA
                    val textY = center.y + textRadius * sinA

                    // Vertical centering offset
                    val fontMetrics = paint.fontMetrics
                    val yOffset = -(fontMetrics.ascent + fontMetrics.descent) / 2f

                    drawContext.canvas.nativeCanvas.drawText(
                        numberText,
                        textX,
                        textY + yOffset,
                        paint
                    )
                }
            }

            // 4. Center Unit Legend (e.g. "x1000 r/min" or "km/h")
            val legendPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = radius * 0.10f
                color = android.graphics.Color.parseColor("#8A99AC")
            }
            val legendY = center.y - (radius * 0.38f)
            drawContext.canvas.nativeCanvas.drawText(
                if (isRpmDial) "x1000r/min" else unit,
                center.x,
                legendY,
                legendPaint
            )

            // 5. Iconic Mazda Amber Needle - Mathematically Calibrated
            // Base needle is defined along the X-axis (from (center.x - tail, center.y) to (center.x + needleLength, center.y)).
            // Rotating by `needleAngle` makes it point precisely and strictly along `needleAngle` degrees!
            val needleLength = radius - (radius * 0.10f)
            val tailLength = radius * 0.18f

            rotate(needleAngle, pivot = center) {
                // Needle shadow for realistic 3D depth
                drawLine(
                    color = Color.Black.copy(alpha = 0.45f),
                    start = Offset(center.x - tailLength, center.y + 2.5f),
                    end = Offset(center.x + needleLength - 4f, center.y + 2.5f),
                    strokeWidth = 5.5f,
                    cap = StrokeCap.Round
                )

                // Needle glowing core (Mazda iconic Amber / Red warning)
                drawLine(
                    color = if (isWarning) RaceRed else Mx5Amber,
                    start = Offset(center.x - tailLength, center.y),
                    end = Offset(center.x + needleLength, center.y),
                    strokeWidth = 4.5f,
                    cap = StrokeCap.Round
                )

                // White bright needle tip highlight
                drawLine(
                    color = Color.White,
                    start = Offset(center.x + needleLength - (radius * 0.22f), center.y),
                    end = Offset(center.x + needleLength, center.y),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // 6. Center Hub / Chrome Boss (Classic NBFL instrument cluster cap)
            drawCircle(
                color = Color(0xFF181C24),
                radius = radius * 0.17f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8C9AA9), Color(0xFF2C3440)),
                    center = center,
                    radius = radius * 0.12f
                ),
                radius = radius * 0.12f,
                center = center
            )
            drawCircle(
                color = Mx5Amber.copy(alpha = 0.8f),
                radius = 3.5f,
                center = center
            )
        }

        // Digital Numeric Readout & Title at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (maxValue >= 1000f) {
                        String.format("%.0f", animatedValue)
                    } else if (maxValue >= 100f) {
                        String.format("%.0f", animatedValue)
                    } else {
                        String.format("%.1f", animatedValue)
                    },
                    color = if (isWarning) RaceRed else Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
