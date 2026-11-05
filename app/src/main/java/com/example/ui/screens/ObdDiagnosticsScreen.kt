package com.example.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.ObdConnectionState
import com.example.model.ObdConnectionType
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen
import com.example.ui.theme.WarningYellow
import com.example.viewmodel.TelemetryViewModel

@SuppressLint("MissingPermission")
@Composable
fun ObdDiagnosticsScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier
) {
    val connState by viewModel.obdManager.connectionState.collectAsState()
    val connType by viewModel.obdManager.activeConnectionType.collectAsState()
    val dtcCodes by viewModel.obdManager.dtcCodes.collectAsState()
    val rawLogs by viewModel.obdManager.rawLog.collectAsState()

    var wifiIp by remember { mutableStateOf("192.168.0.10") }
    var wifiPort by remember { mutableStateOf("35000") }

    // Paired Bluetooth devices list
    val bluetoothAdapter = remember {
        try { BluetoothAdapter.getDefaultAdapter() } catch (_: Exception) { null }
    }
    val pairedDevices: List<BluetoothDevice> = remember(connState) {
        try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "STATO ADATTATORE OBD2", color = Color(0xFF8A99AC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = connState.statusLabel,
                        color = when (connState) {
                            ObdConnectionState.CONNECTED -> TrackGreen
                            ObdConnectionState.ERROR -> RaceRed
                            ObdConnectionState.DISCONNECTED -> Color.White
                            else -> WarningYellow
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(text = "Modalità: ${connType.name}", color = Mx5Amber, fontSize = 12.sp)
                }

                if (connState == ObdConnectionState.CONNECTED) {
                    OutlinedButton(
                        onClick = { viewModel.disconnectObd() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RaceRed)
                    ) {
                        Text("Disconnetti", fontSize = 12.sp)
                    }
                }
            }
        }

        // Connection Modes Section
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "SELEZIONE CONNESSIONE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                // 1. Simulator Mode Button
                Button(
                    onClick = { viewModel.startSimulator() },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("start_simulator_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connType == ObdConnectionType.SIMULATOR && connState == ObdConnectionState.CONNECTED) Mx5Amber else CockpitSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = if (connType == ObdConnectionType.SIMULATOR && connState == ObdConnectionState.CONNECTED) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simulatore Dinamica Mazda MX-5 NBFL (Demo)",
                        color = if (connType == ObdConnectionType.SIMULATOR && connState == ObdConnectionState.CONNECTED) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // 2. Bluetooth ELM327 List
                Text(
                    text = "Dispositivi Bluetooth Associati (ELM327 / OBDLink / vLinker):",
                    color = Color(0xFF8A99AC),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (pairedDevices.isEmpty()) {
                    Text(
                        text = "Nessun dispositivo Bluetooth associato trovato. Associa il tuo adattatore OBD2 nelle Impostazioni Bluetooth di Android prima di connetterti.",
                        color = Color(0xFF6B7A8D),
                        fontSize = 11.sp
                    )
                } else {
                    pairedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CockpitSurfaceVariant)
                                .clickable { viewModel.connectBluetooth(device) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Mx5Amber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = device.name ?: "Dispositivo Sconosciuto", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = device.address, color = Color(0xFF8A99AC), fontSize = 10.sp)
                                }
                            }
                            Text(text = "CONNETTI", color = Mx5Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Wi-Fi Adapter Option
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Oppure Connessione Wi-Fi:", color = Color(0xFF8A99AC), fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wifiIp,
                        onValueChange = { wifiIp = it },
                        label = { Text("IP Adattatore") },
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Mx5Amber,
                            unfocusedBorderColor = CockpitOutline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = wifiPort,
                        onValueChange = { wifiPort = it },
                        label = { Text("Porta") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Mx5Amber,
                            unfocusedBorderColor = CockpitOutline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
                Button(
                    onClick = { viewModel.connectWifi(wifiIp, wifiPort.toIntOrNull() ?: 35000) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CockpitSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Mx5Amber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connetti via Wi-Fi", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Diagnostic Trouble Codes (DTC) Scanner Section
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "DIAGNOSI CODICI GUASTO (DTC)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = Mx5Amber)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.readFaultCodes() },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("scan_dtc_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Mx5Amber),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SCANSIONA DTC", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearFaultCodes() },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("clear_dtc_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RaceRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = RaceRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CANCELLA", fontSize = 12.sp)
                    }
                }

                if (dtcCodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CockpitSurfaceVariant)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun codice di errore rilevato nella centralina.",
                            color = TrackGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    dtcCodes.forEach { dtc ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(RaceRed.copy(alpha = 0.15f))
                                .border(1.dp, RaceRed, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = dtc.code, color = RaceRed, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Text(text = dtc.system, color = Color(0xFFB0BCCB), fontSize = 11.sp)
                            }
                            Text(
                                text = dtc.description,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Terminal Log Console
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Mx5Amber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "LOG COMANDI ELM327", color = Color(0xFF8A99AC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF090B0F))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        rawLogs.takeLast(15).forEach { line ->
                            Text(
                                text = "> $line",
                                color = Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
