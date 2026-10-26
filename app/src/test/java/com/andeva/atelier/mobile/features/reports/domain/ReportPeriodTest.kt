package com.andeva.atelier.mobile.features.reports.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class ReportPeriodTest {
    @Test
    fun `month da el primer y el ultimo dia del mes`() {
        val period = ReportPeriod.month(YearMonth.of(2028, 2))

        assertEquals(LocalDate.of(2028, 2, 1), period.from)
        assertEquals(LocalDate.of(2028, 2, 29), period.to)
    }

    @Test
    fun `year da del 1 de enero al 31 de diciembre`() {
        val period = ReportPeriod.year(Year.of(2026))

        assertEquals(LocalDate.of(2026, 1, 1), period.from)
        assertEquals(LocalDate.of(2026, 12, 31), period.to)
    }

    @Test
    fun `periodo invertido falla con period`() {
        val error = assertThrows(InvalidReportInputException::class.java) {
            ReportPeriod(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1))
        }

        assertEquals("period", error.field)
    }
}
