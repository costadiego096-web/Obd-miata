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
    val iatWarningTempC: Float = 50f,
    val vehicleWeightKg: Int = 1065
) {
    val tireCircumferenceMeters: Float
        get() {
            val tireSidewallMm = tireWidthMm * (tireAspectPct / 100.0f)
            val totalDiameterMm = (rimDiameterInches * 25.4f) + (2 * tireSidewallMm)
            return (Math.PI * totalDiameterMm / 1000.0).toFloat()
        }
}

enum class MixtureStatus(val label: String, val description: String) {
    RICH("RICCA (POTENZA)", "Miscela grassa ricca di benzina per massima potenza WOT"),
    STOICHIOMETRIC("STECHIOMETRICA (14.7)", "Rapporto ideale stechiometrico lambda = 1.00"),
    LEAN("MAGRA (CRUISE)", "Miscela magra a basso consumo in crociera"),
    CUTOFF("CUT-OFF (RILASCIO)", "Iniezione carburante interrotta in rilascio")
}

data class ObdData(
    val rpm: Float = 850f,
    val speedKmh: Float = 0f,
    val coolantTempC: Float = 88f,
    val intakeTempC: Float = 26f, // Temperatura Aria Aspirata (IAT PID 010F)
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

    // Sonda Lambda originale Narrowband NBFL (0-1V) e Rapporto Stechiometrico AFR (PID 0114 / 0124)
    val o2Voltage: Float = 0.46f,         // Tensione sonda lambda anteriore (0.05V - 0.95V)
    val lambdaRatio: Float = 1.00f,       // Coefficiente Lambda λ (1.00 = 14.7:1)
    val afr: Float = 14.7f,               // Rapporto Aria / Benzina (Air-Fuel Ratio, es. 14.7 : 1)
    val mixtureStatus: MixtureStatus = MixtureStatus.STOICHIOMETRIC,

    // Status flags
    val isShiftLightActive: Boolean = false,
    val isVvtActive: Boolean = false,
    val isCoolantWarningActive: Boolean = false,
    val isIatHeatSoakWarning: Boolean = false, // Temp aria aspirata troppo calda (>50°C)
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

enum class DashboardLayoutType(val displayName: String, val iconDesc: String) {
    CLASSIC_NBFL("Quadranti NBFL", "Design strumentazione originale con lancette arancioni"),
    RACE_COCKPIT("Cockpit Pista", "Display da gara con contagiri, shift light e G-meter"),
    AIR_FUEL_FOCUS("Carburazione & IAT", "Focus su rapporto stechiometrico AFR e temp. aria"),
    MODULAR_GRID("Griglia Personalizzata", "Layout modulare con widget configurabili a piacere"),
    HUD_MIRROR("HUD Parabrezza", "Proiezione a specchio per parabrezza")
}

enum class WidgetDisplayStyle(val displayName: String) {
    DIGITAL_CARD("Scheda Digitale"),
    ANALOG_DIAL("Quadrante Analogico"),
    ARC_BAR("Barra Curva"),
    SPARKLINE_CHART("Grafico Andamento Live")
}

enum class DashboardWidgetId(val title: String, val unit: String, val maxVal: Float, val minVal: Float = 0f) {
    AFR_RATIO("Rapp. Stechiometrico (AFR)", ":1", 18.0f, 10.0f),
    INTAKE_TEMP("Temp. Aria Aspirata (IAT)", "°C", 90f, -20f),
    LAMBDA_O2("Tensione Lambda O2", "V", 1.0f, 0f),
    COOLANT("Temp. Liquido (ECT)", "°C", 130f, 0f),
    RPM("Giri Motore", "RPM", 8000f, 0f),
    SPEED("Velocità", "km/h", 240f, 0f),
    THROTTLE("Farfalla (TPS)", "%", 100f, 0f),
    TIMING_ADVANCE("Anticipo Accensione", "°", 45f, 0f),
    MAF("Flusso Aria (MAF)", "g/s", 160f, 0f),
    MANIFOLD_PRESSURE("Pressione Coll.", "bar", 1.5f, 0f),
    BATTERY("Tensione Batteria", "V", 16f, 10f),
    FUEL_TRIM("Correzione Miscela", "%", 25f, -25f),
    ENGINE_LOAD("Carico Motore", "%", 100f, 0f),
    POWER_HP("Potenza Stimata", "CV", 250f, 0f),
    TORQUE_NM("Coppia Stimata", "Nm", 280f, 0f),
    G_FORCE("Accelerazione G", "G", 1.8f, 0f)
}

data class CustomWidgetSlot(
    val slotIndex: Int,
    val widgetId: DashboardWidgetId,
    val displayStyle: WidgetDisplayStyle = WidgetDisplayStyle.DIGITAL_CARD
)

data class DtcCode(
    val code: String,
    val description: String,
    val system: String,
    val isPending: Boolean = false
)
