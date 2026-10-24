package com.andeva.atelier.mobile.features.workorders.domain

import java.math.BigDecimal

/** Restricciones de los requests de docs/api/operations.md. Solo evitan una llamada inútil; el backend vuelve a validar. */
object WorkOrderValidation {
    const val DIAGNOSTIC_MAX = 2000
    const val TASK_DESCRIPTION_MAX = 1000
    const val PROPOSAL_DESCRIPTION_MAX = 2000
    const val REASON_MAX = 1000
    const val NOTES_MAX = 1000
    const val PHOTO_DESCRIPTION_MAX = 500

    fun isMileage(value: Int) = value >= 0

    fun isOptionalText(value: String?, max: Int) = value == null || value.trim().length <= max

    fun isRequiredText(value: String, max: Int) = value.isNotBlank() && value.trim().length <= max

    fun isOptionalAmount(value: BigDecimal?) = value == null || value >= BigDecimal.ZERO

    fun isAmount(value: BigDecimal) = value >= BigDecimal.ZERO
}

/** Un dato no cumple [WorkOrderValidation]; [field] dice cuál (`mileageIn`, `photo`, `severity`…). */
class InvalidWorkOrderInputException(val field: String) : Exception("Invalid work order input: $field")
