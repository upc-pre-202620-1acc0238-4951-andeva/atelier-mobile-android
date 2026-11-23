package com.andeva.atelier.mobile.features.inventory.presentation

import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus

fun ItemCategory.labelRes(): Int = when (this) {
    ItemCategory.LUBRICANTS -> R.string.category_lubricants
    ItemCategory.BRAKES -> R.string.category_brakes
    ItemCategory.SUSPENSION -> R.string.category_suspension
    ItemCategory.ENGINE -> R.string.category_engine
    ItemCategory.ELECTRICAL -> R.string.category_electrical
    ItemCategory.TIRES -> R.string.category_tires
    ItemCategory.FILTERS -> R.string.category_filters
    ItemCategory.BODYWORK -> R.string.category_bodywork
    ItemCategory.ACCESSORIES -> R.string.category_accessories
}

/** Semáforo de existencias (US31 E2): agotado en rojo, en el mínimo en aviso y con saldo en verde. */
fun Part.stockLabelRes(): Int = when {
    isOutOfStock -> R.string.stock_out
    isLowStock -> R.string.stock_low
    else -> R.string.stock_ok
}

fun Part.stockChipKind(): ChipKind = when {
    isOutOfStock -> ChipKind.Bad
    isLowStock -> ChipKind.Warn
    else -> ChipKind.Ok
}

fun PurchaseOrderStatus?.labelRes(): Int = when (this) {
    PurchaseOrderStatus.DRAFT -> R.string.inventory_po_status_draft
    PurchaseOrderStatus.ISSUED -> R.string.inventory_po_status_issued
    PurchaseOrderStatus.RECEIVED -> R.string.inventory_po_status_received
    PurchaseOrderStatus.CANCELED -> R.string.inventory_po_status_canceled
    null -> R.string.inventory_po_status_unknown
}

fun PurchaseOrderStatus?.chipKind(): ChipKind = when (this) {
    PurchaseOrderStatus.ISSUED -> ChipKind.Info
    PurchaseOrderStatus.RECEIVED -> ChipKind.Ok
    PurchaseOrderStatus.CANCELED -> ChipKind.Bad
    else -> ChipKind.Idle
}
