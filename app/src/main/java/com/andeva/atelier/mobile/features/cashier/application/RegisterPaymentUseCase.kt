package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.domain.PaymentMethod
import com.andeva.atelier.mobile.features.cashier.domain.Voucher
import com.andeva.atelier.mobile.features.cashier.domain.VoucherPaymentStatus
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Registra un cobro. Si el comprobante queda `PAID` recarga la orden; el fallo de esa recarga se ignora, porque el
 * cobro ya se registró.
 */
class RegisterPaymentUseCase @Inject constructor(
    private val cashierRepository: CashierRepository,
    private val getWorkOrderDetail: GetWorkOrderDetailUseCase
) {
    suspend operator fun invoke(
        voucherId: String,
        amount: BigDecimal,
        method: PaymentMethod,
        transactionReference: String? = null
    ): Result<Voucher> =
        cashierRepository.registerPayment(voucherId, amount, method, transactionReference).onSuccess { voucher ->
            val workOrderId = voucher.workOrderId
            if (voucher.paymentStatus == VoucherPaymentStatus.PAID && workOrderId != null) getWorkOrderDetail(workOrderId)
        }
}
