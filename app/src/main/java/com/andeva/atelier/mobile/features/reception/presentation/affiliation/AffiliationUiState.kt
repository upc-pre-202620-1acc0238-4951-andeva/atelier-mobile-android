package com.andeva.atelier.mobile.features.reception.presentation.affiliation

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.reception.domain.AffiliationLookup
import com.andeva.atelier.mobile.features.reception.domain.AffiliationStep
import com.andeva.atelier.mobile.features.reception.domain.Customer

data class AffiliationUiState(
    val email: String = "",
    val taxId: String = "",
    val phone: String = "",
    val isSearching: Boolean = false,
    val isAffiliating: Boolean = false,
    val lookup: AffiliationLookup? = null,
    /** Cliente ya afiliado en esta pantalla (US44 E1). */
    val affiliated: Customer? = null,
    val error: UiText? = null,
    /** Campo que no pasó la validación local (`email`, `taxId`, `phone`). */
    val invalidField: String? = null,
    val canLookup: Boolean = false,
    val canAffiliate: Boolean = false
) {
    val isBusy: Boolean get() = isSearching || isAffiliating
    val step: AffiliationStep? get() = lookup?.nextStep
    val canSearch: Boolean get() = canLookup && email.isNotBlank() && !isBusy
    val canSubmitAffiliation: Boolean
        get() = canAffiliate && step == AffiliationStep.AFFILIATE && affiliated == null && taxId.isNotBlank() && !isBusy
}
