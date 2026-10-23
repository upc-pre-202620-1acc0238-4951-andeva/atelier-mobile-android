package com.andeva.atelier.mobile.core.data.sync

import com.andeva.atelier.mobile.core.domain.ApiException
import java.io.IOException

/**
 * Envía un tipo de operación de la cola (`CLOCK_IN`, `TELEMETRY_BATCH`…). Cada feature registra el suyo:
 *
 * ```
 * @Binds @IntoMap @StringKey("CLOCK_IN")
 * fun bindClockIn(handler: ClockInHandler): OperationHandler
 * ```
 *
 * El worker no conoce endpoints de features. Si el envío tiene éxito, el handler refresca la caché afectada.
 */
interface OperationHandler {
    suspend fun send(operation: PendingOperationEntity): OperationResult
}

sealed interface OperationResult {
    /** 2xx o "ya aplicada" según la tabla de tipos de docs/06-offline-sync.md §3: se borra. */
    data object Applied : OperationResult

    /** 5xx, timeout o sin red: vuelve a PENDING con backoff. */
    data class Retry(val httpStatus: Int? = null, val code: String? = null, val message: String? = null) : OperationResult

    /** Regla de negocio (otro 4xx): FAILED con el `message` del backend, no se reintenta solo. */
    data class Failed(val httpStatus: Int?, val code: String?, val message: String?) : OperationResult

    /** 401 tras intentar renovar: la sesión terminó. Queda PENDING y el worker se detiene. */
    data object Unauthorized : OperationResult

    companion object {
        /**
         * Clasifica el resultado de una llamada (`apiCall` / `apiCallUnit`).
         * [isAlreadyApplied] recibe los errores que el tipo considera "ya aplicada" (por ejemplo 409 en CLOCK_IN).
         */
        fun from(result: Result<*>, isAlreadyApplied: (ApiException) -> Boolean = { false }): OperationResult {
            val error = result.exceptionOrNull() ?: return Applied
            return when {
                error is ApiException && isAlreadyApplied(error) -> Applied
                error is ApiException && error.httpStatus == 401 -> Unauthorized
                error is ApiException && (error.httpStatus >= 500 || error.httpStatus == 408 || error.httpStatus == 429) ->
                    Retry(error.httpStatus, error.code, error.message)
                error is ApiException -> Failed(error.httpStatus, error.code, error.message)
                error is IOException -> Retry(message = error.message)
                else -> Retry(message = error.message)
            }
        }
    }
}
