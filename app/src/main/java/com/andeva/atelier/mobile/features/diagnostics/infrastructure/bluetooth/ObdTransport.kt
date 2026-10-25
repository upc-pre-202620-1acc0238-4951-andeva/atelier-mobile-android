package com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth

/**
 * Canal serie con el ELM327: envía un comando y devuelve el texto hasta el prompt `>`. Separado de
 * [Elm327ObdLink] para probar el protocolo sin Bluetooth.
 */
interface ObdTransport {
    val isOpen: Boolean

    /** Falla con las excepciones de dominio de Bluetooth o con `ObdAdapterUnreachableException`. */
    suspend fun open(address: String)

    /** Sin respuesta en [timeoutMillis] o socket cerrado → `IOException`. */
    suspend fun command(command: String, timeoutMillis: Long): String

    fun close()
}
