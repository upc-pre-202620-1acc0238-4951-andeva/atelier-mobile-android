package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Recarga el catálogo de servicios del taller desde el servidor sin usar la caché en memoria. */
class RefreshServiceCatalogUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(): Result<List<ServiceOption>> =
        workOrderRepository.refreshServiceCatalog()
}
