package com.example.data

import android.content.Context
import com.example.model.Mx5EngineType
import com.example.model.Mx5Transmission
import com.example.model.VehicleConfig

/**
 * Salva e ricarica il profilo veicolo (motore, cambio, rapporto finale, soglie...)
 * in modo che le impostazioni restino dopo la chiusura dell'app.
 */
object VehicleConfigStore {
    private const val PREFS = "vehicle_config"

    fun load(context: Context): VehicleConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = VehicleConfig()
        return VehicleConfig(
            modelName = d.modelName,
            engineType = enumOr(prefs.getString("engine", null), d.engineType),
            transmission = enumOr(prefs.getString("transmission", null), d.transmission),
            finalDriveRatio = prefs.getFloat("final_drive", d.finalDriveRatio),
            tireWidthMm = prefs.getInt("tire_width", d.tireWidthMm),
            tireAspectPct = prefs.getInt("tire_aspect", d.tireAspectPct),
            rimDiameterInches = prefs.getInt("rim_diameter", d.rimDiameterInches),
            redlineRpm = prefs.getInt("redline", d.redlineRpm),
            shiftLightRpm = prefs.getInt("shift_light", d.shiftLightRpm),
            coolantWarningTempC = prefs.getFloat("coolant_warning", d.coolantWarningTempC),
            iatWarningTempC = prefs.getFloat("iat_warning", d.iatWarningTempC),
            vehicleWeightKg = prefs.getInt("weight", d.vehicleWeightKg)
        )
    }

    fun save(context: Context, config: VehicleConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("engine", config.engineType.name)
            .putString("transmission", config.transmission.name)
            .putFloat("final_drive", config.finalDriveRatio)
            .putInt("tire_width", config.tireWidthMm)
            .putInt("tire_aspect", config.tireAspectPct)
            .putInt("rim_diameter", config.rimDiameterInches)
            .putInt("redline", config.redlineRpm)
            .putInt("shift_light", config.shiftLightRpm)
            .putFloat("coolant_warning", config.coolantWarningTempC)
            .putFloat("iat_warning", config.iatWarningTempC)
            .putInt("weight", config.vehicleWeightKg)
            .apply()
    }

    // Se il valore salvato non esiste piu' (es. enum rinominato) si usa il predefinito
    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
