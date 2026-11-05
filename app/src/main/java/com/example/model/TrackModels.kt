package com.example.model

data class TrackCircuit(
    val id: String,
    val name: String,
    val location: String,
    val lengthMeters: Int,
    val finishLineLat: Double,
    val finishLineLon: Double,
    val finishLineHeadingDeg: Float = 0f,
    val finishLineRadiusMeters: Double = 25.0
)

object TrackPresets {
    val CIRCUITS = listOf(
        TrackCircuit("monza", "Autodromo Nazionale Monza", "Monza (MB), Italia", 5793, 45.6189, 9.2812),
        TrackCircuit("misano", "Misano World Circuit Marco Simoncelli", "Misano Adriatico (RN), Italia", 4226, 43.9625, 12.6842),
        TrackCircuit("mugello", "Autodromo Internazionale del Mugello", "Scarperia (FI), Italia", 5245, 43.9975, 11.3714),
        TrackCircuit("vallelunga", "Autodromo Piero Taruffi Vallelunga", "Campagnano (RM), Italia", 4085, 42.1586, 12.3703),
        TrackCircuit("modena", "Autodromo di Modena", "Marzaglia (MO), Italia", 2007, 44.6289, 10.8123),
        TrackCircuit("custom", "Tracciato Personalizzato / Manuale", "Punto GPS o Trigger Manuale", 0, 0.0, 0.0)
    )
}

data class LiveGForce(
    val latG: Float = 0f,      // + Right, - Left
    val longG: Float = 0f,     // + Acceleration, - Braking
    val peakLeftG: Float = 0f,
    val peakRightG: Float = 0f,
    val peakBrakingG: Float = 0f,
    val peakAccelG: Float = 0f
) {
    val totalG: Float
        get() = kotlin.math.sqrt(latG * latG + longG * longG)
}

data class LiveLapState(
    val currentLapNumber: Int = 1,
    val currentLapTimeMs: Long = 0L,
    val lastLapTimeMs: Long? = null,
    val bestLapTimeMs: Long? = null,
    val deltaToBestMs: Long? = null, // e.g. -450ms (faster) or +620ms (slower)
    val totalSessionTimeMs: Long = 0L,
    val isRecording: Boolean = false,
    val selectedCircuit: TrackCircuit = TrackPresets.CIRCUITS[0]
)

enum class SprintState {
    IDLE,
    ARMED, // Stopped at 0 km/h, ready to launch
    LAUNCHED, // Accelerating
    COMPLETED
}

data class SprintTestResult(
    val state: SprintState = SprintState.IDLE,
    val zeroToFiftyMs: Long? = null,
    val zeroToOneHundredMs: Long? = null,
    val quarterMileMs: Long? = null,
    val quarterMileExitSpeedKmh: Float? = null,
    val hundredToZeroMs: Long? = null,
    val hundredToZeroDistanceMeters: Float? = null,
    val peakAccelG: Float = 0f,
    val peakBrakingG: Float = 0f,
    val currentSpeedKmh: Float = 0f,
    val elapsedTimeMs: Long = 0L,
    val runTimestamp: Long = System.currentTimeMillis()
)
