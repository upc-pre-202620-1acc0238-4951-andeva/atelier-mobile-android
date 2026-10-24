package com.andeva.atelier.mobile.features.attendance.infrastructure.sync

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceService
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.ClockInRequestDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.repository.AttendanceCache
import com.google.gson.Gson
import java.time.Clock
import javax.inject.Inject

/** `CLOCK_IN`: 409 = ya existe una jornada abierta → aplicada (docs/06-offline-sync.md §3). */
class ClockInHandler @Inject constructor(
    private val service: AttendanceService,
    private val cache: AttendanceCache,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val body = gson.fromJson(operation.payload, ClockInRequestDto::class.java)
        val result = apiCall(gson, { service.clockIn(body) }) { it }
        val outcome = OperationResult.from(result) { it.httpStatus == 409 }
        val membershipId = sessionManager.getSession()?.membershipId
        if (outcome == OperationResult.Applied && membershipId != null) {
            val dto = result.getOrNull()
            // 409 no trae cuerpo: se recarga el día para que la caché no quede vieja
            if (dto != null) cache.save(dto, membershipId) else cache.refreshDay(membershipId, captureDay(body.clockInTime, clock))
        }
        return outcome
    }

    companion object {
        const val TYPE = "CLOCK_IN"
    }
}
