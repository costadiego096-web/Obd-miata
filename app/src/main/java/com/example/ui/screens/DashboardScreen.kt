package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DashboardLayoutType
import com.example.model.DashboardWidgetId
import com.example.model.LiveGForce
import com.example.model.ObdData
import com.example.model.VehicleConfig
import com.example.ui.components.AnalogGauge
import com.example.ui.components.FrictionCircle
import com.example.ui.components.HorizontalBarGauge
import com.example.ui.components.ShiftLightBar
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.ShiftCyan
import com.example.ui.theme.TrackGreen
import com.example.ui.theme.TurboBlue
import com.example.ui.theme.WarningYellow
import com.example.viewmodel.TelemetryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier
) {
    val obdData by viewModel.obdManager.obdData.collectAsState()
    val gForce by viewModel.combinedGForce.collectAsState()
    val config by viewModel.vehicleConfig.collectAsState()
    val layoutType by viewModel.dashboardLayout.collectAsState()
    val gridWidgets by viewModel.gridWidgets.collectAsState()
    val connState by viewModel.obdManager.connectionState.collectAsState()
    val connType by viewModel.obdManager.activeConnectionType.collectAsState()

    var showWidgetPickerForSlot by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Top Bar: Connection Badge, MX-5 NBFL Model tag, Layout Switcher
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CockpitSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (connState) {
                                com.example.model.ObdConnectionState.CONNECTED -> TrackGreen
                                com.example.model.ObdConnectionState.CONNECTING,
                                com.example.model.ObdConnectionState.INITIALIZING -> WarningYellow
                                else -> RaceRed
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${connType.name}: ${connState.statusLabel}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // VVT Active Badge on NBFL
            if (obdData.isVvtActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Mx5Amber)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "S-VT ATTIVO",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Layout selector chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DashboardLayoutType.values().forEach { type ->
                    val isSelected = layoutType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Mx5Amber else CockpitSurfaceVariant)
                            .clickable { viewModel.setDashboardLayout(type) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (type) {
                                DashboardLayoutType.CLASSIC_NBFL -> "NBFL"
                                DashboardLayoutType.RACE_COCKPIT -> "Pista"
                                DashboardLayoutType.MODULAR_GRID -> "Griglia"
                                DashboardLayoutType.HUD_MIRROR -> "HUD"
                            },
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Warning alerts banner (Coolant Overheat or Shift Light)
        if (obdData.isCoolantWarningActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RaceRed)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ALLARME TEMPERATURA LIQUIDO: ${String.format("%.1f", obdData.coolantTempC)}°C",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Layout Rendering
        when (layoutType) {
            DashboardLayoutType.CLASSIC_NBFL -> {
                ClassicNbflDashboard(obdData = obdData, config = config)
            }
            DashboardLayoutType.RACE_COCKPIT -> {
                RaceCockpitDashboard(obdData = obdData, gForce = gForce, config = config)
            }
            DashboardLayoutType.MODULAR_GRID -> {
                ModularGridDashboard(
                    obdData = obdData,
                    gForce = gForce,
                    widgets = gridWidgets,
                    onWidgetClick = { slot -> showWidgetPickerForSlot = slot }
                )
            }
            DashboardLayoutType.HUD_MIRROR -> {
                HudMirrorDashboard(obdData = obdData)
            }
        }
    }

    // Widget Slot Customization Dialog
    showWidgetPickerForSlot?.let { slotIndex ->
        AlertDialog(
            onDismissRequest = { showWidgetPickerForSlot = null },
            title = { Text("Personalizza Slot #${slotIndex + 1}", color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Seleziona il parametro da visualizzare in questo quadrante:",
                        color = Color(0xFFB0BCCB),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    DashboardWidgetId.values().forEach { widget ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setGridWidget(slotIndex, widget)
                                    showWidgetPickerForSlot = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${widget.title} (${widget.unit})", color = Color.White, fontSize = 14.sp)
                            if (gridWidgets.getOrNull(slotIndex) == widget) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Mx5Amber)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWidgetPickerForSlot = null }) {
                    Text("Chiudi", color = Mx5Amber)
                }
            },
            containerColor = CockpitSurface
        )
    }
}

/**
 * Authentic Mazda MX-5 NBFL Instrument Cluster Layout
 */
