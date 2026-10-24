package com.andeva.atelier.mobile.features.attendance.infrastructure.sync

import com.andeva.atelier.mobile.core.domain.BusinessTime
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Día de Lima de la marcación capturada (o hoy, si el payload no trae la hora). */
internal fun captureDay(capturedAt: String?, clock: Clock): LocalDate =
    BusinessTime.limaDate(capturedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: clock.instant())
