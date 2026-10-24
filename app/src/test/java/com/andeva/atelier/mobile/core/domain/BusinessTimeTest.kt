package com.andeva.atelier.mobile.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BusinessTimeTest {

    @Test
    fun `el dia se corta en Lima y no en UTC`() {
        // 03:00 UTC del 25 son las 22:00 del 24 en Lima (UTC−5)
        assertEquals(LocalDate.parse("2026-10-24"), BusinessTime.limaDate(Instant.parse("2026-10-25T03:00:00Z")))
        assertEquals(LocalDate.parse("2026-10-25"), BusinessTime.limaDate(Instant.parse("2026-10-25T05:00:00Z")))
    }

    @Test
    fun `inicio del dia de Lima en UTC`() {
        assertEquals(Instant.parse("2026-10-24T05:00:00Z"), BusinessTime.startOfDay(LocalDate.parse("2026-10-24")))
    }
}
