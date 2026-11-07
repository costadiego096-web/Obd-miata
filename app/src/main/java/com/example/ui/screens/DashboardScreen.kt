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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomWidgetSlot
import com.example.model.DashboardLayoutType
import com.example.model.DashboardWidgetId
import com.example.model.LiveGForce
import com.example.model.ObdData
import com.example.model.VehicleConfig
import com.example.model.WidgetDisplayStyle
import com.example.ui.components.AfrGauge
import com.example.ui.components.AnalogGauge
import com.example.ui.components.FrictionCircle
import com.example.ui.components.HorizontalBarGauge
import com.example.ui.components.IatGauge
import com.example.ui.components.ShiftLightBar
import com.example.ui.components.SparklineGauge
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
    val customSlots by viewModel.customSlots.collectAsState()
    val connState by viewModel.obdManager.connectionState.collectAsState()
    val connType by viewModel.obdManager.activeConnectionType.collectAsState()

    val afrHistory by viewModel.afrHistory.collectAsState()
    val iatHistory by viewModel.iatHistory.collectAsState()
    val rpmHistory by viewModel.rpmHistory.collectAsState()

    var showLayoutCustomizerSheet by remember { mutableStateOf(false) }
    var slotBeingEdited by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Top Bar: Connection, VVT badge, Layout selector, Customize Button
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
                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // S-VT Active Badge on NBFL
            if (obdData.isVvtActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Mx5Amber)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "S-VT", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }

            // Layout customizer button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Mx5Amber.copy(alpha = 0.2f))
                    .clickable { showLayoutCustomizerSheet = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = Mx5Amber, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "LAYOUT", color = Mx5Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Layout switcher horizontal tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DashboardLayoutType.values().forEach { type ->
                val isSelected = layoutType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Mx5Amber else CockpitSurfaceVariant)
                        .clickable { viewModel.setDashboardLayout(type) }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (type) {
                            DashboardLayoutType.CLASSIC_NBFL -> "NBFL"
                            DashboardLayoutType.RACE_COCKPIT -> "Pista"
                            DashboardLayoutType.AIR_FUEL_FOCUS -> "AFR / IAT"
                            DashboardLayoutType.MODULAR_GRID -> "Griglia"
                            DashboardLayoutType.HUD_MIRROR -> "HUD"
                        },
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }

        // Alarms banner (Coolant Overheat or IAT Heat-Soak)
        if (obdData.isCoolantWarningActive || obdData.isIatHeatSoakWarning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RaceRed)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (obdData.isCoolantWarningActive) {
                        "ALLARME TEMPERATURA ACQUA: ${String.format("%.1f", obdData.coolantTempC)}°C!"
                    } else {
                        "ATTENZIONE HEAT-SOAK IAT: ${String.format("%.1f", obdData.intakeTempC)}°C (Aria Calda in Aspirazione)"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        // Layout Rendering
        when (layoutType) {
            DashboardLayoutType.CLASSIC_NBFL -> {
                ClassicNbflDashboard(obdData = obdData, config = config)
            }
            DashboardLayoutType.AIR_FUEL_FOCUS -> {
                AirFuelFocusDashboard(
                    obdData = obdData,
                    config = config,
                    afrHistory = afrHistory,
                    iatHistory = iatHistory
                )
            }
            DashboardLayoutType.RACE_COCKPIT -> {
                RaceCockpitDashboard(obdData = obdData, gForce = gForce, config = config)
            }
            DashboardLayoutType.MODULAR_GRID -> {
                ModularGridDashboard(
                    obdData = obdData,
                    gForce = gForce,
                    slots = customSlots,
                    afrHistory = afrHistory,
                    iatHistory = iatHistory,
                    rpmHistory = rpmHistory,
                    onEditSlot = { slotIndex -> slotBeingEdited = slotIndex }
                )
            }
            DashboardLayoutType.HUD_MIRROR -> {
                HudMirrorDashboard(obdData = obdData)
            }
        }
    }

    // Modal Bottom Sheet: Layout Customizer & Presets
    if (showLayoutCustomizerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLayoutCustomizerSheet = false },
            containerColor = CockpitSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "PERSONALIZZAZIONE DASHBOARD",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Scegli un assetto preimpostato o personalizza ogni singolo quadrante.",
                    color = Color(0xFF8A99AC),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Layout Presets
                Text(
                    text = "ASSETTI PREIMPOSTATI",
                    color = Mx5Amber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetButton(
                        title = "Carburazione (AFR & IAT)",
                        subtitle = "Focus stechiometrico e temp aria",
                        onClick = {
                            viewModel.applyLayoutPreset("AFR_IAT_FOCUS")
                            showLayoutCustomizerSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButton(
                        title = "Pista Telemetria",
                        subtitle = "G-meter, giri e shift light",
                        onClick = {
                            viewModel.applyLayoutPreset("TRACK_PERFORMANCE")
                            showLayoutCustomizerSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetButton(
                        title = "Cluster NBFL Originale",
                        subtitle = "Doppio quadrante analogico",
                        onClick = {
                            viewModel.applyLayoutPreset("CLASSIC_NBFL")
                            showLayoutCustomizerSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PresetButton(
                        title = "Griglia Modulare",
                        subtitle = "6 slot completamente modificabili",
                        onClick = {
                            viewModel.applyLayoutPreset("FULL_GRID")
                            showLayoutCustomizerSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Slot Manager
                Text(
                    text = "QUADRANTI CONFIGURATI NELLA GRIGLIA (${customSlots.size} QUADRANTI)",
                    color = Mx5Amber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                customSlots.forEachIndexed { index, slot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CockpitSurfaceVariant)
                            .clickable {
                                slotBeingEdited = index
                                showLayoutCustomizerSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${index + 1}",
                                color = Mx5Amber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${slot.widgetId.title} (${slot.widgetId.unit})",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Stile: ${slot.displayStyle.displayName}",
                                    color = Color(0xFF8A99AC),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = Color(0xFF8A99AC), modifier = Modifier.size(18.dp))
                            if (customSlots.size > 2) {
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = { viewModel.removeCustomSlot(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = RaceRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                if (customSlots.size < 8) {
                    Button(
                        onClick = {
                            viewModel.addCustomSlot(DashboardWidgetId.AFR_RATIO, WidgetDisplayStyle.DIGITAL_CARD)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CockpitSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Mx5Amber)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aggiungi Quadrante alla Griglia", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Slot Editing Dialog: Choose metric and visual style
    slotBeingEdited?.let { slotIndex ->
        val currentSlot = customSlots.getOrNull(slotIndex)
        var selectedWidget by remember { mutableStateOf(currentSlot?.widgetId ?: DashboardWidgetId.AFR_RATIO) }
        var selectedStyle by remember { mutableStateOf(currentSlot?.displayStyle ?: WidgetDisplayStyle.DIGITAL_CARD) }

        AlertDialog(
            onDismissRequest = { slotBeingEdited = null },
            title = {
                Text(text = "Configura Quadrante #${slotIndex + 1}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Style Selector
                    Text(text = "Stile di visualizzazione:", color = Mx5Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WidgetDisplayStyle.values().forEach { style ->
                            val isSel = selectedStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) Mx5Amber else CockpitSurfaceVariant)
                                    .clickable { selectedStyle = style }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (style) {
                                        WidgetDisplayStyle.DIGITAL_CARD -> "Digitale"
                                        WidgetDisplayStyle.ANALOG_DIAL -> "Analogico"
                                        WidgetDisplayStyle.ARC_BAR -> "Barra"
                                        WidgetDisplayStyle.SPARKLINE_CHART -> "Grafico"
                                    },
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Metric Selector
                    Text(text = "Parametro OBD2 da mostrare:", color = Mx5Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    DashboardWidgetId.values().forEach { widget ->
                        val isSel = selectedWidget == widget
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) Mx5Amber.copy(alpha = 0.2f) else CockpitSurfaceVariant)
                                .clickable { selectedWidget = widget }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = widget.title,
                                    color = if (isSel) Mx5Amber else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(text = "Unità: ${widget.unit}", color = Color(0xFF8A99AC), fontSize = 10.sp)
                            }
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Mx5Amber, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomSlot(slotIndex, selectedWidget, selectedStyle)
                        slotBeingEdited = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Mx5Amber)
                ) {
                    Text("Salva", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { slotBeingEdited = null }) {
                    Text("Annulla", color = Color(0xFF8A99AC))
                }
            },
            containerColor = CockpitSurface
        )
    }
}

@Composable
private fun PresetButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CockpitSurfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = subtitle, color = Color(0xFF8A99AC), fontSize = 10.sp)
        }
    }
}

/**
 * Dedicated Air-Fuel Ratio (AFR) & Intake Air Temperature (IAT) Focus Dashboard
 */
@Composable
private fun AirFuelFocusDashboard(
    obdData: ObdData,
    config: VehicleConfig,
    afrHistory: List<Float>,
    iatHistory: List<Float>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Shift Light Bar
        ShiftLightBar(
            currentRpm = obdData.rpm,
            shiftLightRpm = config.shiftLightRpm.toFloat(),
            redlineRpm = config.redlineRpm.toFloat()
        )

        // Hero 1: Rapporto Stechiometrico (AFR) su Sonda Lambda Originale NBFL
        AfrGauge(
            afr = obdData.afr,
            lambda = obdData.lambdaRatio,
            o2Voltage = obdData.o2Voltage,
            mixtureStatus = obdData.mixtureStatus
        )

        // Hero 2: Temperatura Aria Aspirata (IAT)
        IatGauge(
            iatTempC = obdData.intakeTempC,
            coolantTempC = obdData.coolantTempC,
            warningThresholdC = config.iatWarningTempC
        )

        // Live Waveform Sparklines Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SparklineGauge(
                title = "Traccia AFR Live",
                unit = ":1",
                currentValue = obdData.afr,
                history = afrHistory,
                lineColor = when (obdData.mixtureStatus) {
                    com.example.model.MixtureStatus.RICH -> TurboBlue
                    com.example.model.MixtureStatus.STOICHIOMETRIC -> TrackGreen
                    com.example.model.MixtureStatus.LEAN -> WarningYellow
                    com.example.model.MixtureStatus.CUTOFF -> RaceRed
                },
                modifier = Modifier.weight(1f)
            )

            SparklineGauge(
                title = "Traccia Temp. IAT",
                unit = "°C",
                currentValue = obdData.intakeTempC,
                history = iatHistory,
                lineColor = if (obdData.isIatHeatSoakWarning) RaceRed else TurboBlue,
                modifier = Modifier.weight(1f)
            )
        }

        // Secondary Motor Metrics Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "CORRELAZIONE MOTORE & CARBURAZIONE", color = Color(0xFF8A99AC), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    QuickTelemetryStat("Giri Motore", "${String.format("%.0f", obdData.rpm)} RPM", if (obdData.isShiftLightActive) RaceRed else Mx5Amber)
                    QuickTelemetryStat("Farfalla TPS", "${String.format("%.0f", obdData.throttlePosPct)} %", Color.White)
                    QuickTelemetryStat("Anticipo", "${String.format("%.1f", obdData.timingAdvanceDeg)} °", ShiftCyan)
                    QuickTelemetryStat("Temp. Acqua", "${String.format("%.0f", obdData.coolantTempC)} °C", if (obdData.isCoolantWarningActive) RaceRed else Color.White)
                }
            }
        }
    }
}

@Composable
private fun QuickTelemetryStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF8A99AC), fontSize = 10.sp)
        Text(
            text = value,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Classic Mazda MX-5 NBFL Cluster Layout with AFR and IAT Integration
 */
@Composable
private fun ClassicNbflDashboard(obdData: ObdData, config: VehicleConfig) {
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

        // Main Dual Gauges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
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

            AnalogGauge(
                value = obdData.speedKmh,
                minValue = 0f,
                maxValue = 240f,
                title = "Velocità",
                unit = "km/h",
                redlineStart = null,
                majorTickStep = 20f,
                minorTicksPerMajor = 1,
                modifier = Modifier.weight(1f)
            )
        }

        // Center Gear & AFR Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gear badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CockpitSurfaceVariant)
                    .border(2.dp, Mx5Amber, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MARCIA", color = Color(0xFF8A99AC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = obdData.calculatedGear,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // AFR & IAT Quick Pill
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CockpitSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "AFR: ", color = Color(0xFF8A99AC), fontSize = 12.sp)
                    Text(
                        text = "${String.format("%.2f", obdData.afr)} : 1",
                        color = when (obdData.mixtureStatus) {
                            com.example.model.MixtureStatus.RICH -> TurboBlue
                            com.example.model.MixtureStatus.STOICHIOMETRIC -> TrackGreen
                            com.example.model.MixtureStatus.LEAN -> WarningYellow
                            com.example.model.MixtureStatus.CUTOFF -> RaceRed
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Temp. Aria: ", color = Color(0xFF8A99AC), fontSize = 12.sp)
                    Text(
                        text = "${String.format("%.1f", obdData.intakeTempC)} °C",
                        color = if (obdData.isIatHeatSoakWarning) RaceRed else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Ancillary Gauges Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "MONITOR SENSORI MOTORE NBFL", color = Color(0xFF8A99AC), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                HorizontalBarGauge(
                    label = "Temp. Aria Aspirata (IAT)",
                    value = obdData.intakeTempC,
                    unit = "°C",
                    minValue = 10f,
                    maxValue = 70f,
                    warningThreshold = 45f,
                    dangerThreshold = config.iatWarningTempC
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
                    label = "Tensione Sonda Lambda (O2)",
                    value = obdData.o2Voltage,
                    unit = "V",
                    minValue = 0f,
                    maxValue = 1f
                )

                // ---- Tensione Batteria (B+) ----
                val battV = obdData.batteryVoltage
                val battColor = when {
                    battV.isNaN() -> Color(0xFF8A99AC)
                    battV < 11.5f -> RaceRed       // batteria scarica / alternatore KO
                    battV < 12.5f -> WarningYellow // a motore spento: sotto carica
                    battV > 15.0f -> RaceRed       // sovraccarica / regolatore rotto
                    battV > 14.8f -> WarningYellow // inizio sovraccarica
                    else -> TrackGreen             // 12.5-14.8: range sano
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tensione Batteria (B+)",
                        color = Color(0xFF8A99AC),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (battV.isNaN()) "--" else String.format(java.util.Locale.US, "%.1f V", battV),
                        color = battColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
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

        // Center Speed & Gear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CockpitSurface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.0f", obdData.speedKmh),
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "KM/H", color = Mx5Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CockpitSurfaceVariant)
                    .border(3.dp, if (obdData.isShiftLightActive) RaceRed else Mx5Amber, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = obdData.calculatedGear,
                    color = if (obdData.isShiftLightActive) RaceRed else Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.0f", obdData.rpm),
                    color = if (obdData.isShiftLightActive) RaceRed else Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "GIRI/MIN", color = Color(0xFF8A99AC), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live G-Meter & Telemetry Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrictionCircle(
                gForce = gForce,
                modifier = Modifier.weight(1.2f)
            )

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "LIVE TELEMETRIA", color = Color(0xFF8A99AC), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    TelemetryMetricItem("Rapporto AFR", "${String.format("%.1f", obdData.afr)}:1", TrackGreen)
                    TelemetryMetricItem("Temp. Aria (IAT)", "${String.format("%.0f", obdData.intakeTempC)} °C", if (obdData.isIatHeatSoakWarning) RaceRed else TurboBlue)
                    TelemetryMetricItem("TPS Farfalla", "${String.format("%.0f", obdData.throttlePosPct)} %", Mx5Amber)
                    TelemetryMetricItem("Temp. Acqua", "${String.format("%.0f", obdData.coolantTempC)} °C", if (obdData.isCoolantWarningActive) RaceRed else Color.White)
                    TelemetryMetricItem("Anticipo", "${String.format("%.1f", obdData.timingAdvanceDeg)} °", ShiftCyan)
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Modular Grid Layout: Supports custom display styles (Digital Card, Analog Gauge, Arc Bar, Sparkline)
 */
@Composable
private fun ModularGridDashboard(
    obdData: ObdData,
    gForce: LiveGForce,
    slots: List<CustomWidgetSlot>,
    afrHistory: List<Float>,
    iatHistory: List<Float>,
    rpmHistory: List<Float>,
    onEditSlot: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tocca qualsiasi quadrante per cambiare parametro o stile",
                color = Color(0xFF8A99AC),
                fontSize = 11.sp
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(slots) { index, slot ->
                ConfigurableWidgetCard(
                    slot = slot,
                    obdData = obdData,
                    gForce = gForce,
                    afrHistory = afrHistory,
                    iatHistory = iatHistory,
                    rpmHistory = rpmHistory,
                    onClick = { onEditSlot(index) }
                )
            }
        }
    }
}

@Composable
private fun ConfigurableWidgetCard(
    slot: CustomWidgetSlot,
    obdData: ObdData,
    gForce: LiveGForce,
    afrHistory: List<Float>,
    iatHistory: List<Float>,
    rpmHistory: List<Float>,
    onClick: () -> Unit
) {
    val widget = slot.widgetId
    val (valueStr, numValue, maxVal) = when (widget) {
        DashboardWidgetId.AFR_RATIO -> Triple(String.format("%.2f", obdData.afr), obdData.afr, 18f)
        DashboardWidgetId.INTAKE_TEMP -> Triple(String.format("%.1f", obdData.intakeTempC), obdData.intakeTempC, 80f)
        DashboardWidgetId.LAMBDA_O2 -> Triple(String.format("%.3f", obdData.o2Voltage), obdData.o2Voltage, 1f)
        DashboardWidgetId.RPM -> Triple(String.format("%.0f", obdData.rpm), obdData.rpm, 8000f)
        DashboardWidgetId.SPEED -> Triple(String.format("%.0f", obdData.speedKmh), obdData.speedKmh, 240f)
        DashboardWidgetId.COOLANT -> Triple(String.format("%.1f", obdData.coolantTempC), obdData.coolantTempC, 120f)
        DashboardWidgetId.THROTTLE -> Triple(String.format("%.0f", obdData.throttlePosPct), obdData.throttlePosPct, 100f)
        DashboardWidgetId.TIMING_ADVANCE -> Triple(String.format("%.1f", obdData.timingAdvanceDeg), obdData.timingAdvanceDeg, 45f)
        DashboardWidgetId.MAF -> Triple(String.format("%.1f", obdData.mafFlowRateGps), obdData.mafFlowRateGps, 150f)
        DashboardWidgetId.MANIFOLD_PRESSURE -> Triple(String.format("%.2f", obdData.manifoldPressureKpa / 100f), obdData.manifoldPressureKpa / 100f, 1.5f)
        DashboardWidgetId.BATTERY -> if (obdData.batteryVoltage.isNaN()) Triple("--", 0f, 16f) else Triple(String.format("%.2f", obdData.batteryVoltage), obdData.batteryVoltage, 16f)
        DashboardWidgetId.FUEL_TRIM -> Triple(String.format("%.1f", obdData.fuelTrimShortPct), obdData.fuelTrimShortPct, 25f)
        DashboardWidgetId.ENGINE_LOAD -> Triple(String.format("%.0f", obdData.engineLoadPct), obdData.engineLoadPct, 100f)
        DashboardWidgetId.POWER_HP -> Triple(String.format("%.0f", obdData.calculatedHp), obdData.calculatedHp, 200f)
        DashboardWidgetId.TORQUE_NM -> Triple(String.format("%.0f", obdData.calculatedTorqueNm), obdData.calculatedTorqueNm, 250f)
        DashboardWidgetId.G_FORCE -> Triple(String.format("%.2f", gForce.totalG), gForce.totalG, 1.5f)
    }

    when (slot.displayStyle) {
        WidgetDisplayStyle.SPARKLINE_CHART -> {
            val history = when (widget) {
                DashboardWidgetId.AFR_RATIO -> afrHistory
                DashboardWidgetId.INTAKE_TEMP -> iatHistory
                DashboardWidgetId.RPM -> rpmHistory
                else -> emptyList()
            }
            Box(modifier = Modifier.clickable { onClick() }) {
                SparklineGauge(
                    title = widget.title,
                    unit = widget.unit,
                    currentValue = numValue,
                    history = history,
                    lineColor = when (widget) {
                        DashboardWidgetId.AFR_RATIO -> TrackGreen
                        DashboardWidgetId.INTAKE_TEMP -> TurboBlue
                        else -> Mx5Amber
                    }
                )
            }
        }
        WidgetDisplayStyle.ANALOG_DIAL -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clickable { onClick() },
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
            ) {
                    val majorStep = when {
                        widget.maxVal >= 1000f -> 1000f
                        widget.maxVal >= 200f -> 40f
                        widget.maxVal >= 100f -> 20f
                        widget.maxVal >= 50f -> 10f
                        widget.maxVal >= 10f -> 2f
                        else -> 0.2f
                    }
                    AnalogGauge(
                        value = numValue,
                        minValue = widget.minVal,
                        maxValue = widget.maxVal,
                        title = widget.title,
                        unit = widget.unit,
                        majorTickStep = majorStep,
                        modifier = Modifier.size(130.dp)
                    )
            }
        }
        WidgetDisplayStyle.ARC_BAR -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clickable { onClick() },
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = widget.title, color = Color(0xFFB0BCCB), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "$valueStr ${widget.unit}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    HorizontalBarGauge(
                        label = "",
                        value = numValue,
                        unit = widget.unit,
                        minValue = widget.minVal,
                        maxValue = widget.maxVal
                    )
                }
            }
        }
        WidgetDisplayStyle.DIGITAL_CARD -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clickable { onClick() }
                    .testTag("widget_card_${slot.slotIndex}"),
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
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
                            text = "#${slot.slotIndex + 1}",
                            color = CockpitOutline,
                            fontSize = 10.sp
                        )
                    }

                    Column {
                        Text(
                            text = valueStr,
                            color = Color.White,
                            fontSize = 30.sp,
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

                    // Mini progress indicator
                    val fraction = ((numValue - widget.minVal) / (widget.maxVal - widget.minVal)).coerceIn(0f, 1f)
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
            .scale(scaleX = -1f, scaleY = 1f),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "HUD SPECCHIATO PER PARABREZZA",
                color = Color.DarkGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Text(
                text = String.format("%.0f", obdData.speedKmh),
                color = TrackGreen,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "KM/H",
                color = TrackGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AFR: ${String.format("%.1f", obdData.afr)} | IAT: ${String.format("%.0f", obdData.intakeTempC)}°C",
                color = Mx5Amber,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "MARCIA: ${obdData.calculatedGear} • ${String.format("%.0f", obdData.rpm)} RPM",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
