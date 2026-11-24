package com.andeva.atelier.mobile.features.cashier.presentation.series

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.TenantFiscalProfile
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType

/** Serie nueva en edición; [initial] es texto para no perder lo que se escribe. */
data class SeriesForm(
    val type: VoucherType = VoucherType.BOLETA,
    val serie: String = "",
    val initial: String = ""
) {
    val serieValid: Boolean
        get() = if (type == VoucherType.NOTA_CREDITO) {
            CashierValidation.isCreditNoteSeries(serie)
        } else {
            CashierValidation.isSeriesFor(type, serie)
        }

    /** Vacío es válido (el backend parte de 0); si hay texto, un entero no negativo. */
    val initialCorrelative: Int? get() = initial.toIntOrNull()?.takeIf { it >= 0 }
    val initialValid: Boolean get() = initial.isEmpty() || initialCorrelative != null

    val canSubmit: Boolean get() = serieValid && initialValid
}

/** Datos fiscales del taller en edición (US42). [taxId] es solo lectura desde el perfil del tenant. */
data class TenantProfileForm(
    val name: String = "",
    val legalName: String = "",
    val taxId: String = ""
) {
    val nameValid: Boolean get() = name.trim().length in 1..100
    val legalNameValid: Boolean get() = legalName.trim().length in 1..150
    val canSubmit: Boolean get() = nameValid && legalNameValid
}

sealed interface SeriesSheet {
    data class Create(val form: SeriesForm) : SeriesSheet
    data class Detail(val series: FiscalSeries) : SeriesSheet
    data class EditTenant(val form: TenantProfileForm) : SeriesSheet
}

/** Perfil fiscal del taller y series SUNAT de la sede (US42). Solo en línea y sin caché. */
data class FiscalSeriesUiState(
    val series: List<FiscalSeries> = emptyList(),
    val tenantProfile: TenantFiscalProfile? = null,
    val hasLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canManage: Boolean = false,
    val canReadTenant: Boolean = false,
    val canUpdateTenant: Boolean = false,
    val sheet: SeriesSheet? = null,
    val isSaving: Boolean = false,
    val sheetError: UiText? = null,
    val notice: UiText? = null
) {
    val hasAccess: Boolean get() = canManage || canReadTenant || canUpdateTenant
    val showSkeleton: Boolean get() = !hasLoaded && series.isEmpty() && tenantProfile == null && error == null
}
