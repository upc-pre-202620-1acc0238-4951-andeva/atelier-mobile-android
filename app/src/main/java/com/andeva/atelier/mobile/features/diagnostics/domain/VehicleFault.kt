package com.andeva.atelier.mobile.features.diagnostics.domain

import java.time.Instant

/**
 * Falla electrónica de un vehículo (`VehicleFaultResponse`). Mientras el `REGISTER_FAULT` está en cola el id es
 * `local-{uuid}` ([isPending]); al sincronizarse se reemplaza por el del backend (docs/06-offline-sync.md §6).
 */
data class VehicleFault(
    val id: String,
    val vehicleId: String,
    val dtc: Dtc?,
    val dtcCode: String,
    val severity: FaultSeverity?,
    val description: String,
    val detectedAt: Instant?,
    val isResolved: Boolean,
    val resolvedAt: Instant?
) {
    val isPending: Boolean get() = isLocalId(id)

    companion object {
        const val LOCAL_PREFIX = "local-"

        fun isLocalId(id: String) = id.startsWith(LOCAL_PREFIX)
    }
}

/** Resultado del escaneo de US17: vacío = sin códigos (E2). */
data class FaultScanResult(
    val faults: List<VehicleFault>,
    val freezeFrame: FreezeFrame?
) {
    val isClean: Boolean get() = faults.isEmpty()
}

/**
 * Resultado del borrado de US20: [cleared] ya no aparecen tras la re-lectura (E1); [persistent] reaparecen y
 * requieren revisar el circuito (E2).
 */
data class ClearFaultsResult(
    val cleared: List<VehicleFault>,
    val persistent: List<VehicleFault>
) {
    val isClean: Boolean get() = persistent.isEmpty()
}
