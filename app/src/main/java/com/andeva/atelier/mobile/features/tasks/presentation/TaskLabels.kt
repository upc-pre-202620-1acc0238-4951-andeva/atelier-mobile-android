package com.andeva.atelier.mobile.features.tasks.presentation

import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus

/** Texto del estado; una pausa por repuesto se muestra como suspendida. */
fun taskStatusLabelRes(status: TaskStatus?, holdReason: HoldReason?): Int = when (status) {
    TaskStatus.IN_PROGRESS -> R.string.task_status_in_progress
    TaskStatus.ON_HOLD -> if (holdReason != null && !holdReason.isMechanicPause) R.string.task_status_suspended
    else R.string.task_status_on_hold
    TaskStatus.ASSIGNED -> R.string.task_status_assigned
    TaskStatus.COMPLETED -> R.string.task_status_completed
    TaskStatus.CANCELLED -> R.string.task_status_cancelled
    TaskStatus.PENDING, null -> R.string.task_status_pending
}

fun taskStatusChipKind(status: TaskStatus?, holdReason: HoldReason?): ChipKind = when (status) {
    TaskStatus.IN_PROGRESS -> ChipKind.Info
    TaskStatus.ON_HOLD -> if (holdReason != null && !holdReason.isMechanicPause) ChipKind.Bad else ChipKind.Wait
    TaskStatus.COMPLETED -> ChipKind.Ok
    else -> ChipKind.Idle
}