@Composable
private fun ClassicNbflDashboard(obdData: ObdData, config: VehicleConfig) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sequential Shift Light Bar
        ShiftLightBar(
            currentRpm = obdData.rpm,
            shiftLightRpm = config.shiftLightRpm.toFloat(),
            redlineRpm = config.redlineRpm.toFloat(),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Main Dual Instrument Gauges: Tachometer (RPM) & Speedometer (km/h)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Tachometer with NBFL 7200 redline
            AnalogGauge(
                value = obdData.rpm,
                minValue = 0f,
                maxValue = 8000f,
                title = "Giri Motore",
                unit = "RPM",
                redlineStart = config.redlineRpm.toFloat(),
                isWarning = obdData.isShiftLightActive,
                majorTickStep = 1000f,
                modifier = Modifier.weight(1f)
            )

            // Speedometer
            AnalogGauge(
                value = obdData.speedKmh,
                minValue = 0f,
                maxValue = 240f,
                title = "Velocità",
                unit = "km/h",
                redlineStart = 220f,
                majorTickStep = 20f,
                minorTicksPerMajor = 1,
                modifier = Modifier.weight(1f)
            )
        }

        // Center Gear & Power Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gear indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CockpitSurfaceVariant)
                    .border(2.dp, Mx5Amber, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MARCIA", color = Color(0xFF8A99AC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = obdData.calculatedGear,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Estimated HP & Torque
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CockpitSurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "POTENZA: ${String.format("%.0f", obdData.calculatedHp)} CV",
                    color = Mx5Amber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "COPPIA: ${String.format("%.0f", obdData.calculatedTorqueNm)} Nm",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Ancillary Gauges Row (NBFL Auxiliary Cluster)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PARAMETRI SECONDARI MOTORE",
                    color = Color(0xFF8A99AC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                HorizontalBarGauge(
                    label = "Temp. Liquido Raffreddamento (ECT)",
                    value = obdData.coolantTempC,
                    unit = "°C",
                    minValue = 40f,
                    maxValue = 120f,
                    warningThreshold = 100f,
                    dangerThreshold = config.coolantWarningTempC
                )

                HorizontalBarGauge(
                    label = "Apertura Farfalla (TPS)",
                    value = obdData.throttlePosPct,
                    unit = "%",
                    minValue = 0f,
                    maxValue = 100f
                )

                HorizontalBarGauge(
                    label = "Anticipo Accensione (Timing)",
                    value = obdData.timingAdvanceDeg,
                    unit = "° BTDC",
                    minValue = 0f,
                    maxValue = 45f
                )

                HorizontalBarGauge(
                    label = "Tensione Batteria / Alternatore",
                    value = obdData.batteryVoltage,
                    unit = "V",
                    minValue = 10f,
                    maxValue = 16f,
                    warningThreshold = 11.8f
                )
            }
        }
    }
}

/**
 * Race Cockpit Layout: High contrast, big digital gear, RPM arc, and live G-Force
 */
@Composable
private fun RaceCockpitDashboard(obdData: ObdData, gForce: LiveGForce, config: VehicleConfig) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShiftLightBar(
            currentRpm = obdData.rpm,
            shiftLightRpm = config.shiftLightRpm.toFloat(),
            redlineRpm = config.redlineRpm.toFloat(),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Massive Center Speed & Gear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CockpitSurface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.0f", obdData.speedKmh),
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "KM/H", color = Mx5Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Gear Box
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CockpitSurfaceVariant)
                    .border(3.dp, if (obdData.isShiftLightActive) RaceRed else Mx5Amber, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = obdData.calculatedGear,
                    color = if (obdData.isShiftLightActive) RaceRed else Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            // RPM
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.0f", obdData.rpm),
                    color = if (obdData.isShiftLightActive) RaceRed else Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "GIRI/MIN", color = Color(0xFF8A99AC), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live G-Meter & Throttle/Brake Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrictionCircle(
                gForce = gForce,
                modifier = Modifier.weight(1.2f)
            )

            // Right side telemetry telemetry card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "LIVE TELEMETRIA", color = Color(0xFF8A99AC), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    TelemetryMetricItem("TPS Farfalla", "${String.format("%.0f", obdData.throttlePosPct)} %", Mx5Amber)
                    TelemetryMetricItem("Temp. Acqua", "${String.format("%.1f", obdData.coolantTempC)} °C", if (obdData.isCoolantWarningActive) RaceRed else Color.White)
                    TelemetryMetricItem("Aspirazione", "${String.format("%.1f", obdData.intakeTempC)} °C", TurboBlue)
                    TelemetryMetricItem("Anticipo", "${String.format("%.1f", obdData.timingAdvanceDeg)} °", Color.White)
                    TelemetryMetricItem("Flusso MAF", "${String.format("%.1f", obdData.mafFlowRateGps)} g/s", ShiftCyan)
                }
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF8A99AC), fontSize = 11.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Modular Grid Layout: 6 customizable tiles where user can tap and pick any parameter
 */
