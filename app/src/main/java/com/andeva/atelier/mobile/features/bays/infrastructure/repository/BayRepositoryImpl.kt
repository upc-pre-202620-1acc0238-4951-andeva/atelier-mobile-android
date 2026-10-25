package com.andeva.atelier.mobile.features.bays.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayOccupant
import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.domain.laborElapsed
import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayDao
import com.andeva.atelier.mobile.features.bays.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.bays.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.AssignTaskMechanicRequestDto
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.AssignWorkBayRequestDto
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.WorkBayService
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.tasks.infrastructure.repository.TaskRepositoryImpl
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import javax.inject.Inject

/**
 * Tablero de bahías offline-first (docs/06-offline-sync.md §4): la UI observa `bays` + `work_orders` y las acciones
 * en línea dejan la caché al día. El alcance `bays:{branchId}` es el mismo que llena `tasks`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BayRepositoryImpl @Inject constructor(
    private val workBayService: WorkBayService,
    private val workOrderService: WorkOrderService,
    private val bayDao: BayDao,
    private val workOrderDao: WorkOrderDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : BayRepository {

    /** Sube con cada cambio de mecánico: los tableros en observación descartan los detalles ya leídos. */
    private val mechanicChanges = MutableStateFlow(0)

    override suspend fun refreshBranchBays(): Result<Unit> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        val branchId = session.branchId ?: return Result.failure(MissingBranchException())
        val now = clock.millis()
        return apiCall(gson, { workBayService.getBays(branchId) }) { it }.mapCatching { dtos ->
            val rows = dtos.map { it.toEntity(branchId, now) }
            bayDao.replaceBranch(branchId, rows)
            cacheMetadataDao.upsert(CacheMetadataEntity(TaskRepositoryImpl.bayScope(branchId), now, rows.size))
        }
    }

    /**
     * El detalle de la orden (mecánicos y tareas en curso) se lee una sola vez por `currentWorkOrderId` mientras
     * dure la colección; el tiempo se recalcula con el reloj en cada emisión, sin volver a la red.
     */
    override fun observeBoard(): Flow<List<BayBoardItem>> = sessionManager.session
        .map { it?.branchId }
        .distinctUntilChanged()
        .flatMapLatest { branchId ->
            if (branchId == null) return@flatMapLatest flowOf(emptyList())
            flow {
                val details = HashMap<String, List<TaskDetail>>()
                var version = mechanicChanges.value
                emitAll(
                    combine(
                        bayDao.observeBranch(branchId),
                        workOrderDao.observeBranch(branchId),
                        mechanicChanges
                    ) { bays, orders, currentVersion ->
                        if (currentVersion != version) {
                            details.clear()
                            version = currentVersion
                        }
                        val board = bays.map { it.toDomain() }
                        details.keys.retainAll(board.mapNotNull { it.currentWorkOrderId }.toSet())
                        val ordersById = orders.associateBy { it.workOrder.id }
                        board.map { bay ->
                            val orderId = bay.currentWorkOrderId ?: return@map BayBoardItem(bay, null)
                            val tasks = details[orderId] ?: loadTasks(orderId)?.also { details[orderId] = it }
                            val row = ordersById[orderId]
                            BayBoardItem(
                                bay,
                                BayOccupant(
                                    workOrderId = orderId,
                                    internalNumber = row?.workOrder?.internalNumber,
                                    plate = row?.plate,
                                    mechanicIds = tasks.orEmpty().mapNotNull { it.mechanicId }.distinct(),
                                    laborElapsed = tasks?.let { laborElapsed(it, clock.instant()) }
                                )
                            )
                        }
                    }
                )
            }
        }

    /** null si falla: esa bahía sale sin mecánicos y se reintenta en la próxima emisión. */
    private suspend fun loadTasks(workOrderId: String): List<TaskDetail>? =
        apiCall(gson, { workOrderService.getWorkOrder(workOrderId) }) { dto -> dto.tasks.orEmpty().map { it.toDomain() } }
            .getOrNull()

    /** La cabecera nueva trae la bahía; la recarga de bahías es de cortesía y no desmiente la reasignación. */
    override suspend fun reassignBay(workOrderId: String, bayId: String): Result<Unit> =
        apiCall(gson, { workBayService.assignBay(workOrderId, AssignWorkBayRequestDto(bayId)) }) { it }
            .map { dto -> workOrderDao.saveHeader(dto.toEntity(clock.millis())) }
            .also { if (it.isSuccess) refreshBranchBays() }

    override suspend fun reassignMechanic(taskId: String, mechanicId: String): Result<Unit> =
        apiCall(gson, { workBayService.assignMechanic(taskId, AssignTaskMechanicRequestDto(mechanicId)) }) { it }
            .map { mechanicChanges.update { value -> value + 1 } }
}
