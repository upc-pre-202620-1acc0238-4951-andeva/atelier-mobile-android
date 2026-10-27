package com.andeva.atelier.mobile.features.workorders.presentation.proposals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.ApproveProposalUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetServiceCatalogUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.application.NotifyProposalUseCase
import com.andeva.atelier.mobile.features.workorders.application.RejectProposalUseCase
import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.ProposalApproval
import com.andeva.atelier.mobile.features.workorders.presentation.invalidWorkOrderField
import com.andeva.atelier.mobile.features.workorders.presentation.toWorkOrderUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * US15: panel del asesor con las propuestas de la orden. Notifica al cliente por correo y registra su decisión:
 * aprobada (el backend crea la tarea y recalcula el presupuesto) o rechazada con sus observaciones. Todo en línea.
 */
@HiltViewModel
class ProposalsPanelViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val getDetail: GetWorkOrderDetailUseCase,
    private val getServices: GetServiceCatalogUseCase,
    private val notifyProposal: NotifyProposalUseCase,
    private val approveProposal: ApproveProposalUseCase,
    private val rejectProposal: RejectProposalUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workOrderId: String = savedStateHandle["workOrderId"] ?: ""

    private val _uiState = MutableStateFlow(ProposalsPanelUiState())
    val uiState: StateFlow<ProposalsPanelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canNotify = session?.has(Permission.PROPOSALS_NOTIFY) == true,
                        canResolve = session?.has(Permission.PROPOSALS_RESOLVE) == true,
                        canReport = session?.has(Permission.PROPOSALS_CREATE) == true
                    )
                }
            }
        }
        load()
    }

    /** `GET /work-orders/{id}`: propuestas de la orden, y los servicios para elegir al aprobar. */
    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val services = getServices().getOrDefault(emptyList())
            getDetail(workOrderId)
                .onSuccess { detail ->
                    _uiState.update { it.copy(isLoading = false, proposals = detail.proposals, services = services) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, services = services, error = e.toWorkOrderUiText()) }
                }
        }
    }

    /** US15 E1: envía la propuesta con su foto al correo del cliente. */
    fun onNotify(proposalId: String) {
        val state = _uiState.value
        if (!state.canNotify || state.isBusy) return
        _uiState.update { it.copy(busyProposalId = proposalId, notice = null, error = null) }
        viewModelScope.launch {
            notifyProposal(workOrderId, proposalId)
                .onSuccess { email ->
                    _uiState.update { it.copy(busyProposalId = null, notice = UiText.Resource(R.string.proposals_notified, email)) }
                }
                .onFailure(::fail)
        }
    }

    fun onOpenForm(proposalId: String, action: ProposalAction) {
        val state = _uiState.value
        if (!state.canResolve || state.isBusy) return
        val proposal = state.proposals.firstOrNull { it.id == proposalId } ?: return
        _uiState.update {
            it.copy(
                form = ProposalForm(proposalId, action, serviceId = proposal.serviceId),
                notice = null, error = null, invalidField = null
            )
        }
    }

    fun onCloseForm() = _uiState.update { if (it.isBusy) it else it.copy(form = null, error = null, invalidField = null) }

    private fun editForm(change: (ProposalForm) -> ProposalForm) = _uiState.update {
        val form = it.form
        if (form == null || it.isBusy) it else it.copy(form = change(form), error = null, invalidField = null)
    }

    fun onServiceChange(serviceId: String) = editForm { it.copy(serviceId = serviceId) }
    fun onFinalPriceChange(value: String) = editForm { it.copy(finalPrice = value) }
    fun onLaborHoursChange(value: String) = editForm { it.copy(laborHours = value) }
    fun onNotesChange(value: String) = editForm { it.copy(notes = value) }

    fun onConfirm() {
        val state = _uiState.value
        val form = state.form
        if (form == null || !state.canConfirm) return
        when (form.action) {
            ProposalAction.APPROVE -> approve(form)
            ProposalAction.REJECT -> reject(form)
        }
    }

    private fun approve(form: ProposalForm) {
        val serviceId = form.serviceId
        val price = form.finalPrice.toAmount()
        val hours = form.laborHours.toAmount()
        if (serviceId == null || price == null || hours == null) {
            val field = when {
                serviceId == null -> "serviceId"
                price == null -> "finalPrice"
                else -> "laborHours"
            }
            fail(InvalidWorkOrderInputException(field))
            return
        }
        _uiState.update { it.copy(busyProposalId = form.proposalId, notice = null, error = null, invalidField = null) }
        viewModelScope.launch {
            val approval = ProposalApproval(serviceId, price, hours, notes = form.notes.trim().ifBlank { null })
            approveProposal(workOrderId, form.proposalId, approval)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            busyProposalId = null, form = null, proposals = detail.proposals,
                            notice = UiText.Resource(R.string.proposals_approved)
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    private fun reject(form: ProposalForm) {
        _uiState.update { it.copy(busyProposalId = form.proposalId, notice = null, error = null, invalidField = null) }
        viewModelScope.launch {
            rejectProposal(workOrderId, form.proposalId, form.notes.trim())
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            busyProposalId = null, form = null,
                            proposals = it.proposals.map { p -> if (p.id == updated.id) updated else p },
                            notice = UiText.Resource(R.string.proposals_rejected)
                        )
                    }
                }
                .onFailure(::fail)
        }
    }

    /** Acepta coma o punto decimal; null si no es un número. */
    private fun String.toAmount(): BigDecimal? = trim().replace(',', '.').toBigDecimalOrNull()

    private fun fail(error: Throwable) = _uiState.update {
        it.copy(busyProposalId = null, error = error.toWorkOrderUiText(), invalidField = error.invalidWorkOrderField())
    }
}
