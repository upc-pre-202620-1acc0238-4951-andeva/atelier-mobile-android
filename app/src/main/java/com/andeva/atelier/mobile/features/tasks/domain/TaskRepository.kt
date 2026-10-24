package com.andeva.atelier.mobile.features.tasks.domain

import kotlinx.coroutines.flow.Flow
import java.io.File
import java.math.BigDecimal

/**
 * Tareas del mecánico (US26–US30, US23, US33 E1). Offline-first para leer (red → Room → UI); las acciones
 * son en línea (docs/06-offline-sync.md §2) salvo las fotos, que van a la cola.
 */
interface TaskRepository {
    /** Mis tareas abiertas: en curso → en pausa → asignadas. */
    fun observeMyTasks(): Flow<List<Task>>

    fun observeTask(id: TaskId): Flow<Task?>

    /** `GET /tasks/my` y luego placa y bahía. Falla solo si no se pudo leer la lista. */
    suspend fun refreshMyTasks(): Result<Unit>

    suspend fun getDetail(id: TaskId): Result<TaskDetail>

    suspend fun start(id: TaskId): Result<TaskDetail>

    suspend fun pause(id: TaskId, reason: PauseReason): Result<TaskDetail>

    suspend fun requestSuspension(id: TaskId, missingItemDescription: String, inventoryItemId: String?): Result<TaskDetail>

    suspend fun approveSuspension(id: TaskId): Result<TaskDetail>

    suspend fun rejectSuspension(id: TaskId): Result<TaskDetail>

    suspend fun resume(id: TaskId): Result<TaskDetail>

    suspend fun complete(id: TaskId, actualLaborHours: BigDecimal, notes: String?): Result<TaskDetail>

    /** Guarda la foto comprimida y la encola (`UPLOAD_FILE` + `ATTACH_TASK_EVIDENCE`), con o sin red. */
    suspend fun attachEvidence(id: TaskId, workOrderId: String, photo: File, description: String): Result<Unit>

    fun observePendingEvidence(id: TaskId): Flow<List<PendingEvidence>>

    suspend fun searchParts(query: String): Result<List<PartOption>>

    suspend fun addPart(id: TaskId, productId: String, quantity: BigDecimal): Result<TaskProduct>

    suspend fun updatePartQuantity(id: TaskId, productId: String, quantity: BigDecimal): Result<TaskDetail>

    suspend fun removePart(id: TaskId, productId: String): Result<Unit>
}
