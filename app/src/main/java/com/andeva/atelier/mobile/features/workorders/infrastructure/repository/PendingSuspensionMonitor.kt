package com.andeva.atelier.mobile.features.workorders.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.WorkshopCacheEvent
import com.andeva.atelier.mobile.core.data.sync.WorkshopCacheEventBus
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.workorders.domain.PendingSuspensionItem
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.saveHeaderOrDeleteClosed
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toDetail
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor en memoria de solicitudes de suspensión pendientes de validación administrativa (US29 / H5).
 * No escribe tareas de terceros en `TaskDao` (que es exclusivo de `tasks:my`).
 * Aplica TTL de 60 s, concurrencia acotada (`Semaphore(3)`) y tope de 15 órdenes `IN_PROGRESS`.
 */
@Singleton
class PendingSuspensionMonitor(
    private val workOrderService: WorkOrderService,
    private val workOrderDao: WorkOrderDao,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock,
    cacheEventBus: WorkshopCacheEventBus,
    private val appScope: CoroutineScope
) {
    @Inject
    constructor(
        workOrderService: WorkOrderService,
        workOrderDao: WorkOrderDao,
        sessionManager: SessionManager,
        gson: Gson,
        clock: Clock,
        cacheEventBus: WorkshopCacheEventBus
    ) : this(
        workOrderService = workOrderService,
        workOrderDao = workOrderDao,
        sessionManager = sessionManager,
        gson = gson,
        clock = clock,
        cacheEventBus = cacheEventBus,
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )
    private val mutex = Mutex()
    private val semaphore = Semaphore(MAX_CONCURRENCY)
    private val byOrderId = mutableMapOf<String, List<PendingSuspensionItem>>()
    private var lastBranchId: String? = null
    private var lastRefreshedAtMs: Long = 0L

    private val _items = MutableStateFlow<List<PendingSuspensionItem>>(emptyList())
    val items: StateFlow<List<PendingSuspensionItem>> = _items.asStateFlow()

    init {
        appScope.launch {
            sessionManager.session.collect { session ->
                if (session == null) clear()
            }
        }
        appScope.launch {
            cacheEventBus.events.collect { event ->
                when (event) {
                    is WorkshopCacheEvent.WorkOrderMutated -> refreshSingleOrder(event.workOrderId)
                    is WorkshopCacheEvent.VehicleDelivered -> event.workOrderId?.let(::removeOrder)
                }
            }
        }
    }

    fun updateFromDetail(detail: WorkOrderDetail) {
        val order = detail.workOrder
        val pending = detail.tasksAwaitingSuspensionReview.map { task ->
            PendingSuspensionItem(
                taskId = task.id.value,
                workOrderId = order.id,
                workOrderNumber = order.internalNumber,
                plate = order.plate,
                serviceName = task.serviceName,
                taskDescription = task.description,
                reason = task.pendingSuspensionReason ?: task.missingItemDescription,
                requestedAt = task.pendingSuspensionRequestedAt
            )
        }
        synchronized(byOrderId) {
            if (pending.isEmpty()) {
                byOrderId.remove(order.id)
            } else {
                byOrderId[order.id] = pending
            }
            publishLocked()
        }
    }

    suspend fun refreshForActiveBranch(force: Boolean = false) {
        val session = sessionManager.getSession() ?: return
        val branchId = session.branchId ?: return
        if (!session.has(Permission.TASKS_SUSPEND) || !session.has(Permission.WORK_ORDERS_READ)) {
            clear()
            return
        }
        val now = clock.millis()
        mutex.withLock {
            val branchChanged = lastBranchId != branchId
            if (!force && !branchChanged && (now - lastRefreshedAtMs) < TTL_MILLIS) return
            if (branchChanged) {
                synchronized(byOrderId) {
                    byOrderId.clear()
                    publishLocked()
                }
            }
            lastBranchId = branchId
            lastRefreshedAtMs = now
        }

        val branchOrders = workOrderDao.observeBranch(branchId).firstOrNull().orEmpty()
        val inProgressIds = branchOrders
            .filter { it.workOrder.status == WorkOrderStatus.IN_PROGRESS.name }
            .take(MAX_ORDERS_TO_INSPECT)
            .map { it.workOrder.id }

        val activeOrderIds = branchOrders.map { it.workOrder.id }.toSet()
        synchronized(byOrderId) {
            byOrderId.keys.retainAll(activeOrderIds)
            publishLocked()
        }

        if (inProgressIds.isEmpty()) return

        coroutineScope {
            inProgressIds.map { orderId ->
                async {
                    semaphore.withPermit {
                        fetchAndStoreOrder(orderId)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun refreshSingleOrder(workOrderId: String) {
        val session = sessionManager.getSession() ?: return
        if (!session.has(Permission.TASKS_SUSPEND) || !session.has(Permission.WORK_ORDERS_READ)) return
        semaphore.withPermit {
            fetchAndStoreOrder(workOrderId)
        }
    }

    private suspend fun fetchAndStoreOrder(workOrderId: String) {
        apiCall(gson, { workOrderService.getWorkOrder(workOrderId) }) { it }
            .onSuccess { dto ->
                val savedHeader = workOrderDao.saveHeaderOrDeleteClosed(dto, clock.millis())
                val plate = workOrderDao.observeById(workOrderId).firstOrNull()?.plate ?: savedHeader.plate
                val detail = dto.toDetail(savedHeader.copy(plate = plate))
                updateFromDetail(detail)
            }
    }

    private fun removeOrder(workOrderId: String) {
        synchronized(byOrderId) {
            if (byOrderId.remove(workOrderId) != null) {
                publishLocked()
            }
        }
    }

    fun clear() {
        synchronized(byOrderId) {
            byOrderId.clear()
            lastBranchId = null
            lastRefreshedAtMs = 0L
            publishLocked()
        }
    }

    private fun publishLocked() {
        val list = byOrderId.values.flatten()
            .sortedByDescending { it.requestedAt?.toEpochMilli() ?: 0L }
        _items.update { list }
    }

    companion object {
        const val TTL_MILLIS = 60_000L
        const val MAX_CONCURRENCY = 3
        const val MAX_ORDERS_TO_INSPECT = 15
    }
}
