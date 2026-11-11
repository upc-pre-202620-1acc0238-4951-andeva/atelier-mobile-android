package com.andeva.atelier.mobile.features.main.presentation.attendance

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.BranchAttendanceItem
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus

enum class AdminAttendanceTab { BRANCH_DAILY, JUSTIFICATIONS, MY_ATTENDANCE }

data class AdminAttendanceUiState(
    val tab: AdminAttendanceTab = AdminAttendanceTab.BRANCH_DAILY,
    val branchRecords: List<BranchAttendanceItem> = emptyList(),
    val isLoading: Boolean = true,
    val reviewingId: AttendanceId? = null,
    val canAuditAll: Boolean = false,
    val canApproveJustifications: Boolean = false,
    val error: UiText? = null,
    val notice: UiText? = null
) {
    val pendingJustifications: List<BranchAttendanceItem>
        get() = branchRecords.filter { it.attendance.justificationStatus == JustificationStatus.PENDING }
}
