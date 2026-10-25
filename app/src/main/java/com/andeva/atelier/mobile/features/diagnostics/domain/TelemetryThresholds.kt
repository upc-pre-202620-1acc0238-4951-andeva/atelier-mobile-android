package com.andeva.atelier.mobile.features.diagnostics.domain

/**
 * Umbrales de precaución de US18 E1. La API no trae umbrales por fabricante, así que son valores generales de la app.
 */
object TelemetryThresholds {
    const val MAX_COOLANT_CELSIUS = 105.0
    const val MIN_BATTERY_VOLTS = 12.0
    const val MAX_BATTERY_VOLTS = 15.0
    const val MAX_ENGINE_RPM = 6000

    fun warningsFor(sample: TelemetrySample): Set<TelemetryWarning> = buildSet {
        sample.engineTempCelsius?.let { if (it > MAX_COOLANT_CELSIUS) add(TelemetryWarning.COOLANT_OVERHEAT) }
        sample.batteryVoltage?.let {
            if (it < MIN_BATTERY_VOLTS) add(TelemetryWarning.LOW_VOLTAGE)
            if (it > MAX_BATTERY_VOLTS) add(TelemetryWarning.HIGH_VOLTAGE)
        }
        sample.engineRpm?.let { if (it > MAX_ENGINE_RPM) add(TelemetryWarning.HIGH_RPM) }
    }
}
