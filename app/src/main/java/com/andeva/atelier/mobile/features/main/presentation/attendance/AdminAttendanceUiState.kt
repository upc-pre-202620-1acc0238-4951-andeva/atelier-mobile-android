package com.andeva.atelier.mobile.features.main.presentation.attendance

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.BranchAttendanceItem
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import java.time.LocalDate

/** Quien también marca su propia asistencia ve dos pestañas; quien solo supervisa, una lista. */
enum class TeamTab { TEAM, MINE }

/**
 * "Equipo": asistencia de la sede hoy y descargos por revisar de los últimos 7 días. Ambas salen de endpoints en
 * línea (sin caché en Room), así que el esqueleto se muestra hasta la primera respuesta.
 */
data class AdminAttendanceUiState(
    val today: LocalDate = LocalDate.MIN,
    val tab: TeamTab = TeamTab.TEAM,
    val branchName: String? = null,
    val records: List<BranchAttendanceItem> = emptyList(),
    val justifications: List<TeamAttendance> = emptyList(),
    /** Descargos ya resueltos (id → aprobado) que aún muestran su resultado antes de retirarse de la lista. */
    val resolved: Map<String, Boolean> = emptyMap(),
    /** Días de descargos cuya consulta falló (H32): el resultado es parcial y se avisa. */
    val failedJustificationDays: Int = 0,
    val reviewing: TeamAttendance? = null,
    val isSubmittingReview: Boolean = false,
    val reviewError: UiText? = null,
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canAuditAll: Boolean = false,
    val canApproveJustifications: Boolean = false,
    val canClockIn: Boolean = false
) {
    val onShiftCount: Int get() = records.count { it.attendance.isOpen && it.attendance.status != AttendanceStatus.ABSENT }
    val onTimeCount: Int get() = records.count { it.attendance.status == AttendanceStatus.ON_TIME }
    val lateCount: Int get() = records.count { it.attendance.status == AttendanceStatus.LATE }
    val absentCount: Int get() = records.count { it.attendance.status == AttendanceStatus.ABSENT }
    val showSkeleton: Boolean get() = !hasLoaded && records.isEmpty()

    /** Hay pestañas solo cuando la misma cuenta supervisa y marca. */
    val hasTabs: Boolean get() = canAuditAll && canClockIn

    /** El nombre del colaborador de un descargo, si la asistencia del día lo trae (H36); si no, se usa su cargo. */
    fun labelFor(item: TeamAttendance): String? =
        records.firstOrNull { it.membershipId != null && it.membershipId == item.membershipId }?.collaboratorLabel
}
