package com.andeva.atelier.mobile.features.workorders.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.tasks.application.AddPartToTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.RemovePartFromTaskUseCase
import com.andeva.atelier.mobile.features.tasks.application.SearchPartsUseCase
import com.andeva.atelier.mobile.features.tasks.application.UpdateTaskPartQuantityUseCase
import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.presentation.detail.ChargeForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.PartEditForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.PartEditMode
import com.andeva.atelier.mobile.features.tasks.presentation.invalidTaskField
import com.andeva.atelier.mobile.features.tasks.presentation.toTaskUiText
import com.andeva.atelier.mobile.features.workorders.application.AddWorkOrderTaskUseCase
import com.andeva.atelier.mobile.features.workorders.application.ApproveQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.DownloadQuotationPdfUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.application.RejectQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.SendQuotationUseCase
import com.andeva.atelier.mobile.features.workorders.application.UpdateWorkOrderTaskUseCase
import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrderTask
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderTaskChanges
import com.andeva.atelier.mobile.features.workorders.presentation.invalidWorkOrderField
import com.andeva.atelier.mobile.features.workorders.presentation.toWorkOrderUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Detalle en línea y proforma comercial de la orden (`GET /work-orders/{id}`):
 * - **US22**: cómputo automático en cascada por tarea (mano de obra + repuestos) y de la orden (Subtotal, IGV 18 %, Total),
 *   más adición de tareas y edición de mano de obra.
 * - **US23 / US33**: imputación de repuestos a tareas con validación de stock, ajuste de cantidad y devolución al almacén.
 * - **US24**: descarga de la proforma comercial en PDF (`cacheDir/pdfs`) para compartición nativa y envío por correo.
 * - **US25**: formalización de la respuesta del cliente (aceptar e iniciar trabajos o rechazar con motivo y cancelar).
 */
