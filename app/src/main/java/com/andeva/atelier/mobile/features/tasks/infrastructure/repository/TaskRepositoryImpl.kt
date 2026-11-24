package com.andeva.atelier.mobile.features.tasks.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.apiCallUnit
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.data.sync.PendingUploadDao
import com.andeva.atelier.mobile.core.data.sync.WorkshopCacheEvent
import com.andeva.atelier.mobile.core.data.sync.WorkshopCacheEventBus
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayDao
import com.andeva.atelier.mobile.features.bays.infrastructure.repository.BayCacheScope
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
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.InventoryService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.PauseTaskRequestDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.TaskService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.UpdateTaskProductRequestDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.VehicleService
import com.andeva.atelier.mobile.features.bays.infrastructure.local.toEntity as toBayEntity
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.WorkBayService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkOrderTaskDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.sync.AttachTaskEvidenceHandler
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val inventoryService: InventoryService,
    private val vehicleService: VehicleService,
    private val bayService: WorkBayService,
    private val taskDao: TaskDao,
    private val vehicleDao: VehicleDao,
    private val bayDao: BayDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val pendingOperationDao: PendingOperationDao,
    private val pendingUploadDao: PendingUploadDao,
    private val evidenceCapture: TaskEvidenceCapture,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock,
    private val cacheEventBus: WorkshopCacheEventBus = WorkshopCacheEventBus(),
    private val workOrderService: WorkOrderService? = null
) : TaskRepository {

    override fun observeMyTasks(): Flow<List<Task>> = taskDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeTask(id: TaskId): Flow<Task?> = taskDao.observeById(id.value).map { it?.toDomain() }

    /**
     * Recarga `tasks:my` (al abrir y al tocar `TASK_ASSIGNED`), o todas las tareas activas de las órdenes de la sede
     * cuando la sesión es de supervisión del taller (Dueño, Administrador, Asesor) y no ejecuta cronómetro propio.
     */
    override suspend fun refreshMyTasks(): Result<Unit> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        if (!session.has(Permission.TASKS_READ)) return Result.success(Unit)
        val now = clock.millis()
        val isWorkshopSupervisor = !session.has(Permission.TASKS_TRACK_TIME) &&
            (session.has(Permission.WORK_ORDERS_UPDATE) ||
                session.has(Permission.TASKS_HOLD_VALIDATE) ||
                session.has(Permission.BAYS_REASSIGN))
        if (isWorkshopSupervisor && workOrderService != null &&
            session.has(Permission.WORK_ORDERS_READ) && !session.branchId.isNullOrBlank()
        ) {
            return refreshWorkshopTasks(session, session.branchId, now)
        }
        return apiCall(gson, { taskService.getMyTasks() }) { it }
            .mapCatching { dtos ->
                val rows = dtos.map { it.toEntity(now) }
                if (rows.isEmpty() && workOrderService != null &&
                    session.has(Permission.WORK_ORDERS_READ) &&
                    session.has(Permission.TASKS_HOLD_VALIDATE) &&
                    !session.branchId.isNullOrBlank()
                ) {
                    val fallback = loadWorkshopTaskEntities(session.branchId, now).getOrNull()
                    if (!fallback.isNullOrEmpty()) {
                        taskDao.replaceMine(fallback)
                        cacheMetadataDao.upsert(CacheMetadataEntity(SCOPE_MY_TASKS, now, fallback.size))
                        return@mapCatching fallback
                    }
                }
                taskDao.replaceMine(rows)
                cacheMetadataDao.upsert(CacheMetadataEntity(SCOPE_MY_TASKS, now, rows.size))
                rows
            }
            .onSuccess { rows -> enrich(rows, session, now) }
            .map { }
    }

    private suspend fun refreshWorkshopTasks(session: Session, branchId: String, now: Long): Result<Unit> =
        loadWorkshopTaskEntities(branchId, now)
            .mapCatching { rows ->
                taskDao.replaceMine(rows)
                cacheMetadataDao.upsert(CacheMetadataEntity(SCOPE_MY_TASKS, now, rows.size))
                rows
            }
            .onSuccess { rows -> enrich(rows, session, now) }
            .map { }

    private suspend fun loadWorkshopTaskEntities(branchId: String, now: Long): Result<List<TaskEntity>> {
        val service = workOrderService ?: return Result.success(emptyList())
        return apiCall(gson, { service.getWorkOrders(branchId = branchId) }) { it }
            .mapCatching { summaries ->
                val activeSummaries = summaries
                    .filter { it.status !in listOf("PAID", "CANCELED", "COMPLETED") }
                    .take(15)
                val details = coroutineScope {
                    activeSummaries.map { summary ->
                        async { apiCall(gson, { service.getWorkOrder(summary.id) }) { it }.getOrNull() }
                    }.awaitAll().filterNotNull()
                }
                details.flatMap { order ->
                    order.tasks.orEmpty()
                        .filter { it.status !in listOf(TaskStatus.COMPLETED.name, TaskStatus.CANCELLED.name) }
                        .map { task ->
                            TaskEntity(
                                id = task.id,
                                workOrderId = order.id,
                                workOrderNumber = order.internalNumber,
                                workOrderStatus = order.status,
                                vehicleId = order.vehicleId,
                                branchId = order.branchId,
                                bayId = order.currentBayId,
                                serviceName = task.serviceName?.takeIf { it.isNotBlank() && it != "Standard Service" },
                                mechanicId = task.mechanicId,
                                status = task.status.orEmpty(),
                                description = task.description.orEmpty(),
                                estimatedHours = null,
                                holdReason = task.holdReason,
                                missingItemDescription = task.missingItemDescription,
                                pendingSuspensionReason = task.pendingSuspensionReason,
                                pendingSuspensionRequestedAt = task.pendingSuspensionRequestedAt.toEpochMillis(),
                                startedAt = task.startedAt.toEpochMillis(),
                                totalPausedSeconds = task.totalPausedSeconds ?: 0,
                                syncedAt = now
                            )
                        }
                }
            }
    }

    override suspend fun getDetail(id: TaskId): Result<TaskDetail> = act { taskService.getTask(id.value) }

    override suspend fun start(id: TaskId) = act { taskService.start(id.value) }

    override suspend fun pause(id: TaskId, reason: PauseReason) =
        act { taskService.pause(id.value, PauseTaskRequestDto(reason.name)) }

    override suspend fun requestSuspension(id: TaskId, missingItemDescription: String, inventoryItemId: String?) =
        act { taskService.requestHold(id.value, HoldTaskRequestDto(missingItemDescription.trim(), inventoryItemId)) }
            .onSuccess { cacheEventBus.tryEmit(WorkshopCacheEvent.WorkOrderMutated(it.workOrderId)) }

    override suspend fun approveSuspension(id: TaskId) =
        act { taskService.approveHold(id.value) }
            .onSuccess { cacheEventBus.tryEmit(WorkshopCacheEvent.WorkOrderMutated(it.workOrderId)) }

    override suspend fun rejectSuspension(id: TaskId) =
        act { taskService.rejectHold(id.value) }
            .onSuccess { cacheEventBus.tryEmit(WorkshopCacheEvent.WorkOrderMutated(it.workOrderId)) }

    override suspend fun resume(id: TaskId) =
        act { taskService.resume(id.value) }
            .onSuccess { cacheEventBus.tryEmit(WorkshopCacheEvent.WorkOrderMutated(it.workOrderId)) }

    override suspend fun complete(id: TaskId, actualLaborHours: BigDecimal, notes: String?) =
        act { taskService.complete(id.value, CompleteTaskRequestDto(actualLaborHours, notes?.trim()?.takeIf { it.isNotEmpty() })) }
            .onSuccess { cacheEventBus.tryEmit(WorkshopCacheEvent.WorkOrderMutated(it.workOrderId)) }

    override suspend fun reopen(id: TaskId, reason: String?) =
        act { taskService.reopen(id.value, reason?.trim()?.takeIf { it.isNotEmpty() }) }

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
        apiCall(gson, { inventoryService.getItems(search = query.trim().ifEmpty { null }) }) { dtos -> dtos.map { it.toDomain() } }

    override suspend fun addPart(id: TaskId, productId: String, quantity: BigDecimal): Result<TaskProduct> =
        apiCall(gson, { taskService.addProduct(id.value, AddTaskProductRequestDto(productId, quantity)) }) { it.toDomain() }

    /** El `PUT` responde 200 con una línea armada a mano (H9 de docs/03): se ignora su cuerpo y se recarga la tarea. */
    override suspend fun updatePartQuantity(id: TaskId, productId: String, quantity: BigDecimal): Result<TaskDetail> =
        apiCallUnit(gson) { taskService.updateProduct(id.value, productId, UpdateTaskProductRequestDto(quantity)) }
            .fold(onSuccess = { getDetail(id) }, onFailure = { Result.failure(it) })

    override suspend fun removePart(id: TaskId, productId: String): Result<Unit> =
        apiCallUnit(gson) { taskService.removeProduct(id.value, productId) }

    @Volatile
    private var cachedPartNames: Pair<String, Map<String, String>>? = null

    /** Llama, y si la tarea está en `tasks:my` la actualiza (o la saca si quedó cerrada). */
    private suspend fun act(call: suspend () -> Response<WorkOrderTaskDto>): Result<TaskDetail> =
        apiCall(gson, call) { it }
            .onSuccess { saveIfMine(it) }
            .map { enrichProducts(it.toDomain()) }

    private suspend fun enrichProducts(detail: TaskDetail): TaskDetail {
        if (detail.products.none { it.productName.isNullOrBlank() }) return detail
        val session = sessionManager.getSession() ?: return detail
        if (!session.has(Permission.PARTS_READ)) return detail
        val names = cachedPartNames?.takeIf { it.first == session.tenantId }?.second ?: run {
            val loaded = apiCall(gson, { inventoryService.getItems(search = null) }) { dtos ->
                dtos.mapNotNull { dto -> dto.name?.takeIf { it.isNotBlank() }?.let { dto.id to it } }.toMap()
            }.getOrNull().orEmpty()
            if (loaded.isNotEmpty()) cachedPartNames = session.tenantId to loaded
            loaded
        }
        if (names.isEmpty()) return detail
        return detail.copy(
            products = detail.products.map { product ->
                if (product.productName.isNullOrBlank()) {
                    product.copy(productName = names[product.productId])
                } else product
            }
        )
    }

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
            pendingSuspensionRequestedAt = dto.pendingSuspensionRequestedAt.toEpochMillis(),
            startedAt = dto.startedAt.toEpochMillis(),
            totalPausedSeconds = dto.totalPausedSeconds ?: 0,
            syncedAt = clock.millis()
        )
    }

    private suspend fun enrich(rows: List<TaskEntity>, session: Session, now: Long) {
        if (session.has(Permission.BAYS_READ)) {
            rows.mapNotNull { it.branchId }.distinct()
                .filter { isStale(BayCacheScope.of(it), BAYS_TTL_MILLIS, now) }
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
            val bays = dtos.map { it.toBayEntity(branchId, now) }
            bayDao.replaceBranch(branchId, bays)
            cacheMetadataDao.upsert(CacheMetadataEntity(BayCacheScope.of(branchId), now, bays.size))
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
    }
}
