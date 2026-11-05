package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SprintState
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen
import com.example.ui.theme.WarningYellow
import com.example.viewmodel.TelemetryViewModel

@Composable
fun AccelerationSprintScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier
) {
    val sprintResult by viewModel.sprintResult.collectAsState()
    val obdData by viewModel.obdManager.obdData.collectAsState()
    val gForce by viewModel.combinedGForce.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "PROVA ACCELERAZIONE 0-100 & 400m",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Rilevamento automatico della partenza da fermo (Launch Control)",
            color = Color(0xFF8A99AC),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status pill
                val (statusText, statusColor) = when (sprintResult.state) {
                    SprintState.IDLE -> Pair("IN ATTESA", Color(0xFF8A99AC))
                    SprintState.ARMED -> Pair("PRONTO AL LANCIO - ACCELERA!", TrackGreen)
                    SprintState.LAUNCHED -> Pair("IN CORSO...", Mx5Amber)
                    SprintState.COMPLETED -> Pair("TEST COMPLETATO", TrackGreen)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .border(1.dp, statusColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                // Live Speed
                Text(
                    text = "${String.format("%.0f", obdData.speedKmh)} km/h",
                    color = Color.White,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // 0-100 km/h Hero Metric
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CockpitSurfaceVariant)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "0 - 100 KM/H",
                        color = Mx5Amber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sprintResult.zeroToOneHundredMs?.let {
                            String.format("%.2f s", it / 1000f)
                        } ?: if (sprintResult.state == SprintState.LAUNCHED) {
                            String.format("%.2f s", sprintResult.elapsedTimeMs / 1000f)
                        } else {
                            "--.-- s"
                        },
                        color = if (sprintResult.zeroToOneHundredMs != null) TrackGreen else Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Action Buttons: Arm Launch / Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.armSprintTest() },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("arm_sprint_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Mx5Amber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ARMA LANCIO",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(
                onClick = { viewModel.resetSprintTest() },
                modifier = Modifier
                    .weight(0.8f)
                    .height(52.dp)
                    .testTag("reset_sprint_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("RESET")
            }
        }

        // Split Results Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DETTAGLI PRESTAZIONI MISURATE",
                    color = Color(0xFF8A99AC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                SprintMetricRow(
                    label = "0 - 50 km/h",
                    value = sprintResult.zeroToFiftyMs?.let { String.format("%.2f s", it / 1000f) } ?: "--.-- s"
                )

                SprintMetricRow(
                    label = "0 - 100 km/h",
                    value = sprintResult.zeroToOneHundredMs?.let { String.format("%.2f s", it / 1000f) } ?: "--.-- s",
                    highlight = true
                )

                SprintMetricRow(
                    label = "1/4 di Miglio (400 metri)",
                    value = sprintResult.quarterMileMs?.let { String.format("%.2f s", it / 1000f) } ?: "--.-- s"
                )

                sprintResult.quarterMileExitSpeedKmh?.let { exitSpeed ->
                    SprintMetricRow(
                        label = "Velocità uscita 400m",
                        value = "${String.format("%.1f", exitSpeed)} km/h"
                    )
                }

                SprintMetricRow(
                    label = "Picco Accelerazione",
                    value = "${String.format("%.2f", sprintResult.peakAccelG)} G"
                )
            }
        }
    }
}

@Composable
private fun SprintMetricRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (highlight) Mx5Amber else Color(0xFFB0BCCB),
            fontSize = 13.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            color = if (highlight) TrackGreen else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
