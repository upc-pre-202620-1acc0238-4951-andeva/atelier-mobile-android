package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.LaborTime
import com.andeva.atelier.mobile.features.tasks.domain.Task
import java.math.BigDecimal
import java.time.Clock
import javax.inject.Inject

/**
 * Horas hombre para precargar al cerrar (US30 E1): desde `startedAt` menos lo pausado. El mecánico la confirma
 * o corrige (hueco H3). null si la tarea nunca se inició.
 */
class SuggestLaborHoursUseCase @Inject constructor(
    private val clock: Clock
) {
    operator fun invoke(task: Task): BigDecimal? =
        task.startedAt?.let { LaborTime.suggestedHours(it, task.totalPausedSeconds, clock.instant()) }
}
