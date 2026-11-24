package com.andeva.atelier.mobile.features.workorders.presentation.list

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkOrdersUiStateTest {

    private fun orden(id: String, plate: String?, status: WorkOrderStatus) = WorkOrder(
        id = id, internalNumber = null, branchId = "b1", vehicleId = "v$id", customerId = "c$id", currentBayId = null,
        bayName = null, status = status, diagnosticSummary = null, mileageIn = null, subtotal = null, taxAmount = null,
        totalAmount = null, plate = plate, customerName = null
    )

    private val state = WorkOrdersUiState(
        isLoaded = true,
        orders = listOf(
            orden("1", "BKR418", WorkOrderStatus.DRAFT),
            orden("2", "F5U731", WorkOrderStatus.IN_PROGRESS),
            orden("3", null, WorkOrderStatus.IN_PROGRESS),
            orden("4", "D2C604", WorkOrderStatus.COMPLETED)
        )
    )

    @Test
    fun sinFiltroMuestraTodas() {
        assertEquals(listOf("1", "2", "3", "4"), state.visibleOrders.map { it.id })
    }

    @Test
    fun filtraPorEstado() {
        assertEquals(listOf("2", "3"), state.copy(statusFilter = WorkOrderStatus.IN_PROGRESS).visibleOrders.map { it.id })
        assertEquals(listOf("4"), state.copy(statusFilter = WorkOrderStatus.COMPLETED).visibleOrders.map { it.id })
    }

    @Test
    fun buscaPlacaSinGuionNiMayusculas() {
        assertEquals(listOf("1"), state.copy(query = "bkr-418").visibleOrders.map { it.id })
    }

    @Test
    fun combinaEstadoYPlaca() {
        val result = state.copy(statusFilter = WorkOrderStatus.DRAFT, query = "f5u").visibleOrders
        assertEquals(emptyList<String>(), result.map { it.id })
    }
}
