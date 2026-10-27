package com.andeva.atelier.mobile.features.reception.presentation.register

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType

data class RegisterCustomerUiState(
    val kind: CustomerType = CustomerType.INDIVIDUAL,
    val firstName: String = "",
    val lastName: String = "",
    val companyName: String = "",
    val taxId: String = "",
    val email: String = "",
    val phone: String = "",
    val plate: String = "",
    val brand: String = "",
    val model: String = "",
    val engineType: EngineType = EngineType.GASOLINE,
    val vin: String = "",
    val year: String = "",
    val isSaving: Boolean = false,
    /** El cliente ya se creó pero el vehículo falló: al reintentar solo se repite el vehículo. */
    val registeredCustomerId: String? = null,
    val error: UiText? = null,
    val invalidField: String? = null,
    val done: Boolean = false,
    val canRegister: Boolean = false
) {
    val customerLocked: Boolean get() = registeredCustomerId != null
    val canSubmit: Boolean
        get() = canRegister && !isSaving && !done && taxId.isNotBlank() && plate.isNotBlank() &&
            brand.isNotBlank() && model.isNotBlank() &&
            when (kind) {
                CustomerType.INDIVIDUAL -> firstName.isNotBlank() && lastName.isNotBlank()
                CustomerType.COMPANY -> companyName.isNotBlank()
            }
}
