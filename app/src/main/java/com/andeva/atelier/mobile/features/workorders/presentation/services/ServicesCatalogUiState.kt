package com.andeva.atelier.mobile.features.workorders.presentation.services

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption

/** Formulario de alta o edición de un servicio estándar del catálogo del taller. */
data class ServiceEditorForm(
    val serviceId: String? = null,
    val name: String = "",
    val basePrice: String = "",
    val estimatedMinutes: String = ""
) {
    val isEditing: Boolean get() = serviceId != null
}

data class ServicesCatalogUiState(
    val services: List<ServiceOption> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val canRead: Boolean = false,
    val canManage: Boolean = false,
    val isSaving: Boolean = false,
    val form: ServiceEditorForm? = null,
    val deletingService: ServiceOption? = null,
    val invalidField: String? = null,
    val notice: UiText? = null,
    val error: UiText? = null
) {
    val filteredServices: List<ServiceOption>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return services
            return services.filter { it.name.contains(q, ignoreCase = true) }
        }

    val canCreate: Boolean get() = canManage && isOnline && !isSaving

    val canConfirmForm: Boolean
        get() {
            val current = form ?: return false
            return current.name.isNotBlank() &&
                current.basePrice.isNotBlank() &&
                current.estimatedMinutes.isNotBlank() &&
                isOnline && !isSaving
        }
}
