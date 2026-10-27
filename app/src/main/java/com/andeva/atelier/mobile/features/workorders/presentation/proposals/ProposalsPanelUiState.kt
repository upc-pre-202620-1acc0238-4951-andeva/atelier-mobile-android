package com.andeva.atelier.mobile.features.workorders.presentation.proposals

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.workorders.domain.Proposal
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption

enum class ProposalAction { APPROVE, REJECT }

/** Formulario abierto sobre una propuesta: registrar la aprobación (crea la tarea) o el rechazo del cliente. */
data class ProposalForm(
    val proposalId: String,
    val action: ProposalAction,
    val serviceId: String? = null,
    /** Precio final sin IGV: el backend calcula el 18 % (docs/api/README.md). */
    val finalPrice: String = "",
    val laborHours: String = "",
    /** Observaciones: opcionales al aprobar, obligatorias al rechazar (`customerNotes`). */
    val notes: String = ""
) {
    val isFilled: Boolean
        get() = when (action) {
            ProposalAction.APPROVE -> finalPrice.isNotBlank() && laborHours.isNotBlank()
            ProposalAction.REJECT -> notes.isNotBlank()
        }
}

data class ProposalsPanelUiState(
    val proposals: List<Proposal> = emptyList(),
    val services: List<ServiceOption> = emptyList(),
    val isLoading: Boolean = true,
    /** Propuesta con una operación en curso (notificar, aprobar o rechazar). */
    val busyProposalId: String? = null,
    val form: ProposalForm? = null,
    val notice: UiText? = null,
    val error: UiText? = null,
    /** Campo del formulario que no pasó la validación (`serviceId`, `finalPrice`, `laborHours`, `notes`). */
    val invalidField: String? = null,
    val canNotify: Boolean = false,
    val canResolve: Boolean = false,
    val canReport: Boolean = false
) {
    val isBusy: Boolean get() = busyProposalId != null
    val canConfirm: Boolean get() = form?.isFilled == true && !isBusy
}
