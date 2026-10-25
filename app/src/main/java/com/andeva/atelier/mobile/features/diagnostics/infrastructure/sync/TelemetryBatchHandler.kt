package com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync

import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.IotService
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.TelemetryBatchRequestDto
import com.google.gson.Gson
import javax.inject.Inject

/**
 * `TELEMETRY_BATCH` (docs/06-offline-sync.md §3): reintentar es seguro porque el backend ignora las lecturas repetidas.
 * Un 404 (el vehículo no tiene escáner instalado) queda FAILED con el `message`. Las lecturas se borran a los 7 días.
 */
class TelemetryBatchHandler @Inject constructor(
    private val service: IotService,
    private val gson: Gson
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val body = gson.fromJson(operation.payload, TelemetryBatchRequestDto::class.java)
        return OperationResult.from(apiCall(gson, { service.ingestTelemetry(body) }) { it })
    }

    companion object {
        const val TYPE = "TELEMETRY_BATCH"
    }
}
