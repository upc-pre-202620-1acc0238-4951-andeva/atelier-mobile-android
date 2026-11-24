package com.andeva.atelier.mobile.features.reports.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodForTest {
    private val today = LocalDate.of(2026, 3, 16)

    @Test
    fun `este mes va del primero al ultimo dia`() {
        val period = periodFor(PeriodChoice.ThisMonth, today)
        assertEquals(LocalDate.of(2026, 3, 1), period.from)
        assertEquals(LocalDate.of(2026, 3, 31), period.to)
    }

    @Test
    fun `mes pasado cruza al anio anterior en enero`() {
        val period = periodFor(PeriodChoice.PreviousMonth, LocalDate.of(2026, 1, 5))
        assertEquals(LocalDate.of(2025, 12, 1), period.from)
        assertEquals(LocalDate.of(2025, 12, 31), period.to)
    }

    @Test
    fun `este anio cubre de enero a diciembre`() {
        val period = periodFor(PeriodChoice.ThisYear, today)
        assertEquals(LocalDate.of(2026, 1, 1), period.from)
        assertEquals(LocalDate.of(2026, 12, 31), period.to)
    }

    @Test
    fun `rango personalizado conserva las fechas elegidas`() {
        val from = LocalDate.of(2026, 2, 10)
        val to = LocalDate.of(2026, 3, 5)
        val period = periodFor(PeriodChoice.Custom(from, to), today)
        assertEquals(from, period.from)
        assertEquals(to, period.to)
    }
}
