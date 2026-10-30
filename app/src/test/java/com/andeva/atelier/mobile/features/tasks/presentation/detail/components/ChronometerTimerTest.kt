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

    // --- D1: que muestra el cronometro segun el estado ---

    private val now = start.plusSeconds(100_000)

    @Test
    fun `en marcha cuenta desde el inicio menos lo pausado`() {
        val display = timerDisplay(start, 600, running = true, paused = false, pausedElapsedSeconds = null, now = start.plusSeconds(3600))
        assertEquals(TimerDisplay("00:50:00", unknown = false), display)
    }

    @Test
    fun `en pausa muestra el tiempo guardado y no depende de la hora actual`() {
        val a = timerDisplay(start, 0, running = false, paused = true, pausedElapsedSeconds = 296, now = now)
        val b = timerDisplay(start, 0, running = false, paused = true, pausedElapsedSeconds = 296, now = now.plusSeconds(3600))
        assertEquals(TimerDisplay("00:04:56", unknown = false), a)
        assertEquals(a, b)
    }

    @Test
    fun `en pausa sin tiempo guardado no inventa un valor`() {
        val display = timerDisplay(start, 0, running = false, paused = true, pausedElapsedSeconds = null, now = now)
        assertEquals(TimerDisplay(UNKNOWN_ELAPSED, unknown = true), display)
    }

    @Test
    fun `una tarea sin iniciar muestra cero`() {
        val display = timerDisplay(null, 0, running = false, paused = false, pausedElapsedSeconds = null, now = now)
        assertEquals(TimerDisplay("00:00:00", unknown = false), display)
    }
}
