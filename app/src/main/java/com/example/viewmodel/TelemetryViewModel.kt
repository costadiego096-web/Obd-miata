package com.example.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.LapEntity
import com.example.data.db.TelemetrySampleEntity
import com.example.data.db.TrackSessionEntity
import com.example.data.obd.Mx5NbflSimulator
import com.example.data.obd.ObdConnectionManager
import com.example.data.sensor.GForceSensorManager
import com.example.model.DashboardLayoutType
import com.example.model.DashboardWidgetId
import com.example.model.LiveGForce
import com.example.model.LiveLapState
import com.example.model.ObdConnectionType
import com.example.model.ObdData
import com.example.model.SprintState
import com.example.model.SprintTestResult
import com.example.model.TrackCircuit
import com.example.model.TrackPresets
import com.example.model.VehicleConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val trackDao = db.trackDao()

    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    val simulator = Mx5NbflSimulator(viewModelScope)
    val obdManager = ObdConnectionManager(viewModelScope, simulator)
    val gForceManager = GForceSensorManager(application)

    // Vehicle Configuration
    private val _vehicleConfig = MutableStateFlow(VehicleConfig())
    val vehicleConfig: StateFlow<VehicleConfig> = _vehicleConfig.asStateFlow()

    // Dashboard Customization
    private val _dashboardLayout = MutableStateFlow(DashboardLayoutType.CLASSIC_NBFL)
    val dashboardLayout: StateFlow<DashboardLayoutType> = _dashboardLayout.asStateFlow()

    private val _gridWidgets = MutableStateFlow(
        listOf(
            DashboardWidgetId.SPEED,
            DashboardWidgetId.RPM,
            DashboardWidgetId.COOLANT,
            DashboardWidgetId.THROTTLE,
            DashboardWidgetId.TIMING_ADVANCE,
            DashboardWidgetId.G_FORCE
        )
    )
    val gridWidgets: StateFlow<List<DashboardWidgetId>> = _gridWidgets.asStateFlow()

    // Live G-Force (merged sensor + OBD/sim)
    private val _combinedGForce = MutableStateFlow(LiveGForce())
    val combinedGForce: StateFlow<LiveGForce> = _combinedGForce.asStateFlow()

    // Track Recording State
    private val _lapState = MutableStateFlow(LiveLapState())
    val lapState: StateFlow<LiveLapState> = _lapState.asStateFlow()

    private var lapTimerJob: Job? = null
    private var lapStartTimeMs = 0L
    private val currentSessionLaps = mutableListOf<LapEntity>()
    private val currentSessionSamples = mutableListOf<TelemetrySampleEntity>()
    private var sessionTopSpeed = 0f
    private var sessionMaxLatG = 0f
    private var sessionMaxBrakeG = 0f

    // Acceleration Sprint Test (0-100 km/h)
    private val _sprintResult = MutableStateFlow(SprintTestResult())
    val sprintResult: StateFlow<SprintTestResult> = _sprintResult.asStateFlow()
    private var sprintStartTimeMs = 0L
    private var sprintStartDistanceMeters = 0.0

    // Database Sessions
    val savedSessions = trackDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedSessionDetail = MutableStateFlow<TrackSessionEntity?>(null)
    val selectedSessionDetail: StateFlow<TrackSessionEntity?> = _selectedSessionDetail.asStateFlow()

    private val _selectedSessionLaps = MutableStateFlow<List<LapEntity>>(emptyList())
    val selectedSessionLaps: StateFlow<List<LapEntity>> = _selectedSessionLaps.asStateFlow()

    private val _selectedSessionTelemetry = MutableStateFlow<List<TelemetrySampleEntity>>(emptyList())
    val selectedSessionTelemetry: StateFlow<List<TelemetrySampleEntity>> = _selectedSessionTelemetry.asStateFlow()

    init {
        // Start by default in Simulator mode so the app works out of the box with live data!
        obdManager.startSimulator(_vehicleConfig.value)
        gForceManager.startListening()

        // Combine phone sensor and simulator G-forces
        viewModelScope.launch {
            while (isActive) {
                delay(40)
                val liveData = obdManager.obdData.value
                val gForce = if (obdManager.activeConnectionType.value == ObdConnectionType.SIMULATOR) {
                    simulator.simulatedGForce.value
                } else {
                    gForceManager.liveGForce.value
                }
                _combinedGForce.value = gForce

                // Shift light vibration alert
                if (liveData.isShiftLightActive) {
                    triggerShiftVibration()
                }

                // If recording track session, log sample periodically (every 100ms)
                if (_lapState.value.isRecording) {
                    recordTelemetrySample(liveData, gForce)
                }

                // Update Sprint test engine if active
                updateSprintEngine(liveData.speedKmh, gForce)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        obdManager.disconnect()
        gForceManager.stopListening()
    }

    // --- Configuration & UI Controls ---

    fun setDashboardLayout(layout: DashboardLayoutType) {
        _dashboardLayout.value = layout
    }

    fun setGridWidget(slotIndex: Int, widget: DashboardWidgetId) {
        val current = _gridWidgets.value.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = widget
            _gridWidgets.value = current
        }
    }

    fun updateVehicleConfig(config: VehicleConfig) {
        _vehicleConfig.value = config
        // If simulator is active, restart with new config
        if (obdManager.activeConnectionType.value == ObdConnectionType.SIMULATOR) {
            simulator.stop()
            simulator.start(config)
        }
    }

    fun connectBluetooth(device: BluetoothDevice) {
        obdManager.connectBluetooth(device, _vehicleConfig.value)
    }

    fun connectWifi(ip: String, port: Int) {
        obdManager.connectWifi(ip, port, _vehicleConfig.value)
    }

    fun startSimulator() {
        obdManager.startSimulator(_vehicleConfig.value)
    }

    fun disconnectObd() {
        obdManager.disconnect()
    }

    fun readFaultCodes() {
        obdManager.readTroubleCodes()
    }

    fun clearFaultCodes() {
        obdManager.clearTroubleCodes()
    }

    fun resetGForcePeaks() {
        gForceManager.resetPeaks()
        simulator.resetPeaks()
    }

    private fun triggerShiftVibration() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    // --- Track Telemetry & Lap Timing ---

    fun setCircuit(circuit: TrackCircuit) {
        _lapState.value = _lapState.value.copy(selectedCircuit = circuit)
    }

    fun toggleSessionRecording() {
        if (_lapState.value.isRecording) {
            stopSessionRecording()
        } else {
            startSessionRecording()
        }
    }

    fun startSessionRecording() {
        currentSessionLaps.clear()
        currentSessionSamples.clear()
        sessionTopSpeed = 0f
        sessionMaxLatG = 0f
        sessionMaxBrakeG = 0f
        lapStartTimeMs = System.currentTimeMillis()

        _lapState.value = _lapState.value.copy(
            currentLapNumber = 1,
            currentLapTimeMs = 0L,
            lastLapTimeMs = null,
            bestLapTimeMs = null,
            deltaToBestMs = null,
            totalSessionTimeMs = 0L,
            isRecording = true
        )

        lapTimerJob?.cancel()
        lapTimerJob = viewModelScope.launch {
            val sessionStart = System.currentTimeMillis()
            while (isActive && _lapState.value.isRecording) {
                delay(30)
                val now = System.currentTimeMillis()
                val currentLapTime = now - lapStartTimeMs
                val totalTime = now - sessionStart

                val best = _lapState.value.bestLapTimeMs
                val delta = if (best != null && currentLapTime > 0) currentLapTime - best else null

                _lapState.value = _lapState.value.copy(
                    currentLapTimeMs = currentLapTime,
                    totalSessionTimeMs = totalTime,
                    deltaToBestMs = delta
                )
            }
        }
    }

    fun triggerNewLap() {
        if (!_lapState.value.isRecording) return

        val now = System.currentTimeMillis()
        val finishedLapTime = now - lapStartTimeMs
        lapStartTimeMs = now

        val lapNum = _lapState.value.currentLapNumber
        val currentBest = _lapState.value.bestLapTimeMs
        val isNewBest = currentBest == null || finishedLapTime < currentBest
        val newBestTime = if (isNewBest) finishedLapTime else currentBest

        val lapEntity = LapEntity(
            sessionId = 0,
            lapNumber = lapNum,
            lapTimeMs = finishedLapTime,
            topSpeedKmh = sessionTopSpeed,
            avgSpeedKmh = 0f,
            isBest = isNewBest
        )
        currentSessionLaps.add(lapEntity)

        _lapState.value = _lapState.value.copy(
            currentLapNumber = lapNum + 1,
            currentLapTimeMs = 0L,
            lastLapTimeMs = finishedLapTime,
            bestLapTimeMs = newBestTime,
            deltaToBestMs = 0L
        )

        triggerShiftVibration()
    }

    fun stopSessionRecording() {
        lapTimerJob?.cancel()
        lapTimerJob = null

        val finishedLaps = currentSessionLaps.toList()
        val samples = currentSessionSamples.toList()
        val circuit = _lapState.value.selectedCircuit
        val totalDuration = _lapState.value.totalSessionTimeMs
        val bestLap = _lapState.value.bestLapTimeMs

        _lapState.value = _lapState.value.copy(isRecording = false)

        if (finishedLaps.isNotEmpty() || samples.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val session = TrackSessionEntity(
                    title = "Sessione ${circuit.name}",
                    circuitName = circuit.name,
                    timestamp = System.currentTimeMillis(),
                    durationMs = totalDuration,
                    totalLaps = finishedLaps.size.coerceAtLeast(1),
                    bestLapMs = bestLap,
                    topSpeedKmh = sessionTopSpeed,
                    maxLatG = sessionMaxLatG,
                    maxBrakingG = sessionMaxBrakeG,
                    vehicleDesc = "${_vehicleConfig.value.modelName} - ${_vehicleConfig.value.engineType.displayName}"
                )
                trackDao.saveFullSession(session, finishedLaps, samples)
            }
        }
    }

    private fun recordTelemetrySample(data: ObdData, gForce: LiveGForce) {
        if (data.speedKmh > sessionTopSpeed) sessionTopSpeed = data.speedKmh
        if (kotlin.math.abs(gForce.latG) > sessionMaxLatG) sessionMaxLatG = kotlin.math.abs(gForce.latG)
        if (kotlin.math.abs(gForce.longG) > sessionMaxBrakeG) sessionMaxBrakeG = kotlin.math.abs(gForce.longG)

        val sample = TelemetrySampleEntity(
            sessionId = 0,
            lapNumber = _lapState.value.currentLapNumber,
            timeOffsetMs = _lapState.value.totalSessionTimeMs,
            speedKmh = data.speedKmh,
            rpm = data.rpm,
            throttlePct = data.throttlePosPct,
            coolantC = data.coolantTempC,
            gear = data.calculatedGear,
            latG = gForce.latG,
            longG = gForce.longG,
            timingDeg = data.timingAdvanceDeg
        )
        currentSessionSamples.add(sample)
    }

    // --- Performance 0-100 Sprint Mode ---

    fun armSprintTest() {
        _sprintResult.value = SprintTestResult(
            state = SprintState.ARMED,
            currentSpeedKmh = obdManager.obdData.value.speedKmh
        )
    }

    fun resetSprintTest() {
        _sprintResult.value = SprintTestResult(state = SprintState.IDLE)
    }

    private fun updateSprintEngine(speedKmh: Float, gForce: LiveGForce) {
        val current = _sprintResult.value
        val now = System.currentTimeMillis()

        when (current.state) {
            SprintState.ARMED -> {
                // If stopped and launches
                if (speedKmh > 1.5f) {
                    sprintStartTimeMs = now
                    sprintStartDistanceMeters = 0.0
                    _sprintResult.value = current.copy(
                        state = SprintState.LAUNCHED,
                        elapsedTimeMs = 0L,
                        currentSpeedKmh = speedKmh,
                        peakAccelG = gForce.longG
                    )
                }
            }
            SprintState.LAUNCHED -> {
                val elapsed = now - sprintStartTimeMs
                var zeroToFifty = current.zeroToFiftyMs
                var zeroToOneHundred = current.zeroToOneHundredMs
                var quarterMile = current.quarterMileMs
                var quarterExit = current.quarterMileExitSpeedKmh

                if (zeroToFifty == null && speedKmh >= 50f) {
                    zeroToFifty = elapsed
                }
                if (zeroToOneHundred == null && speedKmh >= 100f) {
                    zeroToOneHundred = elapsed
                    triggerShiftVibration()
                }

                // Distance accumulation approximation (meters)
                sprintStartDistanceMeters += (speedKmh / 3.6) * 0.04
                if (quarterMile == null && sprintStartDistanceMeters >= 402.336) {
                    quarterMile = elapsed
                    quarterExit = speedKmh
                }

                val peakAccel = maxOf(current.peakAccelG, gForce.longG)

                val isComplete = zeroToOneHundred != null && (speedKmh < 95f || elapsed > 25000)

                _sprintResult.value = current.copy(
                    state = if (isComplete) SprintState.COMPLETED else SprintState.LAUNCHED,
                    zeroToFiftyMs = zeroToFifty,
                    zeroToOneHundredMs = zeroToOneHundred,
                    quarterMileMs = quarterMile,
                    quarterMileExitSpeedKmh = quarterExit,
                    currentSpeedKmh = speedKmh,
                    elapsedTimeMs = elapsed,
                    peakAccelG = peakAccel
                )
            }
            else -> {}
        }
    }

    // --- Saved Session Inspection & Telemetry Visualization ---

    fun loadSessionDetails(sessionId: Long) {
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) { trackDao.getSessionById(sessionId) }
            val laps = withContext(Dispatchers.IO) { trackDao.getLapsForSessionSync(sessionId) }
            val telemetry = withContext(Dispatchers.IO) { trackDao.getTelemetryForSessionSync(sessionId) }

            _selectedSessionDetail.value = session
            _selectedSessionLaps.value = laps
            _selectedSessionTelemetry.value = telemetry
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            trackDao.deleteSession(sessionId)
            if (_selectedSessionDetail.value?.id == sessionId) {
                _selectedSessionDetail.value = null
                _selectedSessionLaps.value = emptyList()
                _selectedSessionTelemetry.value = emptyList()
            }
        }
    }
}
