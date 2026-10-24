package com.andeva.atelier.mobile.features.attendance.infrastructure.sync

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.OperationResult
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.AttendanceDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceService
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.JustificationPayload
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.JustifyAttendanceRequestDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.repository.AttendanceCache
import com.google.gson.Gson
import java.time.Instant
import javax.inject.Inject

/** `SUBMIT_JUSTIFICATION`: 422 "ya justificada" → aplicada (docs/06-offline-sync.md §3). */
class JustificationHandler @Inject constructor(
    private val service: AttendanceService,
    private val cache: AttendanceCache,
    private val dao: AttendanceDao,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : OperationHandler {

    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val payload = gson.fromJson(operation.payload, JustificationPayload::class.java)
        val result = apiCall(gson, { service.submitJustification(payload.attendanceId, JustifyAttendanceRequestDto(payload.reason)) }) { it }
        val outcome = OperationResult.from(result) { it.httpStatus == 422 }
        val membershipId = sessionManager.getSession()?.membershipId
        if (outcome == OperationResult.Applied && membershipId != null) {
            val dto = result.getOrNull()
            if (dto != null) {
                cache.save(dto, membershipId)
            } else {
                // 422 no trae cuerpo: se recarga el día de esa jornada
                dao.findById(payload.attendanceId)?.let { cache.refreshDay(membershipId, BusinessTime.limaDate(Instant.ofEpochMilli(it.clockIn))) }
            }
        }
        return outcome
    }

    companion object {
        const val TYPE = "SUBMIT_JUSTIFICATION"
    }
}
