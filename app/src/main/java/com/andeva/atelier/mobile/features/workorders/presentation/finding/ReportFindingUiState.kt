package com.andeva.atelier.mobile.features.workorders.presentation.finding

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.workorders.domain.PendingProposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import java.io.File

data class ReportFindingUiState(
    val description: String = "",
    val severity: ProposalSeverity? = null,
    /** Foto del hallazgo: es obligatoria, sin ella el backend responde 400 (US14 E2). */
    val photo: File? = null,
    val suggestedServiceId: String? = null,
    val services: List<ServiceOption> = emptyList(),
    val isLoadingServices: Boolean = true,
    val pending: List<PendingProposal> = emptyList(),
    val isSaving: Boolean = false,
    /** El último hallazgo quedó guardado: se enviará en cuanto haya señal. */
    val saved: Boolean = false,
    val error: UiText? = null,
    /** Campo que no pasó la validación local (`description`, `severity`, `photo`). */
    val invalidField: String? = null,
    val canCreate: Boolean = false
) {
    val canSubmit: Boolean
        get() = canCreate && !isSaving && description.isNotBlank() && severity != null && photo != null
}
