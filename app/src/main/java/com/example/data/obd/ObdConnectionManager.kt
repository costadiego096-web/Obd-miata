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

    // Configurazione veicolo SEMPRE aggiornata: il ciclo di lettura la rilegge a ogni giro,
    // cosi' le modifiche fatte nelle impostazioni (cambio, soglie...) valgono subito,
    // senza dover riconnettere l'adattatore.
    @Volatile
    private var activeConfig: VehicleConfig = VehicleConfig()

    fun updateConfig(config: VehicleConfig) {
        activeConfig = config
    }

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
        activeConfig = vehicleConfig
        disconnect()
        _activeConnectionType.value = ObdConnectionType.SIMULATOR
        _connectionState.value = ObdConnectionState.CONNECTED
        simulator.start(vehicleConfig)
        log("Modalità Simulatore Mazda MX-5 NBFL avviata")
    }

    @SuppressLint("MissingPermission")
    fun connectBluetooth(device: BluetoothDevice, vehicleConfig: VehicleConfig) {
        activeConfig = vehicleConfig
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

                runObdPollingLoop()
            } catch (e: Exception) {
                log("Errore connessione Bluetooth: ${e.localizedMessage}")
                _connectionState.value = ObdConnectionState.ERROR
                closeSockets()
            }
        }
    }

    fun connectWifi(ip: String = DEFAULT_WIFI_IP, port: Int = DEFAULT_WIFI_PORT, vehicleConfig: VehicleConfig) {
        activeConfig = vehicleConfig
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

                runObdPollingLoop()
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
        log("ATZ -> ${sendCommand("ATZ", 4000)}")    // Reset ELM327 (i cloni possono metterci 1-3 s)
        log("ATE0 -> ${sendCommand("ATE0")}")         // Echo off
        log("ATL0 -> ${sendCommand("ATL0")}")         // Linefeeds off
        log("ATS0 -> ${sendCommand("ATS0")}")         // Spaces off
        log("ATH0 -> ${sendCommand("ATH0")}")         // Headers off
        log("ATSP0 -> ${sendCommand("ATSP0")}")       // Auto protocol search
        log("ATAT2 -> ${sendCommand("ATAT2")}")       // Adaptive timing aggressivo (riduce i timeout)

        // Il primo comando OBD fa partire la ricerca del protocollo. Sulla NB (ISO 9141-2 /
        // KWP, init lenta a 5 baud) puo' richiedere 5-15 s: senza questa attesa la risposta
        // arriva in ritardo e finisce attaccata al comando successivo (sfasamento permanente).
        log("Ricerca protocollo ECU (puo' richiedere fino a 15 s, quadro su ON)...")
        val warmup = sendCommand("0100", 20000)
        log("0100 -> $warmup")
        log("Protocollo: ${sendCommand("ATDP")}")
    }

    private suspend fun runObdPollingLoop() {
        resetGearTracking()
        var currentRpm = 800f
        var currentSpeed = 0f
        var currentCoolant = 85f
        var currentIat = 25f
        var currentThrottle = 0f
        var currentTiming = 12f
        var currentMaf = 3.0f
        var currentO2Volt = 0.45f
        var currentVoltage = Float.NaN // NaN = non ancora letta (mostrata come "--")
        var failedCycles = 0
        var cycleCount = 0
        var slowIndex = 0
        var battFailCount = 0
        var statStart = System.nanoTime()

        // Legge un valore secondario (TPS, O2, anticipo, MAF, temperature, batteria)
        suspend fun readSlow(kind: String) {
            when (kind) {
                "TPS" -> parseThrottle(sendPid("0111", 500))?.let { currentThrottle = it }
                "O2" -> parseO2Voltage(sendPid("0114", 500))?.let { currentO2Volt = it }
                "TIMING" -> parseTiming(sendPid("010E", 500))?.let { currentTiming = it }
                "MAF" -> parseMaf(sendPid("0110", 500))?.let { currentMaf = it }
                "ECT" -> parseCoolant(sendPid("0105", 500))?.let { currentCoolant = it }
                "IAT" -> parseIat(sendPid("010F", 500))?.let { currentIat = it }
                "BATT" -> {
                    // ATRV: tensione misurata dall'adattatore sul pin 16 della presa OBD
                    val battResp = sendCommand("ATRV", 500)
                    val v = parseBatteryVoltage(battResp)
                    currentVoltage = v ?: Float.NaN
                    if (v == null) {
                        battFailCount++
                        if (battFailCount <= 3) log("ATRV senza valore valido: \"$battResp\"")
                    }
                }
            }
        }

        // Lettura iniziale di tutti i valori, cosi' non si vedono valori inventati all'avvio
        try {
            for (kind in listOf("TPS", "O2", "TIMING", "MAF", "ECT", "IAT", "BATT")) readSlow(kind)
        } catch (e: Exception) {
            log("Lettura iniziale parziale: ${e.message}")
        }

        while (scope.isActive && _connectionState.value == ObdConnectionState.CONNECTED) {
            try {
                // ---- RPM e VELOCITA': a ogni ciclo, cosi' giri e marcia sono sempre freschi ----
                val rpmResp = sendPid("010C", 500)
                val parsedRpm = parseRpm(rpmResp)
                if (parsedRpm != null) {
                    currentRpm = parsedRpm
                    failedCycles = 0
                } else {
                    failedCycles++
                    if (failedCycles <= 3 || failedCycles % 20 == 0) {
                        log("010C senza dati validi: \"$rpmResp\"")
                    }
                }

                val speedResp = sendPid("010D", 500)
                parseSpeed(speedResp)?.let { currentSpeed = it }

                // ---- Un solo valore "secondario" a rotazione, un ciclo si' e uno no ----
                // Prima c'erano cicli con 9 richieste di fila (ogni 5/10/20 cicli) che facevano
                // "fermare" giri e marcia per un attimo: ora il tempo di ogni ciclo e' costante.
                if (cycleCount % 2 == 1) {
                    readSlow(slowRotation[slowIndex % slowRotation.size])
                    slowIndex++
                }

                // ---- Calcoli derivati ----
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

                // Config riletta a ogni ciclo (vale subito se cambi cambio/soglie)
                val config = activeConfig

                // Calculate Gear from Speed and RPM
                val gear = stabilizeGear(calculateGear(currentSpeed, currentRpm, config))
                val shiftActive = currentRpm >= config.shiftLightRpm
                val coolantWarning = currentCoolant >= config.coolantWarningTempC
                val isIatWarning = currentIat >= config.iatWarningTempC
                val isVvt = config.engineType == com.example.model.Mx5EngineType.BP_Z3_1_8_VVT &&
                            currentRpm > 4200f && currentThrottle > 40f

                // Power estimate
                val hp = (currentRpm / config.redlineRpm) * config.engineType.stockHp *
                         (currentThrottle / 100f).coerceAtLeast(0.05f)
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

                cycleCount++
                if (cycleCount % 100 == 0) {
                    val now = System.nanoTime()
                    val msPerCycle = (now - statStart) / 1_000_000f / 100f
                    log(String.format(java.util.Locale.US, "Lettura: %.0f ms/ciclo (%.1f aggiornamenti/s)", msPerCycle, 1000f / msPerCycle))
                    statStart = now
                }
                // NIENTE delay(40): il prompt '>' gia' sincronizza i comandi
            } catch (e: Exception) {
                log("Errore lettura PID: ${e.message}")
                delay(200)
            }
        }
    }

    /**
     * Marcia istantanea da velocita' e giri: "N" se fermo, null se nessuna marcia e' compatibile
     * (frizione premuta, slittamento, transitorio), altrimenti "1".."6".
     *
     * Si confronta il rapporto osservato (giri motore / giri ruota) con quello di ogni marcia,
     * in percentuale: e' piu' preciso del confronto sui giri assoluti, e la tolleranza cresce a
     * bassa velocita' perche' la velocita' OBD e' un numero intero di km/h.
     */
    private fun calculateGear(speedKmh: Float, rpm: Float, config: VehicleConfig): String? {
        if (speedKmh < 5f || rpm < 500f) return "N"
        val wheelRps = (speedKmh / 3.6f) / config.tireCircumferenceMeters
        val observedRatio = (rpm / 60f) / (wheelRps * config.finalDriveRatio)
        val tolerance = 0.10f + 0.5f / speedKmh

        var bestGear: String? = null
        var bestError = Float.MAX_VALUE
        config.transmission.ratios.forEachIndexed { index, ratio ->
            val error = kotlin.math.abs(observedRatio - ratio) / ratio
            if (error < bestError) {
                bestError = error
                bestGear = (index + 1).toString()
            }
        }
        return if (bestError <= tolerance) bestGear else null
    }

    // Rotazione dei valori secondari: la farfalla (TPS) ogni 2 posti, la sonda lambda 3 volte su 16,
    // gli altri una volta sola ogni giro completo
    private val slowRotation = listOf(
        "TPS", "O2", "TPS", "TIMING", "TPS", "O2", "TPS", "MAF",
        "TPS", "O2", "TPS", "ECT", "TPS", "IAT", "TPS", "BATT"
    )

    // --- Stabilizzazione: la marcia cambia solo dopo piu' letture uguali di fila ---
    private var confirmedGear = "N"
    private var gearCandidate = "N"
    private var gearCandidateCount = 0

    private fun resetGearTracking() {
        confirmedGear = "N"
        gearCandidate = "N"
        gearCandidateCount = 0
    }

    private fun stabilizeGear(raw: String?): String {
        if (raw == "N") {
            resetGearTracking()
            return "N"
        }
        // Nessuna marcia compatibile (frizione premuta / transitorio): si tiene l'ultima nota
        if (raw == null) return confirmedGear
        if (raw == gearCandidate) gearCandidateCount++ else {
            gearCandidate = raw
            gearCandidateCount = 1
        }
        if (gearCandidateCount >= 2) confirmedGear = raw
        return confirmedGear
    }

    private suspend fun sendCommand(cmd: String, timeoutMs: Long = 800): String = withContext(Dispatchers.IO) {
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
        // tutto il testo accumulato e non solo l'ultimo pezzo ricevuto.
        // Thread.sleep(1) invece di delay(10): molto piu' preciso sulla latenza.
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (inStream.available() > 0) {
                val read = inStream.read(buffer)
                if (read > 0) {
                    sb.append(String(buffer, 0, read))
                    if (sb.contains('>')) break
                }
            } else {
                Thread.sleep(1)
            }
        }
        sb.toString().replace(">", "").replace("\r", " ").replace("\n", " ").trim()
    }

    // Con il suffisso "1" (es. "010C1") l'ELM327 risponde appena arriva la prima risposta
    // della centralina, senza aspettare il timeout. Sulla linea K della NB fa guadagnare
    // fino a ~200 ms per PID. Se l'adattatore non lo supporta (risponde "?"), si disattiva.
    private var useResponseCount = true

    private suspend fun sendPid(pid: String, timeoutMs: Long = 800): String {
        if (!useResponseCount) return sendCommand(pid, timeoutMs)
        val resp = sendCommand(pid + "1", timeoutMs)
        if (resp.contains("?")) {
            useResponseCount = false
            log("Adattatore senza suffisso risposta: modalita' standard")
            return sendCommand(pid, timeoutMs)
        }
        return resp
    }

    private fun parseBatteryVoltage(response: String): Float? {
        val m = Regex("""(\d{1,2}(?:[.,]\d{1,2})?)\s*V""", RegexOption.IGNORE_CASE).find(response) ?: return null
        val v = m.groupValues[1].replace(',', '.').toFloatOrNull() ?: return null
        return if (v in 5f..20f) v else null
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
