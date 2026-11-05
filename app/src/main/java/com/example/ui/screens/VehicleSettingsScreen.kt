package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Mx5EngineType
import com.example.model.Mx5Transmission
import com.example.model.VehicleConfig
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen
import com.example.viewmodel.TelemetryViewModel

@Composable
fun VehicleSettingsScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.vehicleConfig.collectAsState()

    var shiftRpm by remember(config.shiftLightRpm) { mutableStateOf(config.shiftLightRpm.toFloat()) }
    var coolantAlarm by remember(config.coolantWarningTempC) { mutableStateOf(config.coolantWarningTempC) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Header: Mazda MX-5 NBFL Profile
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Mx5Amber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Mx5Amber, modifier = Modifier.size(30.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "PROFILO VEICOLO", color = Color(0xFF8A99AC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = config.modelName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(
                        text = "${config.engineType.displayName} • ${config.transmission.displayName}",
                        color = Mx5Amber,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 1. Engine Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "MOTORIZZAZIONE MAZDA NBFL", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Mx5EngineType.values().forEach { engine ->
                    val isSelected = config.engineType == engine
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Mx5Amber.copy(alpha = 0.2f) else CockpitSurfaceVariant)
                            .clickable {
                                viewModel.updateVehicleConfig(
                                    config.copy(
                                        engineType = engine,
                                        redlineRpm = engine.defaultRedline,
                                        shiftLightRpm = engine.defaultRedline - 400
                                    )
                                )
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = engine.displayName,
                                color = if (isSelected) Mx5Amber else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${engine.stockHp} CV • Fuorigiri: ${engine.defaultRedline} RPM",
                                color = Color(0xFF8A99AC),
                                fontSize = 11.sp
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Mx5Amber)
                        }
                    }
                }
            }
        }

        // 2. Transmission & Final Drive Differential
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "TRASMISSIONE E DIFFERENZIALE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mx5Transmission.values().forEach { trans ->
                        val isSelected = config.transmission == trans
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Mx5Amber else CockpitSurfaceVariant)
                                .clickable { viewModel.updateVehicleConfig(config.copy(transmission = trans)) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (trans == Mx5Transmission.SIX_SPEED) "6 Marce (Sport)" else "5 Marce",
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Rapporto Differenziale Finale (Conica Torsen):",
                    color = Color(0xFF8A99AC),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                val diffOptions = listOf(
                    Pair("3.909 (Stock NBFL 1.8 6M)", 3.909f),
                    Pair("3.636 (NBFL 6M Lungo)", 3.636f),
                    Pair("4.100 (NB 5M Standard)", 4.100f),
                    Pair("4.300 (Corto Pista)", 4.300f)
                )

                diffOptions.forEach { (label, ratio) ->
                    val isSelected = config.finalDriveRatio == ratio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Mx5Amber.copy(alpha = 0.2f) else CockpitSurfaceVariant)
                            .clickable { viewModel.updateVehicleConfig(config.copy(finalDriveRatio = ratio)) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, color = if (isSelected) Mx5Amber else Color.White, fontSize = 12.sp)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Mx5Amber, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 3. Shift Light & Alarms Sliders
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "SOGLIE DI ALLARME E SHIFT LIGHT", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                // Shift Light Threshold
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Luce di Cambiata (Shift Light)", color = Color(0xFF8A99AC), fontSize = 12.sp)
                    Text(
                        text = "${shiftRpm.toInt()} RPM",
                        color = Mx5Amber,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = shiftRpm,
                    onValueChange = {
                        shiftRpm = it
                        viewModel.updateVehicleConfig(config.copy(shiftLightRpm = it.toInt()))
                    },
                    valueRange = 5500f..7500f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = Mx5Amber, activeTrackColor = Mx5Amber)
                )

                // Coolant Warning Temp
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Allarme Sovratemperatura Liquido (ECT)", color = Color(0xFF8A99AC), fontSize = 12.sp)
                    Text(
                        text = "${coolantAlarm.toInt()} °C",
                        color = RaceRed,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = coolantAlarm,
                    onValueChange = {
                        coolantAlarm = it
                        viewModel.updateVehicleConfig(config.copy(coolantWarningTempC = it))
                    },
                    valueRange = 98f..115f,
                    steps = 16,
                    colors = SliderDefaults.colors(thumbColor = RaceRed, activeTrackColor = RaceRed)
                )
            }
        }

        // Reset Sensor Peaks
        OutlinedButton(
            onClick = { viewModel.resetGForcePeaks() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Mx5Amber)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Azzera Picchi G-Meter e Telemetria")
        }
    }
}
