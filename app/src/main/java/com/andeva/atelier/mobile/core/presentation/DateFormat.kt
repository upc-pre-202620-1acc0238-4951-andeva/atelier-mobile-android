package com.andeva.atelier.mobile.core.presentation

import com.andeva.atelier.mobile.core.domain.BusinessTime
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-PE"))

/** Fecha del taller (America/Lima) a partir de un instante UTC del backend (docs/api/README.md). */
fun Instant.formatLimaDate(): String = DayFormatter.format(atZone(BusinessTime.ZONE))
