package com.andeva.atelier.mobile.features.attendance.infrastructure.sync

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceService
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.ClockOutRequestDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.repository.AttendanceCache
import com.google.gson.Gson
import java.io.IOException
import java.time.Clock
import javax.inject.Inject

/**
 * `CLOCK_OUT`. 400 ("salida ya registrada", pero también reloj desajustado) y 404 ("sin jornada
 * abierta", pero también salida sin ingreso, US08 E3) son ambiguos: se verifican con `history` del
 * día (docs/06-offline-sync.md §3). Si la jornada ya tiene salida → aplicada; si no → FAILED con el
 * `message` del backend.
 */
class ClockOutHandler @Inject constructor(
    private val service: AttendanceService,
    private val cache: AttendanceCache,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val body = gson.fromJson(operation.payload, ClockOutRequestDto::class.java)
        val result = apiCall(gson, { service.clockOut(body) }) { it }
        val membershipId = sessionManager.getSession()?.membershipId
            ?: return OperationResult.from(result)

        result.onSuccess {
            cache.save(it, membershipId)
            return OperationResult.Applied
        }
        val error = result.exceptionOrNull()
        if (error !is ApiException || error.httpStatus !in AMBIGUOUS) return OperationResult.from(result)

        val day = cache.refreshDay(membershipId, captureDay(body.clockOutTime, clock))
        val rows = day.getOrElse { verifyError ->
            // No se pudo verificar sin red: se reintenta después en vez de darla por fallida
            return if (verifyError is IOException) OperationResult.Retry(message = verifyError.message) else OperationResult.from(result)
        }
        val closed = rows.any { it.clockOut != null && (body.attendanceId == null || it.id == body.attendanceId) }
        return if (closed) OperationResult.Applied else OperationResult.from(result)
    }

    companion object {
        const val TYPE = "CLOCK_OUT"
        private val AMBIGUOUS = setOf(400, 404)
    }
}
