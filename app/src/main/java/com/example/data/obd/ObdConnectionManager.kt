package com.example.data.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.example.model.DtcCode
import com.example.model.ObdConnectionState
import com.example.model.ObdConnectionType
import com.example.model.ObdData
import com.example.model.VehicleConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/**
 * Manages physical OBD2 connections (Bluetooth SPP, Wi-Fi TCP)
 * and dispatches live telemetry parameters.
 */
class ObdConnectionManager(
    private val scope: CoroutineScope,
    private val simulator: Mx5NbflSimulator
) {
    companion object {
        // Standard Bluetooth Serial Port Profile (SPP) UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val DEFAULT_WIFI_IP = "192.168.0.10"
        const val DEFAULT_WIFI_PORT = 35000
    }

    private val _connectionState = MutableStateFlow(ObdConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ObdConnectionState> = _connectionState.asStateFlow()

    private val _activeConnectionType = MutableStateFlow(ObdConnectionType.SIMULATOR)
    val activeConnectionType: StateFlow<ObdConnectionType> = _activeConnectionType.asStateFlow()

    private val _obdData = MutableStateFlow(ObdData())
    val obdData: StateFlow<ObdData> = _obdData.asStateFlow()

    private val _dtcCodes = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcCodes: StateFlow<List<DtcCode>> = _dtcCodes.asStateFlow()

    private val _rawLog = MutableStateFlow<List<String>>(emptyList())
    val rawLog: StateFlow<List<String>> = _rawLog.asStateFlow()

    private var connectionJob: Job? = null
    private var btSocket: BluetoothSocket? = null
    private var wifiSocket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    init {
        // Collect simulator data when in simulator mode
        scope.launch {
            simulator.simulatedData.collect { simData ->
                if (_activeConnectionType.value == ObdConnectionType.SIMULATOR) {
                    _obdData.value = simData
                }
            }
        }
    }

    fun startSimulator(vehicleConfig: VehicleConfig) {
        disconnect()
        _activeConnectionType.value = ObdConnectionType.SIMULATOR
        _connectionState.value = ObdConnectionState.CONNECTED
        simulator.start(vehicleConfig)
        log("Modalità Simulatore Mazda MX-5 NBFL avviata")
    }

    @SuppressLint("MissingPermission")
    fun connectBluetooth(device: BluetoothDevice, vehicleConfig: VehicleConfig) {
        disconnect()
        _activeConnectionType.value = ObdConnectionType.BLUETOOTH_CLASSIC
        _connectionState.value = ObdConnectionState.CONNECTING
        log("Connessione Bluetooth a ${device.name ?: device.address}...")

        connectionJob = scope.launch(Dispatchers.IO) {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                bluetoothAdapter?.cancelDiscovery()

                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                btSocket = socket
                inputStream = socket.inputStream
                outputStream = socket.outputStream

                _connectionState.value = ObdConnectionState.INITIALIZING
                log("Socket connesso. Inizializzazione protocollo ELM327...")

                initElm327()
                _connectionState.value = ObdConnectionState.CONNECTED
                log("Connesso con successo alla centralina OBD2 Mazda MX-5!")

                runObdPollingLoop(vehicleConfig)
            } catch (e: Exception) {
                log("Errore connessione Bluetooth: ${e.localizedMessage}")
                _connectionState.value = ObdConnectionState.ERROR
                closeSockets()
            }
        }
    }

    fun connectWifi(ip: String = DEFAULT_WIFI_IP, port: Int = DEFAULT_WIFI_PORT, vehicleConfig: VehicleConfig) {
        disconnect()
        _activeConnectionType.value = ObdConnectionType.WIFI
        _connectionState.value = ObdConnectionState.CONNECTING
        log("Connessione Wi-Fi a $ip:$port...")

        connectionJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), 5000)
                wifiSocket = socket
                inputStream = socket.inputStream
                outputStream = socket.outputStream

                _connectionState.value = ObdConnectionState.INITIALIZING
                log("Socket Wi-Fi aperto. Inizializzazione ELM327...")

                initElm327()
                _connectionState.value = ObdConnectionState.CONNECTED
                log("Connesso via Wi-Fi a centralina OBD2!")

                runObdPollingLoop(vehicleConfig)
            } catch (e: Exception) {
                log("Errore connessione Wi-Fi: ${e.localizedMessage}")
                _connectionState.value = ObdConnectionState.ERROR
                closeSockets()
            }
        }
    }

    fun disconnect() {
        simulator.stop()
        connectionJob?.cancel()
        connectionJob = null
        closeSockets()
        _connectionState.value = ObdConnectionState.DISCONNECTED
        log("Disconnesso")
    }

    private fun closeSockets() {
        try {
            inputStream?.close()
            outputStream?.close()
            btSocket?.close()
            wifiSocket?.close()
        } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        btSocket = null
        wifiSocket = null
    }

    private suspend fun initElm327() {
        log("ATZ -> ${sendCommand("ATZ", 4000)}")   // Reset ELM327 (i cloni possono metterci 1-3 s)
        log("ATE0 -> ${sendCommand("ATE0")}")        // Echo off
        log("ATL0 -> ${sendCommand("ATL0")}")        // Linefeeds off
        log("ATS0 -> ${sendCommand("ATS0")}")        // Spaces off
        log("ATH0 -> ${sendCommand("ATH0")}")        // Headers off
        log("ATSP0 -> ${sendCommand("ATSP0")}")      // Auto protocol search

        // Il primo comando OBD fa partire la ricerca del protocollo. Sulla NB (ISO 9141-2 /
        // KWP, init lenta a 5 baud) puo' richiedere 5-15 s: senza questa attesa la risposta
        // arriva in ritardo e finisce attaccata al comando successivo (sfasamento permanente).
        log("Ricerca protocollo ECU (puo' richiedere fino a 15 s, quadro su ON)...")
        val warmup = sendCommand("0100", 20000)
        log("0100 -> $warmup")
        log("Protocollo: ${sendCommand("ATDP")}")
    }

    private suspend fun runObdPollingLoop(config: VehicleConfig) {
        var currentRpm = 800f
        var currentSpeed = 0f
        var currentCoolant = 85f
        var currentIat = 25f
        var currentThrottle = 0f
        var currentTiming = 12f
        var currentMaf = 3.0f
        var currentVoltage = 14.0f
        var failedCycles = 0

        while (scope.isActive && _connectionState.value == ObdConnectionState.CONNECTED) {
            try {
                // Query RPM (PID 010C) - high priority
                val rpmResp = sendCommand("010C")
                val parsedRpm = parseRpm(rpmResp)
                if (parsedRpm != null) {
                    currentRpm = parsedRpm
                    failedCycles = 0
                } else {
                    failedCycles++
                    // Mostra nel log cosa risponde davvero l'adattatore (es. NO DATA, UNABLE TO CONNECT)
                    if (failedCycles <= 3 || failedCycles % 20 == 0) {
                        log("010C senza dati validi: \"$rpmResp\"")
                    }
                }

                // Query Speed (PID 010D)
                val speedResp = sendCommand("010D")
                parseSpeed(speedResp)?.let { currentSpeed = it }

                // Query Throttle Position (PID 0111)
                val tpsResp = sendCommand("0111")
                parseThrottle(tpsResp)?.let { currentThrottle = it }

                // Periodic slower PIDs (Coolant 0105, Timing 010E, MAF 0110, Voltage ATRV)
                val ectResp = sendCommand("0105")
                parseCoolant(ectResp)?.let { currentCoolant = it }

                val timingResp = sendCommand("010E")
                parseTiming(timingResp)?.let { currentTiming = it }

                val mafResp = sendCommand("0110")
                parseMaf(mafResp)?.let { currentMaf = it }

                // Query Intake Air Temperature (PID 010F)
                val iatResp = sendCommand("010F")
                parseIat(iatResp)?.let { currentIat = it }

                // Query Oxygen Sensor 1 Bank 1 Voltage (PID 0114)
                var currentO2Volt = 0.45f
                val o2Resp = sendCommand("0114")
                parseO2Voltage(o2Resp)?.let { currentO2Volt = it }

                // Calculate stoichiometric AFR from narrowband O2 voltage
                val lambdaVal = if (currentO2Volt >= 0.45f) {
                    (1.0f - (currentO2Volt - 0.45f) * 0.40f).coerceIn(0.75f, 1.0f)
                } else {
                    (1.0f + (0.45f - currentO2Volt) * 0.50f).coerceIn(1.0f, 1.30f)
                }
                val currentAfr = lambdaVal * 14.7f
                val mixStatus = when {
                    currentThrottle < 5f && currentRpm > 1800f -> com.example.model.MixtureStatus.CUTOFF
                    currentAfr < 14.2f -> com.example.model.MixtureStatus.RICH
                    currentAfr > 15.2f -> com.example.model.MixtureStatus.LEAN
                    else -> com.example.model.MixtureStatus.STOICHIOMETRIC
                }

                // Calculate Gear from Speed and RPM
                val gear = calculateGear(currentSpeed, currentRpm, config)
                val shiftActive = currentRpm >= config.shiftLightRpm
                val coolantWarning = currentCoolant >= config.coolantWarningTempC
                val isIatWarning = currentIat >= config.iatWarningTempC
                val isVvt = config.engineType == com.example.model.Mx5EngineType.BP_Z3_1_8_VVT && currentRpm > 4200f && currentThrottle > 40f

                // Power estimate
                val hp = (currentRpm / config.redlineRpm) * config.engineType.stockHp * (currentThrottle / 100f).coerceAtLeast(0.05f)
                val torque = if (currentRpm > 0) (hp * 7127f / currentRpm).coerceAtLeast(0f) else 0f

                _obdData.value = ObdData(
                    rpm = currentRpm,
                    speedKmh = currentSpeed,
                    coolantTempC = currentCoolant,
                    intakeTempC = currentIat,
                    throttlePosPct = currentThrottle,
                    timingAdvanceDeg = currentTiming,
                    mafFlowRateGps = currentMaf,
                    manifoldPressureKpa = 35f + (currentThrottle / 100f) * 65f,
                    batteryVoltage = currentVoltage,
                    engineLoadPct = (currentThrottle * 0.9f).coerceIn(10f, 100f),
                    calculatedHp = hp,
                    calculatedTorqueNm = torque,
                    calculatedGear = gear,
                    o2Voltage = currentO2Volt,
                    lambdaRatio = lambdaVal,
                    afr = currentAfr,
                    mixtureStatus = mixStatus,
                    isShiftLightActive = shiftActive,
                    isVvtActive = isVvt,
                    isCoolantWarningActive = coolantWarning,
                    isIatHeatSoakWarning = isIatWarning,
                    timestamp = System.currentTimeMillis()
                )

                delay(40) // Allow adapter to buffer
            } catch (e: Exception) {
                log("Errore lettura PID: ${e.message}")
                delay(200)
            }
        }
    }

    private fun calculateGear(speedKmh: Float, rpm: Float, config: VehicleConfig): String {
        if (speedKmh < 5f || rpm < 500f) return "N"
        val tireCirc = config.tireCircumferenceMeters
        val finalDrive = config.finalDriveRatio
        val speedMps = speedKmh / 3.6f

        var closestGear = "N"
        var minDiff = Float.MAX_VALUE

        config.transmission.ratios.forEachIndexed { index, ratio ->
            val expectedRpm = (speedMps * ratio * finalDrive / tireCirc) * 60f
            val diff = kotlin.math.abs(rpm - expectedRpm)
            if (diff < minDiff && diff < (rpm * 0.22f)) {
                minDiff = diff
                closestGear = (index + 1).toString()
            }
        }
        return closestGear
    }

    private suspend fun sendCommand(cmd: String, timeoutMs: Long = 2500): String = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext ""
        val inStream = inputStream ?: return@withContext ""

        // Svuota eventuali byte rimasti da risposte in ritardo, cosi' non si sfasano i comandi
        val trash = ByteArray(256)
        while (inStream.available() > 0) inStream.read(trash)

        out.write((cmd + "\r").toByteArray())
        out.flush()

        val buffer = ByteArray(256)
        val sb = StringBuilder()
        val startTime = System.currentTimeMillis()

        // Si legge finche' non arriva il prompt '>' (fine risposta), controllando
        // tutto il testo accumulato e non solo l'ultimo pezzo ricevuto
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (inStream.available() > 0) {
                val read = inStream.read(buffer)
                if (read > 0) {
                    sb.append(String(buffer, 0, read))
                    if (sb.contains(">")) break
                }
            } else {
                delay(10)
            }
        }
        sb.toString().replace(">", "").replace("\r", " ").replace("\n", " ").trim()
    }

    private fun parseRpm(response: String): Float? {
        val clean = response.replace(" ", "")
        val idx = clean.indexOf("410C")
        if (idx >= 0 && clean.length >= idx + 8) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 4f
        }
        return null
    }

    private fun parseSpeed(response: String): Float? {
        val clean = response.replace(" ", "")
        val idx = clean.indexOf("410D")
        if (idx >= 0 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return a.toFloat()
        }
        return null
    }

    private fun parseCoolant(response: String): Float? {
        val clean = response.replace(" ", "")
        val idx = clean.indexOf("4105")
        if (idx >= 0 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a - 40).toFloat()
        }
        return null
    }

    private fun parseThrottle(response: String): Float? {
        val clean = response.replace(" ", "")
        val idx = clean.indexOf("4111")
        if (idx >= 0 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a * 100f / 255f)
        }
        return null
    }

    private fun parseTiming(response: String): Float? {
        val clean = response.replace(" ", "")
        val idx = clean.indexOf("410E")
        if (idx >= 0 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a / 2f) - 64f
        }
        return null
    }

    private fun parseMaf(response: String): Float? {
        val clean = response.replace(" ", "")
        val idx = clean.indexOf("4110")
        if (idx >= 0 && clean.length >= idx + 8) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 100f
        }
        return null
    }

    private fun parseIat(response: String): Float? {
        val clean = response.replace(" ", "")
        val idx = clean.indexOf("410F")
        if (idx >= 0 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a - 40).toFloat()
        }
        return null
    }

    private fun parseO2Voltage(response: String): Float? {
        val clean = response.replace(" ", "")
        val idx = clean.indexOf("4114")
        if (idx >= 0 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a / 200f) // Standard OBD2 PID 0114: Voltage = A / 200
        }
        return null
    }

    fun readTroubleCodes() {
        scope.launch(Dispatchers.IO) {
            if (_activeConnectionType.value == ObdConnectionType.SIMULATOR) {
                // Sample Mazda MX-5 NBFL codes for demo/simulation
                _dtcCodes.value = listOf(
                    DtcCode("P0171", "Sistema troppo magro (Bancata 1) - Possibile debimetro MAF sporco o aspirazione aria", "Alimentazione Carburante", false),
                    DtcCode("P0300", "Mancata accensione casuale cilindri - Controllare candele e bobine NBFL", "Accensione", true)
                )
                log("Codici DTC simulati caricati")
                return@launch
            }

            try {
                val stored = sendCommand("03")
                val pending = sendCommand("07")
                val parsed = mutableListOf<DtcCode>()
                // Parse 43 xx xx xx
                _dtcCodes.value = parsed
                log("Scansione DTC completata: ${parsed.size} codici rilevati")
            } catch (e: Exception) {
                log("Errore lettura codici guasto: ${e.message}")
            }
        }
    }

    fun clearTroubleCodes() {
        scope.launch(Dispatchers.IO) {
            if (_activeConnectionType.value == ObdConnectionType.SIMULATOR) {
                _dtcCodes.value = emptyList()
                log("Codici DTC cancellati con successo (Simulatore)")
                return@launch
            }
            try {
                sendCommand("04")
                _dtcCodes.value = emptyList()
                log("Comando cancellazione codici 04 inviato")
            } catch (e: Exception) {
                log("Errore cancellazione codici: ${e.message}")
            }
        }
    }

    private fun log(msg: String) {
        val list = _rawLog.value.toMutableList()
        if (list.size > 50) list.removeAt(0)
        list.add(msg)
        _rawLog.value = list
    }
}
