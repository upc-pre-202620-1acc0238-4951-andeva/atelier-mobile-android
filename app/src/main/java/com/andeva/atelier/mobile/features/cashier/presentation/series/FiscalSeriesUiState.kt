package com.andeva.atelier.mobile.features.cashier.presentation.series

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.cashier.domain.CashierValidation
import com.andeva.atelier.mobile.features.cashier.domain.FiscalSeries
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType

/** Serie nueva en edición; [initial] es texto para no perder lo que se escribe. */
data class SeriesForm(
    val type: VoucherType = VoucherType.BOLETA,
    val serie: String = "",
    val initial: String = ""
) {
    val serieValid: Boolean get() = CashierValidation.isSeriesFor(type, serie)

    /** Vacío es válido (el backend parte de 0); si hay texto, un entero no negativo. */
    val initialCorrelative: Int? get() = initial.toIntOrNull()?.takeIf { it >= 0 }
    val initialValid: Boolean get() = initial.isEmpty() || initialCorrelative != null

    val canSubmit: Boolean get() = serieValid && initialValid
}

sealed interface SeriesSheet {
    data class Create(val form: SeriesForm) : SeriesSheet
    data class Detail(val series: FiscalSeries) : SeriesSheet
}

/** Series SUNAT de la sede. Solo en línea y sin caché: se lee del backend cada vez. */
data class FiscalSeriesUiState(
    val series: List<FiscalSeries> = emptyList(),
    val hasLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canManage: Boolean = false,
    val sheet: SeriesSheet? = null,
    val isSaving: Boolean = false,
    val sheetError: UiText? = null,
    val notice: UiText? = null
) {
    val showSkeleton: Boolean get() = !hasLoaded && series.isEmpty() && error == null
}
