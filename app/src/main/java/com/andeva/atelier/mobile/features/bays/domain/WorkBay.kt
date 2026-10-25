package com.andeva.atelier.mobile.features.bays.domain

/** `WorkBayStatus` de docs/api/operations.md. */
enum class BayStatus { AVAILABLE, OCCUPIED, MAINTENANCE }

/** Los 8 tipos de `CreateWorkBayResource` (docs/api/operations.md); README §8 lista solo 4. */
enum class BayType { LIFT, PAINT_BOOTH, WASHING, ALIGNMENT, MECHANICAL_LIFT, WASH_BAY, DIAGNOSTIC_PIT, ALIGNMENT_STATION }

/** Bahía de trabajo de la sede (`WorkBayResource`). */
data class WorkBay(
    val id: String,
    val branchId: String,
    val name: String,
    val type: BayType?,
    val status: BayStatus?,
    val currentWorkOrderId: String?
) {
    /** Solo una bahía libre puede recibir una orden. */
    val canReceive: Boolean get() = status == BayStatus.AVAILABLE
}
