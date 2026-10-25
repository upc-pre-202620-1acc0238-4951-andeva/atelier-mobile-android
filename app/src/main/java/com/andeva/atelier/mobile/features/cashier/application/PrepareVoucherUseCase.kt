package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.InvalidCashierInputException
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDrafts
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Arma el comprobante de una orden `COMPLETED` (US39, H26). Los nombres de servicio salen del catálogo y los de
 * repuesto de la caché de inventario; si no se pueden leer (el catálogo pide un permiso que algunos roles no tienen)
 * la línea queda con la descripción de la tarea o el nombre del repuesto, sin fallar el borrador; si ese nombre es el
 * genérico del detalle (H11) la línea queda marcada con `needsDescription` y la UI debe pedir editarla antes de emitir.
 * Sin líneas que cobrar falla con `lines`; con precios en varias monedas, con `currency`.
 */
class PrepareVoucherUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository,
    private val inventoryRepository: InventoryRepository,
    private val receptionRepository: ReceptionRepository
) {
    suspend operator fun invoke(workOrderId: String): Result<VoucherDraft> {
        val detail = workOrderRepository.getDetail(workOrderId).getOrElse { return Result.failure(it) }
        if (detail.workOrder.status != WorkOrderStatus.COMPLETED) return Result.failure(InvalidCashierInputException("status"))

        val serviceNames = workOrderRepository.getServiceCatalog().getOrNull().orEmpty().associate { it.id to it.name }
        val partNames = detail.tasks.flatMap { it.products }.map { it.productId }.distinct()
            .mapNotNull { id -> inventoryRepository.observePart(id).first()?.name?.let { id to it } }
            .toMap()
        val lines = try {
            VoucherDrafts.fromWorkOrder(detail, serviceNames, partNames)
        } catch (e: InvalidCashierInputException) {
            return Result.failure(e)
        }

        // Solo cuentan los precios que dan línea: una tarea cancelada o un repuesto en 0 no fijan la moneda.
        val currency = VoucherDrafts.currenciesOf(detail).singleOrNull()
            ?: return Result.failure(InvalidCashierInputException("currency"))

        val customer = receptionRepository.getCustomer(detail.workOrder.customerId).getOrElse { return Result.failure(it) }
        val (type, info) = VoucherDrafts.suggestFiscalInfo(customer)
        return Result.success(VoucherDraft(workOrderId, customer.id, type, info, currency, lines))
    }
}