@HiltViewModel
class WorkOrderDetailViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getDetail: GetWorkOrderDetailUseCase,
    private val getServices: GetServiceCatalogUseCase,
    private val addTask: AddWorkOrderTaskUseCase,
    private val updateTask: UpdateWorkOrderTaskUseCase,
    private val searchParts: SearchPartsUseCase,
    private val addPart: AddPartToTaskUseCase,
    private val updatePartQuantity: UpdateTaskPartQuantityUseCase,
    private val removePart: RemovePartFromTaskUseCase,
    private val downloadQuotationPdf: DownloadQuotationPdfUseCase,
    private val sendQuotation: SendQuotationUseCase,
    private val approveQuotation: ApproveQuotationUseCase,
    private val rejectQuotation: RejectQuotationUseCase,
    connectivity: ConnectivityObserver,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val workOrderId: String = savedStateHandle["workOrderId"] ?: ""

    private val _uiState = MutableStateFlow(WorkOrderDetailUiState())
    val uiState: StateFlow<WorkOrderDetailUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                val canReadParts = session?.has(Permission.PARTS_READ) == true
                _uiState.update {
                    it.copy(
                        canUpdateWorkOrder = session?.has(Permission.WORK_ORDERS_UPDATE) == true,
                        canChargeParts = session?.has(Permission.TASKS_CHARGE_PARTS) == true ||
                            session?.has(Permission.WORK_ORDERS_UPDATE) == true,
                        canReadParts = canReadParts,
                        canReadQuotation = session?.has(Permission.QUOTATIONS_READ) == true,
                        canSendQuotation = session?.has(Permission.QUOTATIONS_SEND) == true,
                        canApproveQuotation = session?.has(Permission.QUOTATIONS_APPROVE) == true
                    )
                }
                if (canReadParts && _uiState.value.partNamesById.isEmpty()) {
                    preloadPartNames()
                }
            }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val wasOfflineWithoutDetail = online && _uiState.value.detail == null && !_uiState.value.isLoading
                _uiState.update { it.copy(isOnline = online) }
                if (wasOfflineWithoutDetail) load()
            }
        }
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val servicesResult = getServices()
            val detailResult = getDetail(workOrderId)
            val services = servicesResult.getOrDefault(_uiState.value.services)
            detailResult
                .onSuccess { detail ->
                    _uiState.update { it.copy(isLoading = false, detail = detail, services = services) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, services = services, error = e.toWorkOrderUiText()) }
                }
        }
    }

    /** H11/H36: precarga nombres comerciales del catálogo si la sesión tiene `inventory:parts:read`. */
    private fun preloadPartNames() {
        viewModelScope.launch {
            searchParts("").onSuccess { parts ->
                val map = parts.associate { it.id to it.name }
                _uiState.update { it.copy(partNamesById = it.partNamesById + map) }
            }
        }
    }

    // --- US22 E1: agregar una tarea a la orden ---

    fun onOpenAddTask() {
        if (!_uiState.value.canModifyTasks) return
        _uiState.update {
            it.copy(addTaskForm = AddTaskForm(), notice = null, error = null, invalidField = null)
        }
    }

    fun onDismissAddTask() = _uiState.update {
        if (it.isBusy) it else it.copy(addTaskForm = null, error = null, invalidField = null)
    }

    fun onSelectTaskService(service: ServiceOption) = editAddTask { form ->
        val defaultPrice = service.basePrice?.amount?.stripTrailingZeros()?.toPlainString().orEmpty()
        form.copy(
            serviceId = service.id,
            description = form.description.ifBlank { service.name },
            price = form.price.ifBlank { defaultPrice }
        )
    }

    fun onAddTaskDescriptionChange(value: String) = editAddTask { it.copy(description = value) }

    fun onAddTaskPriceChange(value: String) = editAddTask { it.copy(price = value) }

    private fun editAddTask(change: (AddTaskForm) -> AddTaskForm) = _uiState.update {
        val form = it.addTaskForm
        if (form == null || it.isBusy) it else it.copy(addTaskForm = change(form), error = null, invalidField = null)
    }

    fun onConfirmAddTask() {
        val state = _uiState.value
        val form = state.addTaskForm ?: return
        if (!state.canModifyTasks) return
        val serviceId = form.serviceId
        if (serviceId.isNullOrBlank()) {
            failWorkOrder(InvalidWorkOrderInputException("serviceId"))
            return
        }
        val price = if (form.price.isBlank()) null else form.price.toDecimalAmount()
        if (form.price.isNotBlank() && price == null) {
            failWorkOrder(InvalidWorkOrderInputException("price"))
            return
        }
        _uiState.update { it.copy(busyAction = WorkOrderDetailAction.ADD_TASK, error = null, invalidField = null) }
        viewModelScope.launch {
            addTask(workOrderId, NewWorkOrderTask(serviceId = serviceId, description = form.description.trim(), price = price))
                .onSuccess { updatedDetail ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            addTaskForm = null,
                            detail = updatedDetail,
                            notice = UiText.Resource(R.string.order_task_added)
                        )
                    }
                }
                .onFailure(::failWorkOrder)
        }
    }

    // --- US22 E2: editar mano de obra de una tarea ---

    fun onOpenEditLabor(task: TaskDetail) {
        val state = _uiState.value
        if (!state.canModifyTasks) return
        val currentPrice = task.price?.amount?.stripTrailingZeros()?.toPlainString().orEmpty()
        _uiState.update {
            it.copy(
                editLaborForm = EditLaborForm(task.id, state.taskTitle(task), currentPrice),
                notice = null,
                error = null,
                invalidField = null
            )
        }
    }

    fun onDismissEditLabor() = _uiState.update {
        if (it.isBusy) it else it.copy(editLaborForm = null, error = null, invalidField = null)
    }

    fun onEditLaborPriceChange(value: String) = _uiState.update {
        val form = it.editLaborForm
        if (form == null || it.isBusy) it
        else it.copy(editLaborForm = form.copy(price = value), error = null, invalidField = null)
    }

    fun onConfirmEditLabor() {
        val state = _uiState.value
        val form = state.editLaborForm ?: return
        if (!state.canModifyTasks) return
        val price = form.price.toDecimalAmount()
        if (price == null) {
            failWorkOrder(InvalidWorkOrderInputException("price"))
            return
        }
        _uiState.update { it.copy(busyAction = WorkOrderDetailAction.EDIT_LABOR, error = null, invalidField = null) }
        viewModelScope.launch {
            updateTask(workOrderId, form.taskId.value, WorkOrderTaskChanges(price = price))
                .onSuccess { updatedDetail ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            editLaborForm = null,
                            detail = updatedDetail,
                            notice = UiText.Resource(R.string.order_task_updated)
                        )
                    }
                }
                .onFailure(::failWorkOrder)
        }
    }

    // --- US23: imputar repuesto a una tarea y recalcular en cascada ---

    fun onOpenChargePart(taskId: TaskId) {
        if (!_uiState.value.canAddPartToTask) return
        _uiState.update {
            it.copy(
                chargeTaskId = taskId,
                chargeForm = ChargeForm(),
                notice = null,
                error = null,
                invalidField = null
            )
        }
    }

    fun onDismissChargePart() {
        searchJob?.cancel()
        _uiState.update {
            if (it.isBusy) it else it.copy(chargeTaskId = null, chargeForm = null, error = null, invalidField = null)
        }
    }

    private fun editCharge(change: (ChargeForm) -> ChargeForm) = _uiState.update {
        val form = it.chargeForm
        if (form == null || it.isBusy) it else it.copy(chargeForm = change(form), error = null, invalidField = null)
    }

    fun onChargeQueryChange(value: String) {
        searchJob?.cancel()
        if (value.isBlank()) {
            editCharge { it.copy(query = value, selected = null, results = emptyList(), isSearching = false) }
            return
        }
        editCharge { it.copy(query = value, selected = null) }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            editCharge { it.copy(isSearching = true) }
            searchParts(value.trim())
                .onSuccess { parts ->
                    val names = parts.associate { it.id to it.name }
                    _uiState.update { state ->
                        state.copy(
                            partNamesById = state.partNamesById + names,
                            chargeForm = state.chargeForm?.copy(results = parts, isSearching = false)
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(chargeForm = it.chargeForm?.copy(isSearching = false), error = e.toTaskUiText())
                    }
                }
        }
    }

    fun onSelectPart(part: PartOption) {
        if (!part.isAvailable) return
        _uiState.update { state ->
            val form = state.chargeForm ?: return@update state
            if (state.isBusy) state
            else state.copy(
                partNamesById = state.partNamesById + (part.id to part.name),
                chargeForm = form.copy(selected = part),
                error = null,
                invalidField = null
            )
        }
    }

    fun onChargeQuantityChange(value: String) = editCharge { it.copy(quantity = value) }

    fun onConfirmChargePart() {
        val state = _uiState.value
        val taskId = state.chargeTaskId ?: return
        val form = state.chargeForm ?: return
        val part = form.selected ?: return
        if (!state.canAddPartToTask) return
        val quantity = form.quantity.toDecimalAmount()
        if (quantity == null) {
            failTask(InvalidTaskInputException("quantity"))
            return
        }
        _uiState.update { it.copy(busyAction = WorkOrderDetailAction.CHARGE_PART, error = null, invalidField = null) }
        viewModelScope.launch {
            addPart(taskId, part.id, quantity)
                .onSuccess { reloadOrderAfterPartChange(UiText.Resource(R.string.task_part_charged)) }
                .onFailure(::failTask)
        }
    }

    // --- US23 / US33: corregir cantidad o retirar pieza imputada ---

    fun onOpenEditPart(taskId: TaskId, product: TaskProduct) =
        openPartEdit(taskId, product, PartEditMode.EDIT_QUANTITY)

    fun onOpenReturnPart(taskId: TaskId, product: TaskProduct) =
        openPartEdit(taskId, product, PartEditMode.RETURN)

    private fun openPartEdit(taskId: TaskId, product: TaskProduct, mode: PartEditMode) {
        val state = _uiState.value
        if (!state.canEditTaskParts) return
        _uiState.update {
            it.copy(
                editingPartTaskId = taskId,
                partEdit = PartEditForm(product, mode, product.quantity.stripTrailingZeros().toPlainString()),
                notice = null,
                error = null,
                invalidField = null
            )
        }
    }

    fun onDismissPartEdit() = _uiState.update {
        if (it.isBusy) it else it.copy(editingPartTaskId = null, partEdit = null, error = null, invalidField = null)
    }

    fun onPartEditQuantityChange(value: String) = _uiState.update {
        val edit = it.partEdit
        if (edit == null || it.isBusy) it
        else it.copy(partEdit = edit.copy(quantity = value), error = null, invalidField = null)
    }

    fun onConfirmPartEdit() {
        val state = _uiState.value
        val taskId = state.editingPartTaskId ?: return
        val edit = state.partEdit ?: return
        if (!state.canEditTaskParts) return
        when (edit.mode) {
            PartEditMode.RETURN -> {
                _uiState.update { it.copy(busyAction = WorkOrderDetailAction.REMOVE_PART, error = null, invalidField = null) }
                viewModelScope.launch {
                    removePart(taskId, edit.product.productId)
                        .onSuccess { reloadOrderAfterPartChange(UiText.Resource(R.string.task_part_returned)) }
                        .onFailure(::failTask)
                }
            }
            PartEditMode.EDIT_QUANTITY -> {
                val quantity = edit.quantity.toDecimalAmount()
                if (quantity == null) {
                    failTask(InvalidTaskInputException("quantity"))
                    return
                }
                _uiState.update { it.copy(busyAction = WorkOrderDetailAction.EDIT_PART, error = null, invalidField = null) }
                viewModelScope.launch {
                    updatePartQuantity(taskId, edit.product.productId, quantity)
                        .onSuccess { reloadOrderAfterPartChange(UiText.Resource(R.string.task_part_updated)) }
                        .onFailure(::failTask)
                }
            }
        }
    }

    private suspend fun reloadOrderAfterPartChange(notice: UiText) {
        getDetail(workOrderId)
            .onSuccess { updated ->
                _uiState.update {
                    it.copy(
                        busyAction = null,
                        chargeTaskId = null,
                        chargeForm = null,
                        editingPartTaskId = null,
                        partEdit = null,
                        detail = updated,
                        notice = notice
                    )
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        busyAction = null,
                        chargeTaskId = null,
                        chargeForm = null,
                        editingPartTaskId = null,
                        partEdit = null,
                        notice = notice,
                        error = e.toWorkOrderUiText()
                    )
                }
            }
    }

    // --- US24: generar proforma en PDF y enviar por correo ---

    fun onGenerateQuotationPdf() {
        if (!_uiState.value.canGeneratePdf) return
        _uiState.update {
            it.copy(busyAction = WorkOrderDetailAction.DOWNLOAD_PDF, notice = null, error = null, invalidField = null)
        }
        viewModelScope.launch {
            downloadQuotationPdf(workOrderId)
                .onSuccess { file ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            generatedPdf = file,
                            notice = UiText.Resource(R.string.quotation_pdf_ready)
                        )
                    }
                }
                .onFailure(::failWorkOrder)
        }
    }

    fun onPdfHandled() = _uiState.update { it.copy(generatedPdf = null) }

    fun onPdfOpenFailed() = _uiState.update {
        it.copy(generatedPdf = null, error = UiText.Resource(R.string.quotation_no_pdf_app))
    }

    fun onSendQuotationEmail() {
        if (!_uiState.value.canEmailQuotation) return
        _uiState.update {
            it.copy(busyAction = WorkOrderDetailAction.SEND_EMAIL, notice = null, error = null, invalidField = null)
        }
        viewModelScope.launch {
            sendQuotation(workOrderId)
                .onSuccess { email ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            proformaSentLabel = email,
                            notice = UiText.Resource(R.string.quotation_sent_notice, email)
                        )
                    }
                }
                .onFailure(::failWorkOrder)
        }
    }

    // --- US25: formalización de la respuesta del cliente (aceptar o rechazar presupuesto) ---

    fun onApproveQuotation() {
        if (!_uiState.value.canResolveQuotation) return
        _uiState.update {
            it.copy(busyAction = WorkOrderDetailAction.APPROVE_QUOTATION, notice = null, error = null, invalidField = null)
        }
        viewModelScope.launch {
            approveQuotation(workOrderId)
                .onSuccess { updatedOrder ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            detail = it.detail?.copy(workOrder = updatedOrder),
                            notice = UiText.Resource(R.string.quotation_approved_notice)
                        )
                    }
                }
                .onFailure(::failWorkOrder)
        }
    }

    fun onOpenRejectQuotation() {
        if (!_uiState.value.canResolveQuotation) return
        _uiState.update {
            it.copy(rejectForm = RejectQuotationForm(), notice = null, error = null, invalidField = null)
        }
    }

    fun onDismissRejectQuotation() = _uiState.update {
        if (it.isBusy) it else it.copy(rejectForm = null, error = null, invalidField = null)
    }

    fun onRejectReasonChange(value: String) = _uiState.update {
        val form = it.rejectForm
        if (form == null || it.isBusy) it
        else it.copy(rejectForm = form.copy(reason = value), error = null, invalidField = null)
    }

    fun onConfirmRejectQuotation() {
        val state = _uiState.value
        val form = state.rejectForm ?: return
        if (!state.canResolveQuotation) return
        _uiState.update {
            it.copy(busyAction = WorkOrderDetailAction.REJECT_QUOTATION, error = null, invalidField = null)
        }
        viewModelScope.launch {
            rejectQuotation(workOrderId, form.reason.trim())
                .onSuccess { canceledOrder ->
                    _uiState.update {
                        it.copy(
                            busyAction = null,
                            rejectForm = null,
                            detail = it.detail?.copy(workOrder = canceledOrder),
                            notice = UiText.Resource(R.string.quotation_rejected_notice)
                        )
                    }
                }
                .onFailure(::failWorkOrder)
        }
    }

    private fun String.toDecimalAmount(): BigDecimal? = trim().replace(',', '.').toBigDecimalOrNull()

    private fun failWorkOrder(error: Throwable) = _uiState.update {
        it.copy(
            busyAction = null,
            error = error.toWorkOrderUiText(),
            invalidField = error.invalidWorkOrderField()
        )
    }

    private fun failTask(error: Throwable) = _uiState.update {
        it.copy(
            busyAction = null,
            error = error.toTaskUiText(),
            invalidField = error.invalidTaskField()
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
