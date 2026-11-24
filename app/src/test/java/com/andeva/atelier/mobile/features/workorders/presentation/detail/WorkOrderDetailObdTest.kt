package com.andeva.atelier.mobile.features.workorders.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Plan
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.tasks.application.AddPartToTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.RemovePartFromTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.SearchPartsUseCase
import com.andeva.atelier.mobile.features.tasks.application.UpdateTaskPartQuantityUseCase
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.workorders.application.AddWorkOrderTaskUseCase
import com.andeva.atelier.mobile.features.workorders.application.ApproveQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.DownloadQuotationPdfUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.application.RejectQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.SendQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.UpdateWorkOrderTaskUseCase
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkOrderDetailObdTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val workOrderRepo = mockk<WorkOrderRepository>(relaxed = true)
    private val taskRepo = mockk<TaskRepository>(relaxed = true)
    private val connectivity = mockk<ConnectivityObserver> {
        every { isOnline } returns MutableStateFlow(true)
    }

    private fun createViewModel(permissions: List<Permission>, iotEnabled: Boolean): WorkOrderDetailViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        val session = Session(
            userId = "u1", email = "asesor@taller.pe", fullName = "Asesor",
            tenantId = "t1", tenantName = "Taller", membershipId = "m1",
            permissions = permissions.map { it.value }.toSet(),
            plan = Plan("PRO", "ACTIVE", accessGranted = true, iotTelemetryEnabled = iotEnabled, aiDiagnosticsEnabled = false)
        )
        every { observeSession() } returns flowOf(session)
        coEvery { workOrderRepo.getServiceCatalog() } returns Result.success(emptyList())
        coEvery { workOrderRepo.getDetail("wo1") } returns Result.success(
            WorkOrderDetail(
                workOrder = WorkOrder(
                    id = "wo1", internalNumber = 10, branchId = "b1", vehicleId = "v1",
                    customerId = "c1", currentBayId = null, bayName = null,
                    status = WorkOrderStatus.IN_PROGRESS, diagnosticSummary = null,
                    mileageIn = 50000, subtotal = null, taxAmount = null, totalAmount = null,
                    plate = "BKL482", customerName = "Ana"
                ),
                tasks = emptyList(), proposals = emptyList(), intakeImages = emptyList(),
                createdAt = null, updatedAt = null
            )
        )
        return WorkOrderDetailViewModel(
            observeSession = observeSession,
            getDetail = GetWorkOrderDetailUseCase(workOrderRepo),
            getServices = GetServiceCatalogUseCase(workOrderRepo),
            addTask = AddWorkOrderTaskUseCase(workOrderRepo),
            updateTask = UpdateWorkOrderTaskUseCase(workOrderRepo),
            searchParts = SearchPartsUseCase(taskRepo),
            addPart = AddPartToTaskUseCase(taskRepo),
            updatePartQuantity = UpdateTaskPartQuantityUseCase(taskRepo),
            removePart = RemovePartFromTaskUseCase(taskRepo),
            downloadQuotationPdf = DownloadQuotationPdfUseCase(workOrderRepo),
            sendQuotation = SendQuotationUseCase(workOrderRepo),
            approveQuotation = ApproveQuotationUseCase(workOrderRepo),
            rejectQuotation = RejectQuotationUseCase(workOrderRepo),
            connectivity = connectivity,
            savedStateHandle = SavedStateHandle(mapOf("workOrderId" to "wo1"))
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `boton OBD se habilita solo con permiso de telemetria y plan IoT activo`() = runTest(mainRule.dispatcher) {
        val allowed = createViewModel(listOf(Permission.WORK_ORDERS_READ, Permission.TELEMETRY_READ), iotEnabled = true)
        settle()
        assertTrue(allowed.uiState.value.canOpenObd)

        val ingestOnly = createViewModel(listOf(Permission.WORK_ORDERS_READ, Permission.TELEMETRY_INGEST), iotEnabled = true)
        settle()
        assertTrue(ingestOnly.uiState.value.canOpenObd)

        val blockedByPlan = createViewModel(listOf(Permission.WORK_ORDERS_READ, Permission.TELEMETRY_READ), iotEnabled = false)
        settle()
        assertFalse(blockedByPlan.uiState.value.canOpenObd)

        val noPermission = createViewModel(listOf(Permission.WORK_ORDERS_READ), iotEnabled = true)
        settle()
        assertFalse(noPermission.uiState.value.canOpenObd)
    }
}
