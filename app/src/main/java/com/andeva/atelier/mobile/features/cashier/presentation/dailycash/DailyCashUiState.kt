package com.andeva.atelier.mobile.features.cashier.presentation.dailycash

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.cashier.domain.DailyCashSummary
import java.time.LocalDate

/** Caja del día de la sede: lo cobrado por medio de pago. Solo en línea y sin caché: no hay Room para este reporte. */
data class DailyCashUiState(
    val today: LocalDate = LocalDate.MIN,
    val date: LocalDate = LocalDate.MIN,
    val summary: DailyCashSummary? = null,
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canRead: Boolean = false
) {
    val isToday: Boolean get() = date == today
    val canGoNext: Boolean get() = date.isBefore(today)
}
