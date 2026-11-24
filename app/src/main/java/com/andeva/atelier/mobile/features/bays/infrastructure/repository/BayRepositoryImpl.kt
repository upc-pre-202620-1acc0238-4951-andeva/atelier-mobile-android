package com.andeva.atelier.mobile.features.bays.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.apiCallUnit
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayOccupant
import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.bays.domain.laborElapsed
import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayDao
import com.andeva.atelier.mobile.features.bays.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.bays.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.AssignTaskMechanicRequestDto
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.AssignWorkBayRequestDto
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.MaintenanceBayRequestDto
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.WorkBayService
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskDao
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskWithRefs
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.Clock
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tablero de bahías offline-first (docs/06-offline-sync.md §4): la UI observa `bays` + `work_orders` y las acciones
 * en línea dejan la caché al día. El alcance `bays:{branchId}` es el mismo que llena `tasks`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class BayRepositoryImpl @Inject constructor(
    private val workBayService: WorkBayService,
    private val workOrderService: WorkOrderService,
    private val bayDao: BayDao,
    private val workOrderDao: WorkOrderDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock,
    private val taskDao: TaskDao
) : BayRepository {

    /** Sube con cada refresco o cambio de mecánico: revalida detalles en segundo plano sin vaciar la vista previa. */
    private val refreshVersion = MutableStateFlow(0)
    private val detailSemaphore = Semaphore(MAX_DETAIL_CONCURRENCY)

    override suspend fun refreshBranchBays(): Result<Unit> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        val branchId = session.branchId ?: return Result.failure(MissingBranchException())
        val now = clock.millis()
        return apiCall(gson, { workBayService.getBays(branchId) }) { it }.mapCatching { dtos ->
            val rows = dtos.map { it.toEntity(branchId, now) }
            bayDao.replaceBranch(branchId, rows)
            cacheMetadataDao.upsert(CacheMetadataEntity(BayCacheScope.of(branchId), now, rows.size))
        }
    }

    /**
     * Sale de Room de inmediato; el detalle de la orden (mecánicos y tareas en curso) se lee en segundo plano con
     * concurrencia acotada (`Semaphore(3)`) y estrategia stale-while-revalidate cuando sube [refreshVersion].
     */
    override fun observeBoard(): Flow<List<BayBoardItem>> = sessionManager.session
        .map { it?.branchId }
        .distinctUntilChanged()
        .flatMapLatest { branchId ->
            if (branchId == null) return@flatMapLatest flowOf(emptyList())
            channelFlow {
                val details = MutableStateFlow<Map<String, List<TaskDetail>>>(emptyMap())
                val requested = HashSet<String>()
                val lastVersion = AtomicInteger(refreshVersion.value)
                val bays = bayDao.observeBranch(branchId).map { rows -> rows.map { it.toDomain() } }
                val ordersFlow = workOrderDao.observeBranch(branchId)
                val tasksFlow: Flow<List<TaskWithRefs>> = taskDao.observeAll()

                val activeOrderIds = combine(bays, ordersFlow, tasksFlow) { board, orders, tasks ->
                    val ordersByBayId = orders.associateBy { it.workOrder.currentBayId }
                    val tasksByBayId = tasks.filter { it.task.branchId == branchId }.associateBy { it.task.bayId }
                    board.mapNotNull { bay ->
                        bay.currentWorkOrderId
                            ?: ordersByBayId[bay.id]?.workOrder?.id
                            ?: tasksByBayId[bay.id]?.task?.workOrderId
                    }.toSet()
                }.distinctUntilChanged()

                launch {
                    combine(
                        activeOrderIds,
                        refreshVersion
                    ) { ids, version ->
                        ids to version
                    }.collect { (ids, version) ->
                        if (requested.retainAll(ids) || details.value.keys.any { it !in ids }) {
                            details.update { it.filterKeys { id -> id in ids } }
                        }
                        if (version != lastVersion.getAndSet(version)) {
                            // Stale-while-revalidate: conservamos `details` actuales mientras re-consultamos `ids`.
                            requested.clear()
                        }
                        ids.filter { requested.add(it) }.forEach { orderId ->
                            launch {
                                detailSemaphore.withPermit {
                                    loadTasks(orderId)
                                }?.let { tasks ->
                                    details.update { it + (orderId to tasks) }
                                }
                            }
                        }
                    }
                }

                combine(bays, ordersFlow, tasksFlow, details) { board, orders, cachedTasks, loaded ->
                    val ordersById = orders.associateBy { it.workOrder.id }
                    val ordersByBayId = orders.associateBy { it.workOrder.currentBayId }
                    val tasksByBayId = cachedTasks.filter { it.task.branchId == branchId }.associateBy { it.task.bayId }
                    val hasSyncedContext = orders.isNotEmpty() || cachedTasks.isNotEmpty()
                    board.map { bay ->
                        val row = bay.currentWorkOrderId?.let { ordersById[it] } ?: ordersByBayId[bay.id]
                        val fallbackTask = tasksByBayId[bay.id]
                        val orderId = bay.currentWorkOrderId ?: row?.workOrder?.id ?: fallbackTask?.task?.workOrderId
                        if (orderId == null) {
                            // Si el backend dejó `status = OCCUPIED` con `currentWorkOrderId = null` tras entregar
                            // una orden ya cerrada y ninguna orden abierta apunta a esta bahía, se muestra libre.
                            val normalizedBay = if (bay.status == BayStatus.OCCUPIED && hasSyncedContext) {
                                bay.copy(status = BayStatus.AVAILABLE)
                            } else bay
                            return@map BayBoardItem(normalizedBay, null)
                        }
                        val tasks = loaded[orderId]
                        BayBoardItem(
                            bay.copy(status = BayStatus.OCCUPIED, currentWorkOrderId = orderId),
                            BayOccupant(
                                workOrderId = orderId,
                                internalNumber = row?.workOrder?.internalNumber ?: fallbackTask?.task?.workOrderNumber,
                                plate = row?.plate ?: fallbackTask?.plate,
                                mechanicIds = tasks.orEmpty().mapNotNull { it.mechanicId }.distinct(),
                                laborElapsed = tasks?.let { laborElapsed(it, clock.instant()) },
                                tasks = tasks.orEmpty()
                            )
                        )
                    }
                }.collect { send(it) }
            }
        }

    /**
     * Guarda también la cabecera de la orden (`work_orders`) para que el tablero tenga el `internalNumber` aunque la
     * lista de órdenes nunca se haya refrescado. Si la sesión no tiene `operations:work_orders:read`, no consulta el backend.
     */
    private suspend fun loadTasks(workOrderId: String): List<TaskDetail>? = try {
        val session = sessionManager.getSession()
        if (session != null && !session.has(Permission.WORK_ORDERS_READ)) {
            null
        } else {
            apiCall(gson, { workOrderService.getWorkOrder(workOrderId) }) { it }
                .mapCatching { dto ->
                    workOrderDao.saveHeader(dto.toEntity(clock.millis()))
                    dto.tasks.orEmpty().map { it.toDomain() }
                }
                .getOrNull()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** La cabecera nueva trae la bahía; las bahías se recargan para que la anterior quede libre y la nueva ocupada. */
    override suspend fun reassignBay(workOrderId: String, bayId: String): Result<Unit> =
        apiCall(gson, { workBayService.assignBay(workOrderId, AssignWorkBayRequestDto(bayId)) }) { it }
            .mapCatching { dto -> workOrderDao.saveHeader(dto.toEntity(clock.millis())) }
            .also { if (it.isSuccess) refreshBranchBays() }

    override suspend fun releaseBay(workOrderId: String): Result<Unit> =
        apiCallUnit(gson) { workBayService.releaseBay(workOrderId) }
            .also { if (it.isSuccess) refreshBranchBays() }

    override suspend fun setBayMaintenance(bayId: String, reason: String): Result<WorkBay> {
        val fallbackBranch = sessionManager.getSession()?.branchId.orEmpty()
        return apiCall(gson, { workBayService.setMaintenance(bayId, MaintenanceBayRequestDto(reason.trim())) }) { it }
            .mapCatching { dto ->
                val entity = dto.toEntity(fallbackBranch, clock.millis())
                bayDao.upsert(entity)
                entity.toDomain()
            }.also { if (it.isSuccess) refreshBranchBays() }
    }

    override suspend fun restoreBay(bayId: String): Result<WorkBay> {
        val fallbackBranch = sessionManager.getSession()?.branchId.orEmpty()
        return apiCall(gson, { workBayService.restoreBay(bayId) }) { it }
            .mapCatching { dto ->
                val entity = dto.toEntity(fallbackBranch, clock.millis())
                bayDao.upsert(entity)
                entity.toDomain()
            }.also { if (it.isSuccess) refreshBranchBays() }
    }

    override suspend fun reassignMechanic(taskId: String, mechanicId: String): Result<Unit> =
        apiCall(gson, { workBayService.assignMechanic(taskId, AssignTaskMechanicRequestDto(mechanicId)) }) { it }
            .map { refreshVersion.update { value -> value + 1 } }

    override suspend fun createBay(name: String, bayType: BayType): Result<WorkBay> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        val branchId = session.branchId ?: return Result.failure(MissingBranchException())
        val body = com.andeva.atelier.mobile.features.bays.infrastructure.remote.CreateWorkBayRequestDto(
            branchId = branchId,
            name = name.trim(),
            bayType = bayType.name
        )
        return apiCall(gson, { workBayService.createBay(body) }) { it }.mapCatching { dto ->
            val now = clock.millis()
            val entity = dto.toEntity(branchId, now)
            bayDao.upsert(entity)
            entity.toDomain()
        }.also { if (it.isSuccess) refreshBranchBays() }
    }

    private companion object {
        const val MAX_DETAIL_CONCURRENCY = 3
    }
}
