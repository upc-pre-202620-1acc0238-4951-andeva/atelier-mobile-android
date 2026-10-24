package com.andeva.atelier.mobile.features.tasks.domain

import java.math.BigDecimal

/** Restricciones de los requests de docs/api/operations.md. Solo evitan una llamada inútil; el backend vuelve a validar. */
object TaskValidation {
    const val NOTES_MAX = 1000
    const val MISSING_ITEM_MAX = 500
    const val EVIDENCE_DESCRIPTION_MAX = 500

    fun isLaborHours(value: BigDecimal) = value >= BigDecimal.ZERO

    fun isNotes(value: String?) = value == null || value.trim().length <= NOTES_MAX

    fun isMissingItem(value: String) = value.isNotBlank() && value.trim().length <= MISSING_ITEM_MAX

    fun isEvidenceDescription(value: String) = value.length <= EVIDENCE_DESCRIPTION_MAX

    fun isQuantity(value: BigDecimal) = value > BigDecimal.ZERO
}