@Composable
private fun ModularGridDashboard(
    obdData: ObdData,
    gForce: LiveGForce,
    widgets: List<DashboardWidgetId>,
    onWidgetClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Tocca un quadrante per cambiare il parametro visualizzato",
            color = Color(0xFF8A99AC),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(widgets) { index, widget ->
                ModularWidgetCard(
                    slotIndex = index,
                    widget = widget,
                    obdData = obdData,
                    gForce = gForce,
                    onClick = { onWidgetClick(index) }
                )
            }
        }
    }
}

@Composable
private fun ModularWidgetCard(
    slotIndex: Int,
    widget: DashboardWidgetId,
    obdData: ObdData,
    gForce: LiveGForce,
    onClick: () -> Unit
) {
    val (valueStr, numValue, maxVal) = when (widget) {
        DashboardWidgetId.RPM -> Triple(String.format("%.0f", obdData.rpm), obdData.rpm, 8000f)
        DashboardWidgetId.SPEED -> Triple(String.format("%.0f", obdData.speedKmh), obdData.speedKmh, 240f)
        DashboardWidgetId.COOLANT -> Triple(String.format("%.1f", obdData.coolantTempC), obdData.coolantTempC, 120f)
        DashboardWidgetId.INTAKE_TEMP -> Triple(String.format("%.1f", obdData.intakeTempC), obdData.intakeTempC, 80f)
        DashboardWidgetId.THROTTLE -> Triple(String.format("%.0f", obdData.throttlePosPct), obdData.throttlePosPct, 100f)
        DashboardWidgetId.TIMING_ADVANCE -> Triple(String.format("%.1f", obdData.timingAdvanceDeg), obdData.timingAdvanceDeg, 45f)
        DashboardWidgetId.MAF -> Triple(String.format("%.1f", obdData.mafFlowRateGps), obdData.mafFlowRateGps, 150f)
        DashboardWidgetId.MANIFOLD_PRESSURE -> Triple(String.format("%.2f", obdData.manifoldPressureKpa / 100f), obdData.manifoldPressureKpa / 100f, 1.5f)
        DashboardWidgetId.BATTERY -> Triple(String.format("%.2f", obdData.batteryVoltage), obdData.batteryVoltage, 16f)
        DashboardWidgetId.FUEL_TRIM -> Triple(String.format("%.1f", obdData.fuelTrimShortPct), obdData.fuelTrimShortPct, 25f)
        DashboardWidgetId.ENGINE_LOAD -> Triple(String.format("%.0f", obdData.engineLoadPct), obdData.engineLoadPct, 100f)
        DashboardWidgetId.POWER_HP -> Triple(String.format("%.0f", obdData.calculatedHp), obdData.calculatedHp, 200f)
        DashboardWidgetId.TORQUE_NM -> Triple(String.format("%.0f", obdData.calculatedTorqueNm), obdData.calculatedTorqueNm, 250f)
        DashboardWidgetId.G_FORCE -> Triple(String.format("%.2f", gForce.totalG), gForce.totalG, 1.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
            .testTag("widget_card_$slotIndex"),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = widget.title,
                    color = Color(0xFFB0BCCB),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "#${slotIndex + 1}",
                    color = CockpitOutline,
                    fontSize = 10.sp
                )
            }

            Column {
                Text(
                    text = valueStr,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = widget.unit,
                    color = Mx5Amber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Mini bar indicator
            val fraction = (numValue / maxVal).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CockpitSurfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Mx5Amber)
                )
            }
        }
    }
}

/**
 * Heads-Up Display (HUD) Mode: Inverted reflection mode for Mazda MX-5 windshield
 */
@Composable
private fun HudMirrorDashboard(obdData: ObdData) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .scale(scaleX = -1f, scaleY = 1f), // Mirror horizontally for windshield projection!
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HUD SPECCHIATO PER PARABREZZA",
                color = Color.DarkGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = String.format("%.0f", obdData.speedKmh),
                color = TrackGreen,
                fontSize = 90.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "KM/H",
                color = TrackGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MARCIA: ${obdData.calculatedGear}",
                color = Mx5Amber,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "${String.format("%.0f", obdData.rpm)} RPM",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
