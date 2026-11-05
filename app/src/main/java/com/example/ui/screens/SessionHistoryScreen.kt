package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TrackSessionEntity
import com.example.ui.components.TelemetryChart
import com.example.ui.theme.CockpitBlack
import com.example.ui.theme.CockpitOutline
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.Mx5Amber
import com.example.ui.theme.RaceRed
import com.example.ui.theme.TrackGreen
import com.example.viewmodel.TelemetryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionHistoryScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.savedSessions.collectAsState()
    val selectedSession by viewModel.selectedSessionDetail.collectAsState()
    val laps by viewModel.selectedSessionLaps.collectAsState()
    val telemetrySamples by viewModel.selectedSessionTelemetry.collectAsState()
    val context = LocalContext.current

    if (selectedSession != null) {
        // Detailed Session Telemetry View
        SessionDetailView(
            session = selectedSession!!,
            laps = laps,
            samples = telemetrySamples,
            onBack = { viewModel.loadSessionDetails(0) },
            onDelete = { viewModel.deleteSession(selectedSession!!.id) },
            onShare = {
                val shareText = StringBuilder()
                shareText.appendLine("🏁 Telemetria Mazda MX-5 NBFL - ${selectedSession!!.circuitName}")
                shareText.appendLine("Data: ${formatDate(selectedSession!!.timestamp)}")
                shareText.appendLine("Giri Totali: ${selectedSession!!.totalLaps}")
                selectedSession!!.bestLapMs?.let {
                    shareText.appendLine("Miglior Giro: ${formatLapTime(it)}")
                }
                shareText.appendLine("Velocità Max: ${String.format("%.1f", selectedSession!!.topSpeedKmh)} km/h")
                shareText.appendLine("Max G Laterale: ${String.format("%.2f", selectedSession!!.maxLatG)} G")
                shareText.appendLine("Max G Frenata: ${String.format("%.2f", selectedSession!!.maxBrakingG)} G")
                shareText.appendLine("\n--- Tempi sul Giro ---")
                laps.forEach { lap ->
                    val bestTag = if (lap.isBest) " 🏆 BEST" else ""
                    shareText.appendLine("Giro ${lap.lapNumber}: ${formatLapTime(lap.lapTimeMs)}$bestTag")
                }

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Telemetria MX-5 - ${selectedSession!!.circuitName}")
                    putExtra(Intent.EXTRA_TEXT, shareText.toString())
                }
                context.startActivity(Intent.createChooser(intent, "Condividi Telemetria"))
            }
        )
    } else {
        // List of all recorded sessions
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(CockpitBlack)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = Mx5Amber)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SESSIONI IN PISTA REGISTRATE",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color(0xFF6B7A8D),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nessuna sessione registrata",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vai nella scheda 'Pista' e premi 'AVVIA SESSIONE' per registrare i tuoi giri e la telemetria OBD2.",
                            color = Color(0xFF8A99AC),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sessions) { session ->
                        SessionItemCard(
                            session = session,
                            onClick = { viewModel.loadSessionDetails(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItemCard(session: TrackSessionEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("session_card_${session.id}"),
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitOutline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.circuitName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDate(session.timestamp),
                    color = Color(0xFF8A99AC),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CockpitSurfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MIGLIOR GIRO", color = Color(0xFF8A99AC), fontSize = 10.sp)
                    Text(
                        text = session.bestLapMs?.let { formatLapTime(it) } ?: "--:--.--",
                        color = TrackGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "GIRI TOTALI", color = Color(0xFF8A99AC), fontSize = 10.sp)
                    Text(
                        text = "${session.totalLaps}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "VEL. MAX", color = Color(0xFF8A99AC), fontSize = 10.sp)
                    Text(
                        text = "${String.format("%.0f", session.topSpeedKmh)} km/h",
                        color = Mx5Amber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetailView(
    session: TrackSessionEntity,
    laps: List<com.example.data.db.LapEntity>,
    samples: List<com.example.data.db.TelemetrySampleEntity>,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CockpitBlack)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                Text(
                    text = session.circuitName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = formatDate(session.timestamp),
                    color = Color(0xFF8A99AC),
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Condividi", tint = Mx5Amber)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = RaceRed)
            }
        }

        // Hero Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "BEST LAP", color = TrackGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = session.bestLapMs?.let { formatLapTime(it) } ?: "--:--.--",
                        color = TrackGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "VEL. MAX", color = Mx5Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format("%.0f", session.topSpeedKmh)} km/h",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MAX G LAT.", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${String.format("%.2f", session.maxLatG)} G",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Telemetry Chart
        TelemetryChart(
            samples = samples,
            title = "Traccia Telemetria Sessione (Velocità vs Giri Motore)"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Lap Times Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CockpitSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TEMPI SUL GIRO (${laps.size} GIRI)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                if (laps.isEmpty()) {
                    Text(text = "Nessun giro registrato", color = Color(0xFF6B7A8D), fontSize = 12.sp)
                } else {
                    laps.forEach { lap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (lap.isBest) TrackGreen.copy(alpha = 0.15f) else CockpitSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GIRO ${lap.lapNumber}",
                                    color = if (lap.isBest) TrackGreen else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (lap.isBest) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BEST",
                                        color = TrackGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Text(
                                text = formatLapTime(lap.lapTimeMs),
                                color = if (lap.isBest) TrackGreen else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}
