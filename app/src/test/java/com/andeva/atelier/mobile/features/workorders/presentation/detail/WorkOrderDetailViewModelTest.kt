package com.andeva.atelier.mobile.features.workorders.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.bays.application.ReassignTaskMechanicUseCase
import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.tasks.application.AddPartToTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.RemovePartFromTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.SearchPartsUseCase
import com.andeva.atelier.mobile.features.tasks.application.UpdateTaskPartQuantityUseCase
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.workorders.application.AddWorkOrderTaskUseCase
import com.andeva.atelier.mobile.features.workorders.application.ApproveQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.DownloadQuotationPdfUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetBranchMechanicsUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.application.RejectQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.SendQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.UpdateWorkOrderTaskUseCase
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrderTask
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderTaskChanges
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.math.BigDecimal

class WorkOrderDetailViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<WorkOrderRepository>()
    private val bayRepository = mockk<BayRepository>(relaxed = true)
    private val taskRepository = mockk<TaskRepository>()
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }

    private val servicioAfinamiento = ServiceOption("s1", "Afinamiento mayor", Money(BigDecimal("180.00"), Money.PEN), 120)
    private val servicioFrenos = ServiceOption("s2", "Diagnóstico de frenos", Money(BigDecimal("60.00"), Money.PEN), 45)

    private val allPermissions = listOf(
        Permission.WORK_ORDERS_READ,
        Permission.WORK_ORDERS_UPDATE,
        Permission.TASKS_CHARGE_PARTS,
        Permission.PARTS_READ,
        Permission.QUOTATIONS_READ,
        Permission.QUOTATIONS_SEND,
        Permission.QUOTATIONS_APPROVE
    )

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u1",
        email = "asesor@taller.pe",
        fullName = "Asesor",
        tenantId = "t1",
        tenantName = "Taller",
        membershipId = "m1",
        permissions = permissions.map { it.value }.toSet(),
        plan = null
    )

    private fun repuesto(
        id: String,
        productId: String,
        name: String?,
        qty: String,
        unit: String,
        total: String
    ) = TaskProduct(
        id = id,
        productId = productId,
        productName = name,
        quantity = BigDecimal(qty),
        unitPrice = Money(BigDecimal(unit), Money.PEN),
        totalAmount = Money(BigDecimal(total), Money.PEN)
    )

    private fun tarea(
        id: String,
        status: TaskStatus = TaskStatus.ASSIGNED,
        serviceId: String? = "s1",
        serviceName: String? = "Standard Service",
        mechanic: String? = "Luis Mamani",
        price: String? = null,
        products: List<TaskProduct> = emptyList()
    ) = TaskDetail(
        id = TaskId(id),
        workOrderId = "wo1",
        serviceId = serviceId,
        serviceName = serviceName,
        mechanicId = "m1",
        mechanicName = mechanic,
        status = status,
        description = "Lectura de códigos",
        price = price?.let { Money(BigDecimal(it), Money.PEN) },
        holdReason = null,
        missingItemDescription = null,
        totalPausedSeconds = 0,
        startedAt = null,
        completedAt = null,
        pendingSuspensionReason = null,
        pendingSuspensionRequestedAt = null,
        products = products,
        evidenceImages = emptyList()
    )

    private fun orden(status: WorkOrderStatus = WorkOrderStatus.DRAFT) = WorkOrder(
        id = "wo1",
        internalNumber = 424,
        branchId = "b1",
        vehicleId = "v1",
        customerId = "c1",
        currentBayId = null,
        bayName = "Bahía 2",
        status = status,
        diagnosticSummary = null,
        mileageIn = 45000,
        subtotal = null,
        taxAmount = null,
        totalAmount = null,
        plate = "ABC-123",
        customerName = "Ana Torres"
    )

    private fun detalle(
        vararg tasks: TaskDetail,
        status: WorkOrderStatus = WorkOrderStatus.DRAFT
    ) = WorkOrderDetail(
        workOrder = orden(status),
        tasks = tasks.toList(),
        proposals = emptyList(),
        intakeImages = emptyList(),
        createdAt = null,
        updatedAt = null
    )

    private fun viewModel(
        permissions: List<Permission> = allPermissions,
        mechanics: List<MechanicOption> = emptyList()
    ): WorkOrderDetailViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        coEvery { repository.getServiceCatalog() } returns Result.success(listOf(servicioAfinamiento, servicioFrenos))
        coEvery { repository.getBranchMechanics() } returns Result.success(mechanics)
        coEvery { taskRepository.searchParts("") } returns Result.success(emptyList())
        return WorkOrderDetailViewModel(
            observeSession = observeSession,
            getDetail = GetWorkOrderDetailUseCase(repository),
            getServices = GetServiceCatalogUseCase(repository),
            addTask = AddWorkOrderTaskUseCase(repository),
            updateTask = UpdateWorkOrderTaskUseCase(repository),
            searchParts = SearchPartsUseCase(taskRepository),
            addPart = AddPartToTaskUseCase(taskRepository),
            updatePartQuantity = UpdateTaskPartQuantityUseCase(taskRepository),
            removePart = RemovePartFromTaskUseCase(taskRepository),
            downloadQuotationPdf = DownloadQuotationPdfUseCase(repository),
            sendQuotation = SendQuotationUseCase(repository),
            approveQuotation = ApproveQuotationUseCase(repository),
            rejectQuotation = RejectQuotationUseCase(repository),
            connectivity = connectivity,
            savedStateHandle = SavedStateHandle(mapOf("workOrderId" to "wo1")),
            getBranchMechanics = GetBranchMechanicsUseCase(repository),
            reassignTaskMechanic = ReassignTaskMechanicUseCase(bayRepository)
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `carga el detalle con las tareas de la orden`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns
            Result.success(detalle(tarea("t1", TaskStatus.IN_PROGRESS), tarea("t2", TaskStatus.ASSIGNED)))
        val vm = viewModel()
        settle()

        assertEquals(listOf("t1", "t2"), vm.uiState.value.detail?.tasks?.map { it.id.value })
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `un error del backend muestra su mensaje y se puede reintentar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returnsMany listOf(
            Result.failure(ApiException(404, "NOT_FOUND", "La orden no existe.")),
            Result.success(detalle(tarea("t1", TaskStatus.ASSIGNED)))
        )
        val vm = viewModel()
        settle()
        assertEquals("La orden no existe.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertNull(vm.uiState.value.detail)

        vm.load()
        settle()
        assertNull(vm.uiState.value.error)
        assertNotNull(vm.uiState.value.detail)
    }

    @Test
    fun `sin red muestra el aviso y al volver la red carga sola`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returnsMany listOf(
            Result.failure(IOException()),
            Result.success(detalle(tarea("t1", TaskStatus.ASSIGNED)))
        )
        online.value = false
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.error is UiText.Resource)
        assertNull(vm.uiState.value.detail)

        online.value = true
        settle()

        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.detail?.tasks?.size)
        coVerify(exactly = 2) { repository.getDetail("wo1") }
    }

    @Test
    fun `una orden sin tareas queda con la lista vacia`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.detail?.tasks?.isEmpty() == true)
    }

    // --- US22: cómputo automático en cascada de costos de tareas e impuestos ---

    @Test
    fun `US22 calcula en cascada el total por tarea, subtotal imponible, IGV 18 y total final sin error de redondeo`() =
        runTest(mainRule.dispatcher) {
            val t1 = tarea(
                id = "t1",
                serviceId = "s1",
                price = "180.00",
                products = listOf(
                    repuesto("tp1", "p1", "Bujías NGK", "4", "24.00", "96.00"),
                    repuesto("tp2", "p2", "Filtro de aire", "1", "35.00", "35.00")
                )
            )
            val t2 = tarea(
                id = "t2",
                serviceId = "s2",
                price = "60.00",
                products = listOf(
                    repuesto("tp3", "p3", "Aceite 5W-30", "4", "35.00", "140.00")
                )
            )
            coEvery { repository.getDetail("wo1") } returns Result.success(detalle(t1, t2))
            val vm = viewModel()
            settle()

            assertEquals(BigDecimal("311.00"), t1.cascadeTotal().amount)
            assertEquals(BigDecimal("200.00"), t2.cascadeTotal().amount)

            val breakdown = vm.uiState.value.breakdown!!
            assertEquals(BigDecimal("240.00"), breakdown.laborSubtotal.amount)
            assertEquals(BigDecimal("271.00"), breakdown.partsSubtotal.amount)
            assertEquals(BigDecimal("511.00"), breakdown.subtotal.amount)
            assertEquals(BigDecimal("91.98"), breakdown.taxAmount.amount)
            assertEquals(BigDecimal("602.98"), breakdown.totalAmount.amount)
            assertEquals("Afinamiento mayor", vm.uiState.value.taskTitle(t1))
        }

    @Test
    fun `US22 agrega una tarea con precio de mano de obra y recalcula los totales`() = runTest(mainRule.dispatcher) {
        val inicial = detalle(tarea("t1", price = "100.00"))
        val actualizado = detalle(tarea("t1", price = "100.00"), tarea("t2", serviceId = "s2", price = "60.00"))
        coEvery { repository.getDetail("wo1") } returns Result.success(inicial)
        coEvery {
            repository.addTask("wo1", NewWorkOrderTask("s2", "Diagnóstico de frenos", price = BigDecimal("60")))
        } returns Result.success(actualizado)

        val vm = viewModel()
        settle()

        vm.onOpenAddTask()
        vm.onSelectTaskService(servicioFrenos)
        vm.onConfirmAddTask()
        settle()

        assertNull(vm.uiState.value.addTaskForm)
        assertEquals(BigDecimal("160.00"), vm.uiState.value.breakdown?.subtotal?.amount)
        assertEquals(BigDecimal("28.80"), vm.uiState.value.breakdown?.taxAmount?.amount)
        assertEquals(BigDecimal("188.80"), vm.uiState.value.breakdown?.totalAmount?.amount)
    }

    @Test
    fun `US22 actualiza el precio de mano de obra de una tarea y recalcula en cascada`() = runTest(mainRule.dispatcher) {
        val t1 = tarea("t1", price = "100.00")
        val t1Actualizada = tarea("t1", price = "150.50")
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle(t1))
        coEvery {
            repository.updateTask("wo1", "t1", WorkOrderTaskChanges(price = BigDecimal("150.50")))
        } returns Result.success(detalle(t1Actualizada))

        val vm = viewModel()
        settle()

        vm.onOpenEditLabor(t1)
        vm.onEditLaborPriceChange("150,50")
        vm.onConfirmEditLabor()
        settle()

        assertNull(vm.uiState.value.editLaborForm)
        assertEquals(BigDecimal("150.50"), vm.uiState.value.breakdown?.subtotal?.amount)
        assertEquals(BigDecimal("27.09"), vm.uiState.value.breakdown?.taxAmount?.amount)
        assertEquals(BigDecimal("177.59"), vm.uiState.value.breakdown?.totalAmount?.amount)
    }

    // --- US23: imputación de repuestos a tareas y retiro con devolución al almacén ---

    @Test
    fun `US23 busca en el catalogo, bloquea agotados e imputa repuesto recargando la orden en cascada`() =
        runTest(mainRule.dispatcher) {
            val t1SinRepuestos = tarea("t1", price = "180.00")
            val t1ConBujias = tarea(
                "t1",
                price = "180.00",
                products = listOf(repuesto("tp1", "p1", "Product p1", "4", "24.00", "96.00"))
            )
            val disponible = PartOption("p1", "Bujías NGK", "SKU-1", "MOTOR", BigDecimal("24.00"), BigDecimal("10"), "UNIT")
            val agotado = PartOption("p2", "Filtro agotado", "SKU-2", "FILTERS", BigDecimal("35.00"), BigDecimal.ZERO, "UNIT")

            coEvery { repository.getDetail("wo1") } returnsMany listOf(
                Result.success(detalle(t1SinRepuestos)),
                Result.success(detalle(t1ConBujias))
            )
            coEvery { taskRepository.searchParts("bujia") } returns Result.success(listOf(disponible, agotado))
            coEvery { taskRepository.addPart(TaskId("t1"), "p1", BigDecimal("4")) } returns
                Result.success(repuesto("tp1", "p1", "Bujías NGK", "4", "24.00", "96.00"))

            val vm = viewModel()
            settle()

            vm.onOpenChargePart(TaskId("t1"))
            vm.onChargeQueryChange("bujia")
            settle()

            // No permite seleccionar un repuesto sin stock
            vm.onSelectPart(agotado)
            assertNull(vm.uiState.value.chargeForm?.selected)

            // Selecciona el disponible e imputa 4 unidades
            vm.onSelectPart(disponible)
            vm.onChargeQuantityChange("4")
            vm.onConfirmChargePart()
            settle()

            assertNull(vm.uiState.value.chargeForm)
            assertEquals(BigDecimal("276.00"), vm.uiState.value.breakdown?.subtotal?.amount)
            assertEquals("Bujías NGK", vm.uiState.value.productTitle(t1ConBujias.products.single()))
            coVerify(exactly = 1) { taskRepository.addPart(TaskId("t1"), "p1", BigDecimal("4")) }
        }

    @Test
    fun `US23 retirar un repuesto lo desvincula de la tarea y recalcula la orden a la baja`() =
        runTest(mainRule.dispatcher) {
            val pieza = repuesto("tp1", "p1", "Bujías NGK", "4", "24.00", "96.00")
            val conPieza = tarea("t1", price = "180.00", products = listOf(pieza))
            val sinPieza = tarea("t1", price = "180.00", products = emptyList())

            coEvery { repository.getDetail("wo1") } returnsMany listOf(
                Result.success(detalle(conPieza)),
                Result.success(detalle(sinPieza))
            )
            coEvery { taskRepository.removePart(TaskId("t1"), "p1") } returns Result.success(Unit)

            val vm = viewModel()
            settle()
            assertEquals(BigDecimal("276.00"), vm.uiState.value.breakdown?.subtotal?.amount)

            vm.onOpenReturnPart(TaskId("t1"), pieza)
            vm.onConfirmPartEdit()
            settle()

            assertNull(vm.uiState.value.partEdit)
            assertEquals(BigDecimal("180.00"), vm.uiState.value.breakdown?.subtotal?.amount)
            coVerify(exactly = 1) { taskRepository.removePart(TaskId("t1"), "p1") }
        }

    // --- US24: generación y remisión digital de proforma comercial en PDF ---

    @Test
    fun `US24 genera la proforma en PDF y la limpia tras abrir el menu de comparticion`() = runTest(mainRule.dispatcher) {
        val pdfFile = File("proforma-wo1.pdf")
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle(tarea("t1", price = "100.00")))
        coEvery { repository.downloadQuotationPdf("wo1") } returns Result.success(pdfFile)

        val vm = viewModel()
        settle()

        vm.onGenerateQuotationPdf()
        settle()

        assertEquals(pdfFile, vm.uiState.value.generatedPdf)
        assertEquals(R.string.quotation_pdf_ready, (vm.uiState.value.notice as UiText.Resource).id)

        vm.onPdfHandled()
        assertNull(vm.uiState.value.generatedPdf)
    }

    @Test
    fun `US24 envia la proforma por correo o muestra el mensaje del backend si el cliente no tiene correo`() =
        runTest(mainRule.dispatcher) {
            coEvery { repository.getDetail("wo1") } returns Result.success(detalle(tarea("t1", price = "100.00")))
            coEvery { repository.sendQuotation("wo1") } returnsMany listOf(
                Result.failure(ApiException(422, "NO_EMAIL", "El cliente no tiene correo registrado.")),
                Result.success("cliente@correo.pe")
            )

            val vm = viewModel()
            settle()

            vm.onSendQuotationEmail()
            settle()
            assertEquals("El cliente no tiene correo registrado.", (vm.uiState.value.error as UiText.Dynamic).value)

            vm.onSendQuotationEmail()
            settle()
            val notice = vm.uiState.value.notice as UiText.Resource
            assertEquals("cliente@correo.pe", notice.args.single())
        }

    // --- US25: formalización de resolución de presupuesto tras comunicación externa ---

    @Test
    fun `US25 aceptar presupuesto pasa la orden a IN_PROGRESS y avisa que los tecnicos pueden empezar`() =
        runTest(mainRule.dispatcher) {
            coEvery { repository.getDetail("wo1") } returns Result.success(detalle(tarea("t1", price = "180.00")))
            coEvery { repository.approveQuotation("wo1") } returns Result.success(orden(WorkOrderStatus.IN_PROGRESS))

            val vm = viewModel()
            settle()

            vm.onApproveQuotation()
            settle()

            assertEquals(WorkOrderStatus.IN_PROGRESS, vm.uiState.value.status)
            assertEquals(R.string.quotation_approved_notice, (vm.uiState.value.notice as UiText.Resource).id)
            coVerify(exactly = 1) { repository.approveQuotation("wo1") }
        }

    @Test
    fun `US25 rechazar presupuesto exige motivo obligatorio y pasa la orden a CANCELED`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle(tarea("t1", price = "180.00")))
        coEvery { repository.rejectQuotation("wo1", "Precio fuera de presupuesto") } returns
            Result.success(orden(WorkOrderStatus.CANCELED))

        val vm = viewModel()
        settle()

        vm.onOpenRejectQuotation()
        vm.onRejectReasonChange("   ")
        vm.onConfirmRejectQuotation()
        settle()

        assertEquals("reason", vm.uiState.value.invalidField)
        coVerify(exactly = 0) { repository.rejectQuotation(any(), any()) }

        vm.onRejectReasonChange("  Precio fuera de presupuesto ")
        vm.onConfirmRejectQuotation()
        settle()

        assertNull(vm.uiState.value.rejectForm)
        assertEquals(WorkOrderStatus.CANCELED, vm.uiState.value.status)
        assertEquals(R.string.quotation_rejected_notice, (vm.uiState.value.notice as UiText.Resource).id)
    }

    @Test
    fun `sin permisos de cotizacion no permite descargar PDF ni resolver presupuesto`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle(tarea("t1", price = "100.00")))
        val vm = viewModel(permissions = listOf(Permission.WORK_ORDERS_READ))
        settle()

        assertFalse(vm.uiState.value.canGeneratePdf)
        assertFalse(vm.uiState.value.canResolveQuotation)

        vm.onGenerateQuotationPdf()
        vm.onApproveQuotation()
        settle()

        coVerify(exactly = 0) { repository.downloadQuotationPdf(any()) }
        coVerify(exactly = 0) { repository.approveQuotation(any()) }
    }

    @Test
    fun `US22 permite agregar una tarea con mecanico asignado`() = runTest(mainRule.dispatcher) {
        val inicial = detalle(tarea("t1", price = "100.00"))
        val mec = MechanicOption("m-123", "Técnico especialista", "ACTIVE")
        val actualizado = detalle(
            tarea("t1", price = "100.00"),
            tarea("t2", serviceId = "s2", mechanic = "Técnico especialista", price = "60.00")
        )
        coEvery { repository.getDetail("wo1") } returns Result.success(inicial)
        coEvery {
            repository.addTask("wo1", NewWorkOrderTask("s2", "Diagnóstico de frenos", mechanicId = "m-123", price = BigDecimal("60")))
        } returns Result.success(actualizado)

        val vm = viewModel(mechanics = listOf(mec))
        settle()

        vm.onOpenAddTask()
        vm.onSelectTaskService(servicioFrenos)
        vm.onSelectAddTaskMechanic("m-123")
        vm.onConfirmAddTask()
        settle()

        assertNull(vm.uiState.value.addTaskForm)
        coVerify {
            repository.addTask("wo1", NewWorkOrderTask("s2", "Diagnóstico de frenos", mechanicId = "m-123", price = BigDecimal("60")))
        }
    }

    @Test
    fun `US22 reasigna mecanico a una tarea existente usando reassignTaskMechanic`() = runTest(mainRule.dispatcher) {
        val t1 = tarea("t1", mechanic = "Luis Mamani")
        val inicial = detalle(t1)
        val mec = MechanicOption("m-999", "Carlos Electricista", "ACTIVE")
        val recargado = detalle(tarea("t1", mechanic = "Carlos Electricista"))

        coEvery { repository.getDetail("wo1") } returnsMany listOf(
            Result.success(inicial),
            Result.success(recargado)
        )
        coEvery { bayRepository.reassignMechanic("t1", "m-999") } returns Result.success(Unit)

        val vm = viewModel(mechanics = listOf(mec))
        settle()

        vm.onOpenAssignMechanic(t1)
        assertEquals(t1, vm.uiState.value.assigningMechanicTask)

        vm.onConfirmAssignMechanic(mec)
        settle()

        assertNull(vm.uiState.value.assigningMechanicTask)
        coVerify { bayRepository.reassignMechanic("t1", "m-999") }
        assertEquals("Carlos Electricista", vm.uiState.value.detail?.tasks?.first()?.mechanicName)
    }

    @Test
    fun `mechanicNameFor resuelve el nombre de availableMechanics por membershipId`() {
        val mec = MechanicOption("m-777", "Técnico mecánico", "ACTIVE", name = "Juan Pérez")
        val state = WorkOrderDetailUiState(availableMechanics = listOf(mec))
        val taskWithId = tarea("t1", mechanic = null).copy(mechanicId = "m-777")
        val taskWithRawName = tarea("t2", mechanic = "Mecánico Original").copy(mechanicId = "m-unknown")

        assertEquals("Juan Pérez", state.mechanicNameFor(taskWithId))
        assertEquals("Mecánico Original", state.mechanicNameFor(taskWithRawName))
    }
}
