package com.andeva.atelier.mobile.features.tasks.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.apiCallUnit
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.data.sync.PendingUploadDao
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayDao
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleDao
import com.andeva.atelier.mobile.features.reception.infrastructure.local.toEntity as toVehicleEntity
import com.andeva.atelier.mobile.features.reception.infrastructure.repository.ReceptionCachePolicy
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.PauseReason
import com.andeva.atelier.mobile.features.tasks.domain.PendingEvidence
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskDao
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskEntity
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.toEpochMillis
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.AddTaskProductRequestDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.AttachTaskEvidencePayload
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.CompleteTaskRequestDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.HoldTaskRequestDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.PartSearchService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.PauseTaskRequestDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.TaskService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.UpdateTaskProductRequestDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.VehicleService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkBayLookupService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkOrderTaskDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.sync.AttachTaskEvidenceHandler
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Offline-first para leer (docs/06-offline-sync.md §4): la UI observa `tasks` (con placa y bahía por JOIN).
 * Las acciones son en línea (06 §2): sin red fallan con `IOException` y no se encolan; con éxito se guarda en
 * Room lo que devolvió el backend si la tarea es propia. Las fotos van siempre a la cola (06 §6).
 */
class TaskRepositoryImpl @Inject constructor(
    private val taskService: TaskService,
    private val partSearchService: PartSearchService,
    private val vehicleService: VehicleService,
    private val bayService: WorkBayLookupService,
    private val taskDao: TaskDao,
    private val vehicleDao: VehicleDao,
    private val bayDao: BayDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val pendingOperationDao: PendingOperationDao,
    private val pendingUploadDao: PendingUploadDao,
    private val evidenceCapture: TaskEvidenceCapture,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : TaskRepository {

    override fun observeMyTasks(): Flow<List<Task>> = taskDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeTask(id: TaskId): Flow<Task?> = taskDao.observeById(id.value).map { it?.toDomain() }

    /**
     * Recarga `tasks:my` (al abrir y al tocar `TASK_ASSIGNED`). Sin N+1 de detalle (06 §4: el detalle se pide
     * al abrir): después solo bahías por sede y vehículos que faltan, y solo con el permiso para leerlos.
     * Si eso falla se sigue con lo que haya en caché.
     */
    override suspend fun refreshMyTasks(): Result<Unit> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        if (!session.has(Permission.TASKS_READ)) return Result.success(Unit)
        val now = clock.millis()
        return apiCall(gson, { taskService.getMyTasks() }) { it }
            .mapCatching { dtos ->
                val rows = dtos.map { it.toEntity(now) }
                taskDao.replaceMine(rows)
                cacheMetadataDao.upsert(CacheMetadataEntity(SCOPE_MY_TASKS, now, rows.size))
                rows
            }
            .onSuccess { rows -> enrich(rows, session, now) }
            .map { }
    }

    override suspend fun getDetail(id: TaskId): Result<TaskDetail> = act { taskService.getTask(id.value) }

    override suspend fun start(id: TaskId) = act { taskService.start(id.value) }

    override suspend fun pause(id: TaskId, reason: PauseReason) =
        act { taskService.pause(id.value, PauseTaskRequestDto(reason.name)) }

    override suspend fun requestSuspension(id: TaskId, missingItemDescription: String, inventoryItemId: String?) =
        act { taskService.requestHold(id.value, HoldTaskRequestDto(missingItemDescription.trim(), inventoryItemId)) }

    override suspend fun approveSuspension(id: TaskId) = act { taskService.approveHold(id.value) }

    override suspend fun rejectSuspension(id: TaskId) = act { taskService.rejectHold(id.value) }

    override suspend fun resume(id: TaskId) = act { taskService.resume(id.value) }

    override suspend fun complete(id: TaskId, actualLaborHours: BigDecimal, notes: String?) =
        act { taskService.complete(id.value, CompleteTaskRequestDto(actualLaborHours, notes?.trim()?.takeIf { it.isNotEmpty() })) }

    override suspend fun attachEvidence(id: TaskId, workOrderId: String, photo: File, description: String): Result<Unit> =
        evidenceCapture.capture(id.value, workOrderId, photo, description)

    /** Las fotos en cola de la tarea; el archivo local solo mientras no se subió (`UploadFileHandler` lo borra). */
    override fun observePendingEvidence(id: TaskId): Flow<List<PendingEvidence>> = combine(
        pendingOperationDao.observeByTypeAndDedupePrefix(AttachTaskEvidenceHandler.TYPE, TaskEvidenceCapture.evidencePrefix(id.value)),
        pendingUploadDao.observeByTarget(TaskEvidenceCapture.TARGET)
    ) { operations, uploads ->
        val localFiles = uploads.associate { it.id to it.localFilePath }
        operations.map { operation ->
            val payload = gson.fromJson(operation.payload, AttachTaskEvidencePayload::class.java)
            PendingEvidence(
                operationId = operation.id,
                localFilePath = localFiles[operation.dedupeKey.removePrefix(TaskEvidenceCapture.evidencePrefix(id.value))],
                imageUrl = payload.imageUrl,
                description = payload.description,
                status = operation.status,
                lastErrorMessage = operation.lastErrorMessage,
                capturedAt = Instant.ofEpochMilli(operation.createdAt)
            )
        }
    }

    override suspend fun searchParts(query: String): Result<List<PartOption>> =
        apiCall(gson, { partSearchService.search(query.trim().ifEmpty { null }) }) { dtos -> dtos.map { it.toDomain() } }

    override suspend fun addPart(id: TaskId, productId: String, quantity: BigDecimal): Result<TaskProduct> =
        apiCall(gson, { taskService.addProduct(id.value, AddTaskProductRequestDto(productId, quantity)) }) { it.toDomain() }

    /** El `PUT` responde 200 con una línea armada a mano (H9 de docs/03): se ignora su cuerpo y se recarga la tarea. */
    override suspend fun updatePartQuantity(id: TaskId, productId: String, quantity: BigDecimal): Result<TaskDetail> =
        apiCallUnit(gson) { taskService.updateProduct(id.value, productId, UpdateTaskProductRequestDto(quantity)) }
            .fold(onSuccess = { getDetail(id) }, onFailure = { Result.failure(it) })

    override suspend fun removePart(id: TaskId, productId: String): Result<Unit> =
        apiCallUnit(gson) { taskService.removeProduct(id.value, productId) }

    /** Llama, y si la tarea está en `tasks:my` la actualiza (o la saca si quedó cerrada). */
    private suspend fun act(call: suspend () -> Response<WorkOrderTaskDto>): Result<TaskDetail> =
        apiCall(gson, call) { it }
            .onSuccess { saveIfMine(it) }
            .map { it.toDomain() }

    private suspend fun saveIfMine(dto: WorkOrderTaskDto) {
        if (dto.status == TaskStatus.COMPLETED.name || dto.status == TaskStatus.CANCELLED.name) {
            taskDao.delete(dto.id)       // `GET /tasks/my` solo trae tareas abiertas
            return
        }
        taskDao.mergeDetail(
            id = dto.id,
            status = dto.status.orEmpty(),
            description = dto.description.orEmpty(),
            serviceName = dto.serviceName,
            mechanicId = dto.mechanicId,
            holdReason = dto.holdReason,
            missingItemDescription = dto.missingItemDescription,
            pendingSuspensionReason = dto.pendingSuspensionReason,
            startedAt = dto.startedAt.toEpochMillis(),
            totalPausedSeconds = dto.totalPausedSeconds ?: 0,
            syncedAt = clock.millis()
        )
    }

    private suspend fun enrich(rows: List<TaskEntity>, session: Session, now: Long) {
        if (session.has(Permission.BAYS_READ)) {
            rows.mapNotNull { it.branchId }.distinct()
                .filter { isStale(bayScope(it), BAYS_TTL_MILLIS, now) }
                .forEach { refreshBays(it, now) }
        }
        if (session.has(Permission.VEHICLES_READ)) {
            rows.map { it.vehicleId }.distinct()
                .filter { isStale(ReceptionCachePolicy.vehicleScope(it), ReceptionCachePolicy.VEHICLES_TTL_MILLIS, now) }
                .forEach { refreshVehicle(it, now) }
        }
    }

    private suspend fun refreshBays(branchId: String, now: Long) {
        apiCall(gson, { bayService.getBays(branchId) }) { it }.onSuccess { dtos ->
            val bays = dtos.map { it.toEntity(branchId, now) }
            bayDao.replaceBranch(branchId, bays)
            cacheMetadataDao.upsert(CacheMetadataEntity(bayScope(branchId), now, bays.size))
        }
    }

    private suspend fun refreshVehicle(vehicleId: String, now: Long) {
        apiCall(gson, { vehicleService.getVehicle(vehicleId) }) { it }.onSuccess { dto ->
            vehicleDao.saveFromServer(dto.toVehicleEntity(now))
            cacheMetadataDao.upsert(CacheMetadataEntity(ReceptionCachePolicy.vehicleScope(vehicleId), now, 1))
        }
    }

    private suspend fun isStale(scope: String, ttlMillis: Long, now: Long): Boolean {
        val lastSyncedAt = cacheMetadataDao.get(scope)?.lastSyncedAt ?: return true
        return now - lastSyncedAt >= ttlMillis
    }

    companion object {
        const val SCOPE_MY_TASKS = "tasks:my"

        /** Vigencia de docs/06-offline-sync.md §4 (la de vehículos está en [ReceptionCachePolicy]). */
        const val BAYS_TTL_MILLIS = 30 * 1000L

        fun bayScope(branchId: String) = "bays:$branchId"
    }
}
