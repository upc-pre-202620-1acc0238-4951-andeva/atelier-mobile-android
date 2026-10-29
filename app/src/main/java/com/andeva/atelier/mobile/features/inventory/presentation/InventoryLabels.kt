package com.andeva.atelier.mobile.features.inventory.presentation

import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part

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

/** Semáforo de existencias (US31 E2): agotado en rojo, en el mínimo en ámbar y con saldo en verde. */
fun Part.stockLabelRes(): Int = when {
    isOutOfStock -> R.string.stock_out
    isLowStock -> R.string.stock_low
    else -> R.string.stock_ok
}

fun Part.stockChipKind(): ChipKind = when {
    isOutOfStock -> ChipKind.Bad
    isLowStock -> ChipKind.Wait
    else -> ChipKind.Ok
}
