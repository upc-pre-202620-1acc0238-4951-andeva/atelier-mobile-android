package com.andeva.atelier.mobile.features.workorders.presentation

import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus

fun WorkOrderStatus?.labelRes(): Int = when (this) {
    WorkOrderStatus.DRAFT -> R.string.order_status_draft
    WorkOrderStatus.IN_PROGRESS -> R.string.order_status_in_progress
    WorkOrderStatus.COMPLETED -> R.string.order_status_completed
    WorkOrderStatus.PAID -> R.string.order_status_paid
    WorkOrderStatus.CANCELED -> R.string.order_status_canceled
    null -> R.string.order_status_unknown
}

fun WorkOrderStatus?.chipKind(): ChipKind = when (this) {
    WorkOrderStatus.IN_PROGRESS -> ChipKind.Info
    WorkOrderStatus.COMPLETED, WorkOrderStatus.PAID -> ChipKind.Ok
    WorkOrderStatus.CANCELED -> ChipKind.Bad
    WorkOrderStatus.DRAFT, null -> ChipKind.Idle
}
