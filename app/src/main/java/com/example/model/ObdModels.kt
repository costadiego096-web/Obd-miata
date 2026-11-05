package com.example.model

enum class Mx5EngineType(val displayName: String, val displacementCc: Int, val stockHp: Int, val defaultRedline: Int) {
    BP_Z3_1_8_VVT("1.8L 16V S-VT (BP-Z3)", 1839, 146, 7200),
    B6_ZE_1_6("1.6L 16V (B6-ZE)", 1598, 110, 7000),
    TURBO_CUSTOM("1.8L BP Turbo / Supercharged", 1839, 220, 7500)
}

enum class Mx5Transmission(val displayName: String, val ratios: List<Float>) {
    SIX_SPEED("Cambio Manuale 6 Marce (Aisin AZ6)", listOf(3.760f, 2.269f, 1.645f, 1.257f, 1.000f, 0.843f)),
    FIVE_SPEED("Cambio Manuale 5 Marce", listOf(3.136f, 1.888f, 1.330f, 1.000f, 0.814f))
}

data class VehicleConfig(
    val modelName: String = "Mazda MX-5 NBFL (2001-2005)",
    val engineType: Mx5EngineType = Mx5EngineType.BP_Z3_1_8_VVT,
    val transmission: Mx5Transmission = Mx5Transmission.SIX_SPEED,
    val finalDriveRatio: Float = 3.909f, // standard on European NBFL 1.8 Sport 6-speed
    val tireWidthMm: Int = 195,
    val tireAspectPct: Int = 50,
    val rimDiameterInches: Int = 15,
    val redlineRpm: Int = 7200,
    val shiftLightRpm: Int = 6800,
    val coolantWarningTempC: Float = 104f,
    val vehicleWeightKg: Int = 1065
) {
    // Calculates tire circumference in meters
    val tireCircumferenceMeters: Float
        get() {
            val tireSidewallMm = tireWidthMm * (tireAspectPct / 100.0f)
            val totalDiameterMm = (rimDiameterInches * 25.4f) + (2 * tireSidewallMm)
            return (Math.PI * totalDiameterMm / 1000.0).toFloat()
        }
}

data class ObdData(
    val rpm: Float = 850f,
    val speedKmh: Float = 0f,
    val coolantTempC: Float = 88f,
    val intakeTempC: Float = 26f,
    val throttlePosPct: Float = 0f,
    val timingAdvanceDeg: Float = 14f,
    val mafFlowRateGps: Float = 3.4f,
    val manifoldPressureKpa: Float = 35f, // vacuum at idle
    val batteryVoltage: Float = 14.1f,
    val fuelTrimShortPct: Float = 0.5f,
    val fuelTrimLongPct: Float = -1.2f,
    val engineLoadPct: Float = 18f,
    val fuelLevelPct: Float = 65f,
    val calculatedHp: Float = 0f,
    val calculatedTorqueNm: Float = 0f,
    val calculatedGear: String = "N",
    val isShiftLightActive: Boolean = false,
    val isVvtActive: Boolean = false,
    val isCoolantWarningActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ObdConnectionState(val statusLabel: String) {
    DISCONNECTED("Disconnesso"),
    SCANNING("Ricerca Adattatore..."),
    CONNECTING("Connessione in corso..."),
    INITIALIZING("Inizializzazione ELM327..."),
    CONNECTED("Connesso"),
    ERROR("Errore di Connessione")
}

enum class ObdConnectionType {
    BLUETOOTH_CLASSIC,
    WIFI,
    SIMULATOR
}

enum class DashboardLayoutType(val displayName: String) {
    CLASSIC_NBFL("Quadranti NBFL"),
    RACE_COCKPIT("Cockpit Pista"),
    MODULAR_GRID("Griglia Modulare"),
    HUD_MIRROR("HUD Parabrezza")
}

enum class DashboardWidgetId(val title: String, val unit: String, val maxVal: Float) {
    RPM("Giri Motore", "RPM", 8000f),
    SPEED("Velocità", "km/h", 240f),
    COOLANT("Temp. Acqua", "°C", 130f),
    INTAKE_TEMP("Temp. Aspirazione", "°C", 90f),
    THROTTLE("Farfalla (TPS)", "%", 100f),
    TIMING_ADVANCE("Anticipo", "°", 45f),
    MAF("Flusso Aria (MAF)", "g/s", 160f),
    MANIFOLD_PRESSURE("Pressione Coll.", "bar", 1.5f),
    BATTERY("Tensione Batteria", "V", 16f),
    FUEL_TRIM("Correzione Miscela", "%", 25f),
    ENGINE_LOAD("Carico Motore", "%", 100f),
    POWER_HP("Potenza Stimata", "CV", 250f),
    TORQUE_NM("Coppia Stimata", "Nm", 280f),
    G_FORCE("Accelerazione G", "G", 1.8f)
}

data class DtcCode(
    val code: String,
    val description: String,
    val system: String,
    val isPending: Boolean = false
)
