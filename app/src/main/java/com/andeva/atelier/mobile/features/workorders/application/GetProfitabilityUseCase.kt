package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.Profitability
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/**
 * Costo de ventas FIFO y margen de los repuestos de la orden (US35 E2). Requiere `inventory:batches:read` o
 * `invoicing:cashflow:export_pdf`. En línea.
 */
class GetProfitabilityUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String): Result<Profitability> = workOrderRepository.getProfitability(id)
}
