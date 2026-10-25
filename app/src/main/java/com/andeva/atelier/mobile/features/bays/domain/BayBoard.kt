package com.andeva.atelier.mobile.features.bays.domain

import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import java.time.Duration
import java.time.Instant

/** Orden que ocupa una bahía, con sus mecánicos y el tiempo de mano de obra en curso. */
data class BayOccupant(
    val workOrderId: String,
    val internalNumber: Int?,
    val plate: String?,
    val mechanicIds: List<String>,
    val laborElapsed: Duration?
)

/** Una fila del tablero de bahías: la bahía y, si está ocupada, quién la ocupa. */
data class BayBoardItem(
    val bay: WorkBay,
    val occupant: BayOccupant?
)

/**
 * Suma `now − startedAt − totalPausedSeconds` de las tareas `IN_PROGRESS`; null si ninguna está en curso.
 * Con `ON_HOLD` la pausa en curso aún no está en `totalPausedSeconds`, así que esas tareas no se cuentan.
 */
fun laborElapsed(tasks: List<TaskDetail>, now: Instant): Duration? {
    val running = tasks.filter { it.status == TaskStatus.IN_PROGRESS && it.startedAt != null }
    if (running.isEmpty()) return null
    return running.fold(Duration.ZERO) { acc, task ->
        acc + Duration.between(task.startedAt, now).minusSeconds(task.totalPausedSeconds)
    }
}
