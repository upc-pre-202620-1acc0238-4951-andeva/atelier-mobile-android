package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceValidation
import com.andeva.atelier.mobile.features.attendance.domain.InvalidJustificationException
import com.andeva.atelier.mobile.features.attendance.domain.SubmitOutcome
import javax.inject.Inject

/** Enviar descargo de una tardanza o ausencia propia (US10 E1). El dictamen llega por push `JUSTIFICATION_REVIEWED`. */
class SubmitJustificationUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository
) {
    suspend operator fun invoke(id: AttendanceId, reason: String): Result<SubmitOutcome> {
        if (!AttendanceValidation.isJustification(reason)) return Result.failure(InvalidJustificationException())
        return attendanceRepository.submitJustification(id, reason)
    }
}
