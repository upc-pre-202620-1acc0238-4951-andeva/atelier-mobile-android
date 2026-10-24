package com.andeva.atelier.mobile.features.diagnostics.domain

/**
 * Canal con el adaptador ELM327 (docs/04-mobile-integration.md §4). La implementación vive en infraestructura, como
 * `LocationProvider`. Fallos: [BluetoothPermissionDeniedException], [BluetoothDisabledException],
 * [ObdAdapterUnreachableException], [ObdNoResponseException] y [ObdConnectionLostException].
 */
interface ObdLink {
    val isConnected: Boolean

    /** Abre el canal, inicializa el ELM327 y comprueba que la ECU responda; devuelve protocolo y VIN (US16 E1). */
    suspend fun connect(address: String): Result<ObdConnectionInfo>

    /** Vuelve a abrir el último adaptador tras una caída de señal. */
    suspend fun reconnect(): Result<ObdConnectionInfo>

    /** Modo 03: códigos almacenados (vacío = sin averías). */
    suspend fun readDtcs(): Result<List<Dtc>>

    /** Modo 02: cuadro congelado del código que encendió el testigo; null si la ECU no guardó ninguno. */
    suspend fun readFreezeFrame(): Result<FreezeFrame?>

    /** Modo 04: borra los códigos y apaga el testigo. */
    suspend fun clearDtcs(): Result<Unit>

    /** Una muestra del modo 01 y el voltaje del adaptador. */
    suspend fun readSample(): Result<TelemetrySample>

    fun close()
}

/** Adaptadores emparejados en los ajustes de Android (no se escanea: no hace falta `BLUETOOTH_SCAN`). */
interface ObdAdapterProvider {
    fun pairedAdapters(): Result<List<ObdAdapter>>
}
