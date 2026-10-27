package com.andeva.atelier.mobile.features.workorders.presentation.open

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption

/**
 * Nivel de combustible al recibir el vehículo. El backend no tiene un campo para él (docs/api/operations.md): se
 * guarda al inicio del diagnóstico con [summaryLabel] (hueco registrado en docs/03-traceability.md).
 */
enum class FuelLevel(val summaryLabel: String) {
    RESERVE("Reserva"), QUARTER("1/4"), HALF("1/2"), THREE_QUARTERS("3/4"), FULL("Lleno")
}

data class OpenWorkOrderUiState(
    val mileage: String = "",
    val fuel: FuelLevel? = null,
    val reason: String = "",
    val services: List<ServiceOption> = emptyList(),
    val mechanics: List<MechanicOption> = emptyList(),
    val isLoadingCatalog: Boolean = true,
    val selectedServiceIds: Set<String> = emptySet(),
    val selectedMechanicId: String? = null,
    /** Orden en borrador que se completa (llegó por una cita) o la que se acaba de abrir aquí. */
    val workOrderId: String? = null,
    /** La orden ya quedó guardada: al reintentar solo se agregan las faenas que faltan. */
    val orderSaved: Boolean = false,
    val addedServiceIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val error: UiText? = null,
    /** Campo que no pasó la validación local (`mileageIn`, `diagnosticSummary`, `description`). */
    val invalidField: String? = null,
    /** Id de la orden lista, para seguir con las fotos de recepción. */
    val doneWorkOrderId: String? = null,
    val canCreate: Boolean = false,
    val canUpdate: Boolean = false
) {
    /** Completar un borrador existente es una actualización; abrir una nueva es un alta. */
    val isCompletingDraft: Boolean get() = workOrderId != null && !orderSaved
    val formLocked: Boolean get() = isSaving || orderSaved
    val hasPermission: Boolean get() = canCreate && (!isCompletingDraft || canUpdate)
    val canSubmit: Boolean
        get() = hasPermission && !isSaving && doneWorkOrderId == null && !isLoadingCatalog &&
            mileage.isNotBlank() && fuel != null && reason.isNotBlank()
    val pendingServices: List<ServiceOption>
        get() = services.filter { it.id in selectedServiceIds && it.id !in addedServiceIds }
}
