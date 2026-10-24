package com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync

import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.features.diagnostics.domain.Dtc
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.IotService
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.ResolveFaultPayload
import com.google.gson.Gson
import java.time.Clock
import javax.inject.Inject

/**
 * `RESOLVE_FAULT` (docs/06-offline-sync.md §3): resolver dos veces responde 200. Si la falla se borró mientras su
 * `REGISTER_FAULT` se enviaba, el payload trae el id `local-…` y la operación depende de ese registro: el id real se
 * busca entre las fallas activas del vehículo; si ya no está activa, no hay nada que resolver.
 */
class ResolveFaultHandler @Inject constructor(
    private val service: IotService,
    private val faultDao: VehicleFaultDao,
    private val gson: Gson,
    private val clock: Clock
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val payload = gson.fromJson(operation.payload, ResolveFaultPayload::class.java)
        val faultId = if (VehicleFault.isLocalId(payload.faultId)) {
            val active = apiCall(gson, { service.getActiveFaults(payload.vehicleId) }) { it }
            if (active.isFailure) return OperationResult.from(active)
            active.getOrThrow().firstOrNull { Dtc.normalize(it.dtcCode.orEmpty()) == payload.dtcCode }?.id
                ?: return OperationResult.Applied
        } else {
            payload.faultId
        }

        val result = apiCall(gson, { service.resolveFault(faultId) }) { it }
        val outcome = OperationResult.from(result)
        val dto = result.getOrNull()
        if (outcome == OperationResult.Applied && dto != null) {
            faultDao.upsert(dto.toEntity(payload.vehicleId, clock.millis()))
        }
        return outcome
    }

    companion object {
        const val TYPE = "RESOLVE_FAULT"
    }
}
