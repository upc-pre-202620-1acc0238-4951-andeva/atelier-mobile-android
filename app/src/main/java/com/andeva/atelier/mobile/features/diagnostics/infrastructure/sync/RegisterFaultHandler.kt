package com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync

import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.IotService
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.RegisterFaultRequestDto
import com.google.gson.Gson
import java.time.Clock
import javax.inject.Inject

/**
 * `REGISTER_FAULT` (docs/06-offline-sync.md §3 y §6): si el vehículo ya tiene ese DTC activo el backend devuelve esa
 * falla (R3). Al aplicarse, la fila `local-{id de la operación}` pasa a tener el id real; si se borró en el auto
 * mientras se enviaba, conserva la marca de resuelta para que su `RESOLVE_FAULT` la cierre.
 */
class RegisterFaultHandler @Inject constructor(
    private val service: IotService,
    private val faultDao: VehicleFaultDao,
    private val gson: Gson,
    private val clock: Clock
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val body = gson.fromJson(operation.payload, RegisterFaultRequestDto::class.java)
        val result = apiCall(gson, { service.registerFault(body) }) { it }
        val outcome = OperationResult.from(result)
        val dto = result.getOrNull()
        if (outcome == OperationResult.Applied && dto != null) {
            val localId = VehicleFault.LOCAL_PREFIX + operation.id
            val local = faultDao.findById(localId)
            val saved = dto.toEntity(body.vehicleId, clock.millis())
            val entity = if (local?.isResolved == true) saved.copy(isResolved = true, resolvedAt = local.resolvedAt) else saved
            faultDao.replaceLocal(localId, entity)
        }
        return outcome
    }

    companion object {
        const val TYPE = "REGISTER_FAULT"
    }
}
