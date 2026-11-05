package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LiveGForce
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen
import kotlin.math.abs

/**
 * 2D Friction Circle (G-G Diagram) for analyzing cornering and braking forces on track.
 */
@Composable
fun FrictionCircle(
    gForce: LiveGForce,
    maxScaleG: Float = 1.5f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CockpitSurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "G-METER TELEMETRIA",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.2f", gForce.totalG)} G",
                    color = Mx5Amber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = (size.width / 2f) - 16f

                    // Concentric limit circles: 0.5G, 1.0G, 1.5G
                    val gSteps = listOf(0.5f, 1.0f, 1.5f)
                    gSteps.forEach { stepG ->
                        val r = maxRadius * (stepG / maxScaleG)
                        drawCircle(
                            color = if (stepG == 1.0f) CockpitOutline.copy(alpha = 0.9f) else CockpitOutline.copy(alpha = 0.45f),
                            radius = r,
                            center = center,
                            style = Stroke(width = if (stepG == 1.0f) 2f else 1f)
                        )
                    }

                    // Crosshair axes (Lateral X, Longitudinal Y)
                    drawLine(
                        color = CockpitOutline,
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = CockpitOutline,
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.5f
                    )

                    // Target Dot (Instantaneous G-Force)
                    // Screen coordinates:
                    // Lateral: +Right (positive X), -Left (negative X)
                    // Longitudinal: +Braking is upward (-Y), +Accel is downward (+Y)
                    val dotX = center.x + (gForce.latG / maxScaleG) * maxRadius
                    val dotY = center.y - (gForce.longG / maxScaleG) * maxRadius

                    val clampedX = dotX.coerceIn(center.x - maxRadius, center.x + maxRadius)
                    val clampedY = dotY.coerceIn(center.y - maxRadius, center.y + maxRadius)

                    // Glow circle
                    drawCircle(
                        color = if (gForce.totalG > 1.0f) RaceRed.copy(alpha = 0.35f) else Mx5Amber.copy(alpha = 0.35f),
                        radius = 16f,
                        center = Offset(clampedX, clampedY)
                    )

                    // Core dot
                    drawCircle(
                        color = if (gForce.totalG > 1.0f) RaceRed else Mx5Amber,
                        radius = 7f,
                        center = Offset(clampedX, clampedY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(clampedX, clampedY)
                    )
                }

                // Axis directional labels
                Text(
                    text = "FRENATA (-G)",
                    color = Color(0xFF8A99AC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                    text = "ACCEL (+G)",
                    color = Color(0xFF8A99AC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                Text(
                    text = "SX",
                    color = Color(0xFF8A99AC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "DX",
                    color = Color(0xFF8A99AC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // Peak G readings summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                GStatItem(label = "Freno Max", value = "${String.format("%.2f", abs(gForce.peakBrakingG))}G", color = RaceRed)
                GStatItem(label = "Lat. SX", value = "${String.format("%.2f", abs(gForce.peakLeftG))}G", color = TrackGreen)
                GStatItem(label = "Lat. DX", value = "${String.format("%.2f", abs(gForce.peakRightG))}G", color = TrackGreen)
                GStatItem(label = "Accel Max", value = "${String.format("%.2f", abs(gForce.peakAccelG))}G", color = Mx5Amber)
            }
        }
    }
}

@Composable
private fun GStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF8A99AC), fontSize = 10.sp)
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
