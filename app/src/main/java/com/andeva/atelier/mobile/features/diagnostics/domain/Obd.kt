package com.andeva.atelier.mobile.features.diagnostics.domain

/** Adaptador ELM327 emparejado en los ajustes de Bluetooth de Android (US16). */
data class ObdAdapter(
    val name: String,
    val address: String
)

/** Enlace establecido con la computadora del vehículo (US16 E1): protocolo OBD-II y número de chasis si responde. */
data class ObdConnectionInfo(
    val protocol: String?,
    val vin: String?
)

/** Estado del enlace durante la sesión de diagnóstico. */
enum class ObdLinkState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STREAMING,
    RECONNECTING,

    /** Se cortó la señal y no se pudo recuperar (US16 E2): las lecturas ya guardadas no se pierden. */
    LOST
}

/** Cuadro congelado (modo 02) del código que encendió el testigo (US17 E1). Solo se muestra: la API no lo recibe. */
data class FreezeFrame(
    val dtc: Dtc?,
    val engineTempCelsius: Double?,
    val engineRpm: Int?,
    val speedKmh: Int?
)
