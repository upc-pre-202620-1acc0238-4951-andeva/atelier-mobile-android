package com.andeva.atelier.mobile.features.tasks.presentation.detail.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ChronometerTimerTest {

    private val start = Instant.parse("2026-10-28T14:00:00Z")

    @Test
    fun `resta lo pausado del tiempo transcurrido`() {
        assertEquals(3000L, effectiveSeconds(start, 600, start.plusSeconds(3600)))
    }

    @Test
    fun `nunca es negativo`() {
        assertEquals(0L, effectiveSeconds(start, 7200, start.plusSeconds(60)))
    }

    @Test
    fun `formatea horas minutos y segundos`() {
        assertEquals("01:02:05", formatElapsed(3725))
        assertEquals("00:00:00", formatElapsed(-5))
        assertEquals("100:00:00", formatElapsed(360000))
    }
}
