package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import javax.inject.Inject

/** Enviar la proforma al correo del cliente (US24 E2). Devuelve el correo; 422 si el cliente no tiene correo. En línea. */
class SendQuotationUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String): Result<String> = workOrderRepository.sendQuotation(id)
}
