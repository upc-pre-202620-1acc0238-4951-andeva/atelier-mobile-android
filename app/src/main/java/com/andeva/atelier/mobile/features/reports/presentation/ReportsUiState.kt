package com.andeva.atelier.mobile.features.reports.presentation

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reports.domain.CashFlowSummary
import com.andeva.atelier.mobile.features.reports.domain.ReportPeriod
import java.io.File
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

sealed interface PeriodChoice {
    data object ThisMonth : PeriodChoice
    data object PreviousMonth : PeriodChoice
    data object ThisYear : PeriodChoice
    data class Custom(val from: LocalDate, val to: LocalDate) : PeriodChoice
}

enum class ReportPdfActionMode { OPEN, SHARE }

/** El rango de fechas de [choice] visto desde [today] (día de Lima). */
fun periodFor(choice: PeriodChoice, today: LocalDate): ReportPeriod = when (choice) {
    PeriodChoice.ThisMonth -> ReportPeriod.month(YearMonth.from(today))
    PeriodChoice.PreviousMonth -> ReportPeriod.month(YearMonth.from(today).minusMonths(1))
    PeriodChoice.ThisYear -> ReportPeriod.year(Year.from(today))
    is PeriodChoice.Custom -> ReportPeriod(
        from = minOf(choice.from, choice.to),
        to = maxOf(choice.from, choice.to)
    )
}

enum class ReportDownload { CashFlow, FleetHealth }

/** Hoja para elegir al cliente del informe de salud de flota. */
data class FleetSheet(
    val query: String = "",
    val results: List<Customer> = emptyList(),
    val hasSearched: Boolean = false,
    val isSearching: Boolean = false,
    val actionMode: ReportPdfActionMode = ReportPdfActionMode.OPEN,
    val error: UiText? = null
)

/**
 * Reportes del taller: flujo de caja del periodo (todas las sedes, H30) y descargas en PDF. Nunca se muestran
 * `totalExpenses` ni `accountsReceivable`: llegan siempre en 0 (H31). Todo es en línea y sin caché.
 */
data class ReportsUiState(
    val today: LocalDate = LocalDate.MIN,
    val choice: PeriodChoice = PeriodChoice.ThisMonth,
    val period: ReportPeriod = ReportPeriod(LocalDate.MIN, LocalDate.MIN),
    val summary: CashFlowSummary? = null,
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
    val error: UiText? = null,
    val canReadCashFlow: Boolean = false,
    /** Informe de flota: el rol lo tiene, el plan lo incluye y se puede buscar al cliente. */
    val canDownloadFleet: Boolean = false,
    /** El rol tiene el permiso pero el plan del taller no lo incluye. */
    val fleetLockedByPlan: Boolean = false,
    val busy: ReportDownload? = null,
    val pdf: File? = null,
    val pdfActionMode: ReportPdfActionMode = ReportPdfActionMode.OPEN,
    val showCustomRangePicker: Boolean = false,
    val actionError: UiText? = null,
    val fleetSheet: FleetSheet? = null
) {
    val hasAnyAccess: Boolean get() = canReadCashFlow || canDownloadFleet || fleetLockedByPlan
}
