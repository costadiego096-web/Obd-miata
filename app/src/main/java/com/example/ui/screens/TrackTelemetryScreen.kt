package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrackPresets
import com.example.ui.components.FrictionCircle
import com.example.ui.components.ShiftLightBar
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen
import com.example.viewmodel.TelemetryViewModel

@Composable
fun TrackTelemetryScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier
) {
    val lapState by viewModel.lapState.collectAsState()
    val obdData by viewModel.obdManager.obdData.collectAsState()
    val gForce by viewModel.combinedGForce.collectAsState()
    val config by viewModel.vehicleConfig.collectAsState()

    var showCircuitMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(600), repeatMode = RepeatMode.Reverse),
        label = "rec_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circuit Selector & Recording Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circuit Dropdown Selector
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CockpitSurfaceVariant)
                        .clickable { showCircuitMenu = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = Mx5Amber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lapState.selectedCircuit.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = showCircuitMenu,
                    onDismissRequest = { showCircuitMenu = false },
                    modifier = Modifier.background(CockpitSurface)
                ) {
                    TrackPresets.CIRCUITS.forEach { circuit ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(text = circuit.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = circuit.location, color = Color(0xFF8A99AC), fontSize = 11.sp)
                                }
                            },
                            onClick = {
                                viewModel.setCircuit(circuit)
                                showCircuitMenu = false
                            }
                        )
                    }
                }
            }

            // REC status pill
            if (lapState.isRecording) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(RaceRed.copy(alpha = 0.25f))
                        .border(1.dp, RaceRed, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RaceRed.copy(alpha = recAlpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REC TELEMETRIA",
                        color = RaceRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Shift Light Bar
        ShiftLightBar(
            currentRpm = obdData.rpm,
            shiftLightRpm = config.shiftLightRpm.toFloat(),
            redlineRpm = config.redlineRpm.toFloat(),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Main Lap Timing Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GIRO ${lapState.currentLapNumber}",
                        color = Mx5Amber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Delta to best
                    lapState.deltaToBestMs?.let { delta ->
                        val isFaster = delta <= 0
                        val deltaSign = if (isFaster) "-" else "+"
                        val deltaSeconds = kotlin.math.abs(delta) / 1000f
                        Text(
                            text = "$deltaSign${String.format("%.2f", deltaSeconds)}s",
                            color = if (isFaster) TrackGreen else RaceRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Huge Current Lap Timer
                Text(
                    text = formatLapTime(lapState.currentLapTimeMs),
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Last Lap & Best Lap Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CockpitSurfaceVariant)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "ULTIMO GIRO", color = Color(0xFF8A99AC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = lapState.lastLapTimeMs?.let { formatLapTime(it) } ?: "--:--.--",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "MIGLIOR GIRO (BEST)", color = TrackGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = lapState.bestLapTimeMs?.let { formatLapTime(it) } ?: "--:--.--",
                            color = TrackGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Action Buttons: Start/Stop Recording and Manual Lap Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.toggleSessionRecording() },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("record_session_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (lapState.isRecording) RaceRed else TrackGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (lapState.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (lapState.isRecording) "TERMINA SESSIONE" else "AVVIA SESSIONE",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            if (lapState.isRecording) {
                Button(
                    onClick = { viewModel.triggerNewLap() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("trigger_lap_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Mx5Amber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SportsScore, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TAGLIA TRAGUARDO", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Live G-Meter & Friction Circle
        FrictionCircle(
            gForce = gForce,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )

        // Real-time Vehicle OBD telemetry strip
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickMetric("VELOCITÀ", "${String.format("%.0f", obdData.speedKmh)} km/h", Color.White)
                QuickMetric("RPM", String.format("%.0f", obdData.rpm), if (obdData.isShiftLightActive) RaceRed else Mx5Amber)
                QuickMetric("MARCIA", obdData.calculatedGear, TrackGreen)
                QuickMetric("TEMP. ACQUA", "${String.format("%.0f", obdData.coolantTempC)}°C", if (obdData.isCoolantWarningActive) RaceRed else Color.White)
            }
        }
    }
}

@Composable
private fun QuickMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF8A99AC), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

fun formatLapTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (millis % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
}
