package com.andeva.atelier.mobile.features.tasks.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/**
 * Horas hombre sugeridas para cerrar la tarea (US30). Es solo una sugerencia que el mecánico confirma:
 * docs/api/operations.md no aclara si `startedAt` es el primer inicio o la última reanudación (hueco H3).
 */
object LaborTime {
    private const val SECONDS_PER_HOUR = 3600L

    fun suggestedHours(startedAt: Instant, totalPausedSeconds: Long, now: Instant): BigDecimal {
        val worked = (Duration.between(startedAt, now).seconds - totalPausedSeconds).coerceAtLeast(0)
        return BigDecimal.valueOf(worked).divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 2, RoundingMode.HALF_UP)
    }
}
