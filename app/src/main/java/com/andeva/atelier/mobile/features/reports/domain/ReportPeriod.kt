package com.andeva.atelier.mobile.features.reports.domain

import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

/** Rango de fechas (ambas incluidas) de un reporte. Falla con `InvalidReportInputException("period")` si from > to. */
data class ReportPeriod(val from: LocalDate, val to: LocalDate) {
    init {
        if (from.isAfter(to)) throw InvalidReportInputException("period")
    }

    companion object {
        fun month(month: YearMonth) = ReportPeriod(month.atDay(1), month.atEndOfMonth())

        fun year(year: Year) = ReportPeriod(year.atDay(1), year.atDay(year.length()))
    }
}
