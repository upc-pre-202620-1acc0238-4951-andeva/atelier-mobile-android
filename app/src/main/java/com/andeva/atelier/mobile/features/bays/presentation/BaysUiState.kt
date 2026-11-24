package com.andeva.atelier.mobile.features.bays.presentation

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.bays.domain.BayBoardItem
import com.andeva.atelier.mobile.features.bays.domain.BayOccupant
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus

/**
 * Tablero de bahías de la sede. Se pinta desde Room; reasignar, liberar, poner en mantenimiento, restaurar y
 * crear bahías son acciones en línea (no se encolan), por eso [isOnline] las habilita.
 */
data class BaysUiState(
    val board: List<BayBoardItem> = emptyList(),
    val activeOrders: List<WorkOrder> = emptyList(),
    val availableMechanics: List<MechanicOption> = emptyList(),
    val reassigningTaskId: String? = null,
    val statusFilter: BayStatus? = null,
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    val isOnline: Boolean = true,
    val isAssigning: Boolean = false,
    val targetBayForAssign: WorkBay? = null,
    val selectedBayItem: BayBoardItem? = null,
    val isUpdatingBay: Boolean = false,
    val showMaintenanceInput: Boolean = false,
    val maintenanceReason: String = "",
    val invalidField: String? = null,
    val canReadBays: Boolean = false,
    val canReadWorkOrders: Boolean = false,
    val canAssignBay: Boolean = false,
    val canReleaseBay: Boolean = false,
    val canManageBays: Boolean = false,
    val showCreateDialog: Boolean = false,
    val isCreatingBay: Boolean = false,
    val error: UiText? = null,
    val notice: UiText? = null
) {
    val filteredBoard: List<BayBoardItem>
        get() = if (statusFilter == null) board else board.filter { it.bay.status == statusFilter }

    val availableCount: Int get() = board.count { it.bay.status == BayStatus.AVAILABLE }
    val occupiedCount: Int get() = board.count { it.bay.status == BayStatus.OCCUPIED }
    val maintenanceCount: Int get() = board.count { it.bay.status == BayStatus.MAINTENANCE }

    val availableBays: List<WorkBay>
        get() = board.map { it.bay }.filter { it.status == BayStatus.AVAILABLE }

    val assignableOrders: List<WorkOrder>
        get() = activeOrders.filter {
            it.status == WorkOrderStatus.IN_PROGRESS || it.status == WorkOrderStatus.DRAFT
        }

    val hasOpenSheet: Boolean
        get() = targetBayForAssign != null || selectedBayItem != null || showCreateDialog

    /** Esqueleto solo sin caché: primera carga y ninguna bahía que pintar todavía. */
    val showSkeleton: Boolean get() = !hasLoaded && board.isEmpty()

    fun mechanicNameFor(mechanicId: String?, fallbackName: String? = null): String? {
        if (mechanicId == null) return fallbackName?.takeIf { it.isNotBlank() }
        return availableMechanics.firstOrNull { it.membershipId == mechanicId }?.displayName
            ?: fallbackName?.takeIf { it.isNotBlank() }
    }

    fun mechanicLabelsFor(occupant: BayOccupant): List<String> {
        val fromIds = occupant.mechanicIds.mapNotNull { id ->
            availableMechanics.firstOrNull { it.membershipId == id }?.displayName
        }
        if (fromIds.isNotEmpty()) return fromIds.distinct()
        return occupant.tasks.mapNotNull { it.mechanicName?.takeIf { n -> n.isNotBlank() } }.distinct()
    }
}
