package com.andeva.atelier.mobile.features.diagnostics.domain

import java.time.Instant

/**
 * Una muestra en vivo del modo 01 (US18). Solo se guarda y se envía si trae [engineTempCelsius], porque es obligatorio
 * en `TelemetryReadingItemDto` (docs/api/iot.md). [intakePressureKpa] solo se muestra: el lote no tiene ese campo.
 */
data class TelemetrySample(
    val timestamp: Instant,
    val engineTempCelsius: Double?,
    val engineRpm: Int?,
    val speedKmh: Int?,
    val batteryVoltage: Double?,
    val fuelPercentage: Double?,
    val intakePressureKpa: Int?
) {
    val canBeSent: Boolean get() = engineTempCelsius != null
}

/** Lectura que rebasa un umbral de [TelemetryThresholds] (US18 E1). */
enum class TelemetryWarning {
    COOLANT_OVERHEAT,
    LOW_VOLTAGE,
    HIGH_VOLTAGE,
    HIGH_RPM
}

/** Estado de la sesión en vivo: enlace, vehículo, última muestra y sus avisos. */
data class LiveTelemetry(
    val state: ObdLinkState = ObdLinkState.DISCONNECTED,
    val vehicleId: String? = null,
    val sample: TelemetrySample? = null,
    val warnings: Set<TelemetryWarning> = emptySet()
)

/**
 * Muestra congelada (US18 E2). Ya está guardada en `telemetry_readings` y viaja en el lote; no hay endpoint para
 * adjuntarla a la orden (hueco de docs/03-traceability.md).
 */
data class TelemetrySnapshot(
    val vehicleId: String,
    val sample: TelemetrySample,
    val warnings: Set<TelemetryWarning>
)

/** Lo capturado que aún no llega al backend (US19 E1). */
data class DiagnosticsSyncState(
    val pendingReadings: Int,
    val pendingFaults: Int
) {
    val isSynced: Boolean get() = pendingReadings == 0 && pendingFaults == 0
}
