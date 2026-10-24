package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import javax.inject.Inject

/** Buscar en el catálogo el repuesto a imputar, con saldo y precio base (US23 E1, US31). En línea. */
class SearchPartsUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(query: String) = taskRepository.searchParts(query)
}
