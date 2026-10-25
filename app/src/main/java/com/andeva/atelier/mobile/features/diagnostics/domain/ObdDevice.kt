package com.andeva.atelier.mobile.features.diagnostics.domain

import java.time.Instant

enum class DeviceStatus { ACTIVE, INACTIVE, LOST, BROKEN }

/** Escáner OBD-II registrado en el taller (`Obd2DeviceResponse`). */
data class ObdDevice(
    val id: String,
    val deviceIdentifier: String,
    val connectionType: String?,
    val status: DeviceStatus?,
    val hardwareModel: String?,
    val firmwareVersion: String?
) {
    /**
     * Sugerencia para emparejar el adaptador Bluetooth con el dispositivo del backend: no hay una regla documentada
     * que diga que `deviceIdentifier` sea la MAC (hueco de docs/03-traceability.md), así que la UI deja elegir.
     */
    fun matches(address: String) = normalize(deviceIdentifier) == normalize(address)

    private fun normalize(value: String) = value.uppercase().filter { it.isLetterOrDigit() }
}

/** Instalación del escáner en un vehículo (`DeviceInstallationResponse`). El lote de telemetría la exige activa. */
data class DeviceInstallation(
    val id: String,
    val deviceId: String,
    val vehicleId: String,
    val installedAt: Instant?,
    val uninstalledAt: Instant?,
    val initialOdometerKm: Int?,
    val finalOdometerKm: Int?,
    val isActive: Boolean
)
