package com.andeva.atelier.mobile.core.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Zona horaria del negocio (docs/api/README.md §6): los cortes de "hoy" y "este mes" se calculan en
 * Lima (UTC−5, sin horario de verano), no en la zona del teléfono.
 */
object BusinessTime {
    val ZONE: ZoneId = ZoneId.of("America/Lima")

    fun limaDate(instant: Instant): LocalDate = instant.atZone(ZONE).toLocalDate()

    /** Inicio del día de Lima como instante UTC. */
    fun startOfDay(date: LocalDate): Instant = date.atStartOfDay(ZONE).toInstant()
}
