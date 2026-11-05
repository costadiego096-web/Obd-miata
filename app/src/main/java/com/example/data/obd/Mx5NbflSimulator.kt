package com.example.data.obd

import com.example.model.LiveGForce
import com.example.model.Mx5EngineType
import com.example.model.Mx5Transmission
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
import kotlin.math.sin

/**
 * Realistic Mazda MX-5 NBFL physical track & street simulator.
 * Simulates engine dynamics for 1.8L BP-Z3 with VVT and 6-speed gearbox,
 * cornering G-forces, braking, and lap telemetry.
 */
class Mx5NbflSimulator(
    private val scope: CoroutineScope
) {
    private val _simulatedData = MutableStateFlow(ObdData())
    val simulatedData: StateFlow<ObdData> = _simulatedData.asStateFlow()

    private val _simulatedGForce = MutableStateFlow(LiveGForce())
    val simulatedGForce: StateFlow<LiveGForce> = _simulatedGForce.asStateFlow()

    private var job: Job? = null
    var isRunning: Boolean = false
        private set

    // Simulation state
    private var currentGear = 1
    private var engineRpm = 850f
    private var vehicleSpeed = 0f
    private var throttle = 0f
    private var coolant = 88f
    private var trackPhase = 0f // progress along a track lap cycle (0 to 100)
    private var isBraking = false
    private var isShifting = false
    private var shiftTimer = 0f

    // Peak G tracking
    private var peakLeft = 0f
    private var peakRight = 0f
    private var peakBrake = 0f
    private var peakAccel = 0f

    fun start(vehicleConfig: VehicleConfig = VehicleConfig()) {
        if (isRunning) return
        isRunning = true
        job = scope.launch(Dispatchers.Default) {
            var step = 0L
            while (isActive) {
                delay(50) // 20 Hz update rate (standard OBD/telemetry refresh)
                step++
                updatePhysics(vehicleConfig, step)
            }
        }
    }

    fun stop() {
        isRunning = false
        job?.cancel()
        job = null
    }

    fun resetPeaks() {
        peakLeft = 0f
        peakRight = 0f
        peakBrake = 0f
        peakAccel = 0f
        _simulatedGForce.value = LiveGForce()
    }

    private fun updatePhysics(config: VehicleConfig, step: Long) {
        // Track cycle: 0..100% represents a lap with straights, turns, braking zones
        trackPhase = (trackPhase + 0.12f) % 100f

        val ratios = config.transmission.ratios
        val finalDrive = config.finalDriveRatio
        val tireCirc = config.tireCircumferenceMeters
        val redline = config.redlineRpm.toFloat()
        val shiftRpm = config.shiftLightRpm.toFloat()

        var targetThrottle: Float
        var targetLatG: Float
        var targetLongG: Float

        when {
            // Main Straight (0% to 22%): Full throttle, upshifting 3 -> 4 -> 5 -> 6
            trackPhase in 0.0f..22.0f -> {
                targetThrottle = 100f
                isBraking = false
                targetLatG = sin(trackPhase * 2f) * 0.08f // slight straight wandering
                targetLongG = 0.45f * (1.0f - vehicleSpeed / 200f).coerceAtLeast(0.1f)
            }
            // Heavy Braking Zone for Turn 1 (22% to 27%)
            trackPhase in 22.0f..27.0f -> {
                targetThrottle = 0f
                isBraking = true
                targetLatG = 0.1f
                targetLongG = -0.92f // Heavy threshold braking
            }
            // Turn 1 & 2 Chicane (27% to 40%): High lateral cornering Gs, 2nd & 3rd gear
            trackPhase in 27.0f..40.0f -> {
                targetThrottle = 55f + sin(trackPhase * 1.5f) * 35f
                isBraking = false
                targetLatG = 0.95f * sin((trackPhase - 27f) * 0.45f) // up to 1.1G cornering
                targetLongG = 0.15f
            }
            // Acceleration zone (40% to 58%): Curva Grande / Intermediate straight
            trackPhase in 40.0f..58.0f -> {
                targetThrottle = 95f
                isBraking = false
                targetLatG = 0.4f * sin(trackPhase * 0.8f)
                targetLongG = 0.35f
            }
            // Medium braking and technical hairpin (58% to 75%)
            trackPhase in 58.0f..75.0f -> {
                if (trackPhase in 58f..63f) {
                    targetThrottle = 5f
                    isBraking = true
                    targetLongG = -0.78f
                    targetLatG = -0.3f
                } else {
                    targetThrottle = 65f
                    isBraking = false
                    targetLongG = 0.2f
                    targetLatG = -1.05f // Hard left turn
                }
            }
            // Final high speed sweeping turn & run to finish line (75% to 100%)
            else -> {
                targetThrottle = 88f
                isBraking = false
                targetLatG = 0.85f // Fast right sweeper (e.g. Parabolica)
                targetLongG = 0.3f
            }
        }

        // Smooth throttle response
        throttle += (targetThrottle - throttle) * 0.2f

        // Handle shifting and RPM dynamics
        if (isShifting) {
            shiftTimer -= 0.05f
            engineRpm -= 280f
            if (shiftTimer <= 0f) {
                isShifting = false
            }
        } else if (isBraking) {
            // Decelerating
            vehicleSpeed = (vehicleSpeed - 3.2f).coerceAtLeast(35f)
            val currentRatio = ratios.getOrElse(currentGear - 1) { 1.0f }
            val calculatedRpm = (vehicleSpeed / 3.6f) * currentRatio * finalDrive / tireCirc * 60f
            engineRpm = calculatedRpm.coerceIn(1800f, redline)

            // Downshift if RPM drops
            if (engineRpm < 2600f && currentGear > 2) {
                currentGear--
                // Blip throttle for rev matching!
                engineRpm += 1200f
            }
        } else {
            // Accelerating
            val currentRatio = ratios.getOrElse(currentGear - 1) { 1.0f }
            val accelRate = (throttle / 100f) * (38f / currentRatio)
            engineRpm += accelRate

            // Upshift if reaching shift light
            if (engineRpm >= shiftRpm && currentGear < ratios.size) {
                isShifting = true
                shiftTimer = 0.18f // 180ms quick manual shift
                currentGear++
                val nextRatio = ratios.getOrElse(currentGear - 1) { 1.0f }
                engineRpm = engineRpm * (nextRatio / currentRatio)
            } else if (engineRpm > redline) {
                engineRpm = redline - (step % 2) * 120f // Rev limiter bounce!
            }

            // Calculate speed from RPM & gear ratio
            val speedMps = (engineRpm / 60f) * tireCirc / (currentRatio * finalDrive)
            vehicleSpeed = (speedMps * 3.6f).coerceIn(0f, 225f)
        }

        // VVT (Variable Valve Timing / S-VT) is active under load and RPM > 4000 on NBFL 1.8
        val isVvt = config.engineType == Mx5EngineType.BP_Z3_1_8_VVT && engineRpm > 4200f && throttle > 40f
        val timingAdvance = if (isVvt) 28f + (engineRpm / 7000f) * 8f else 12f + (engineRpm / 7000f) * 16f
        val mafGps = (engineRpm / 7200f) * (config.engineType.displacementCc / 1000f) * 65f * (throttle / 100f).coerceAtLeast(0.08f)

        // Coolant temperature: warms up to ~92C, rises under track load to ~98C
        coolant = (coolant + (if (throttle > 70f) 0.02f else -0.015f)).coerceIn(87f, 102f)

        // Estimated engine load
        val engineLoad = (throttle * 0.85f + (engineRpm / redline) * 15f).coerceIn(12f, 100f)

        // Intake Air Temperature (IAT - PID 010F)
        // Airflow at high speed cools the intake, while high engine heat and full throttle warm it up
        val speedCooling = (vehicleSpeed / 200f) * 4.0f
        val engineHeatSoak = (engineLoad / 100f) * 12.0f
        val calculatedIat = (26f + engineHeatSoak - speedCooling + sin(step * 0.05f).toFloat() * 1.5f).coerceIn(18f, 65f)
        val isIatHeatSoak = calculatedIat >= config.iatWarningTempC

        // Sonda Lambda O2 originale Narrowband (0-1V) & Rapporto Stechiometrico AFR (14.7:1)
        val o2Volt: Float
        val currentAfr: Float
        val lambdaVal: Float
        val mixStatus: com.example.model.MixtureStatus

        when {
            // Deceleration fuel cut-off (rilascio gas in marcia con giri elevati)
            throttle < 5f && engineRpm > 1700f -> {
                o2Volt = (0.04f + kotlin.math.abs(sin(step * 0.2f).toFloat()) * 0.02f).coerceIn(0.01f, 0.08f)
                currentAfr = 18.2f + kotlin.math.abs(sin(step * 0.1f).toFloat()) * 0.5f
                lambdaVal = currentAfr / 14.7f
                mixStatus = com.example.model.MixtureStatus.CUTOFF
            }
            // Piena accelerazione WOT / Carico elevato (Open Loop - Miscela Ricca per potenza e sicurezza)
            throttle > 75f -> {
                o2Volt = (0.87f + (throttle / 100f) * 0.06f + sin(step * 0.3f).toFloat() * 0.02f).coerceIn(0.85f, 0.95f)
                currentAfr = 12.6f + (100f - throttle) * 0.025f // Tipico 12.6 - 13.2 AFR per BP-Z3 WOT
                lambdaVal = currentAfr / 14.7f
                mixStatus = com.example.model.MixtureStatus.RICH
            }
            // Andatura parziale moderata (Closed Loop - Oscillazione attorno a 14.7 stechiometrico)
            else -> {
                val wave = sin(step * 0.4f).toFloat() // Zirconia narrowband dither cycle
                o2Volt = (0.45f + wave * 0.36f).coerceIn(0.08f, 0.88f)
                currentAfr = 14.7f - (wave * 0.32f)
                lambdaVal = currentAfr / 14.7f
                mixStatus = if (currentAfr < 14.3f) {
                    com.example.model.MixtureStatus.RICH
                } else if (currentAfr > 15.2f) {
                    com.example.model.MixtureStatus.LEAN
                } else {
                    com.example.model.MixtureStatus.STOICHIOMETRIC
                }
            }
        }

        // Estimated power output
        val powerFactor = when (config.engineType) {
            Mx5EngineType.BP_Z3_1_8_VVT -> 146f
            Mx5EngineType.B6_ZE_1_6 -> 110f
            Mx5EngineType.TURBO_CUSTOM -> 220f
        }
        val estimatedHp = (engineRpm / redline) * powerFactor * (throttle / 100f)
        val estimatedTorque = if (engineRpm > 0) (estimatedHp * 7127f / engineRpm).coerceAtLeast(0f) else 0f

        val shiftLightActive = engineRpm >= config.shiftLightRpm
        val coolantWarning = coolant >= config.coolantWarningTempC

        // Update G-Forces
        if (targetLatG < 0 && targetLatG < peakLeft) peakLeft = targetLatG
        if (targetLatG > 0 && targetLatG > peakRight) peakRight = targetLatG
        if (targetLongG < 0 && targetLongG < peakBrake) peakBrake = targetLongG
        if (targetLongG > 0 && targetLongG > peakAccel) peakAccel = targetLongG

        _simulatedGForce.value = LiveGForce(
            latG = targetLatG,
            longG = targetLongG,
            peakLeftG = peakLeft,
            peakRightG = peakRight,
            peakBrakingG = peakBrake,
            peakAccelG = peakAccel
        )

        _simulatedData.value = ObdData(
            rpm = engineRpm,
            speedKmh = vehicleSpeed,
            coolantTempC = coolant,
            intakeTempC = calculatedIat,
            throttlePosPct = throttle,
            timingAdvanceDeg = if (isIatHeatSoak) timingAdvance - 4.0f else timingAdvance,
            mafFlowRateGps = mafGps,
            manifoldPressureKpa = 32f + (throttle / 100f) * 68f, // 100 kPa atmospheric
            batteryVoltage = 14.1f - (engineRpm / 15000f),
            fuelTrimShortPct = sin(step * 0.1f).toFloat() * 2.5f,
            fuelTrimLongPct = -1.2f,
            engineLoadPct = engineLoad,
            fuelLevelPct = 58f,
            calculatedHp = estimatedHp,
            calculatedTorqueNm = estimatedTorque,
            calculatedGear = currentGear.toString(),
            o2Voltage = o2Volt,
            lambdaRatio = lambdaVal,
            afr = currentAfr,
            mixtureStatus = mixStatus,
            isShiftLightActive = shiftLightActive,
            isVvtActive = isVvt,
            isCoolantWarningActive = coolantWarning,
            isIatHeatSoakWarning = isIatHeatSoak,
            timestamp = System.currentTimeMillis()
        )
    }
}
