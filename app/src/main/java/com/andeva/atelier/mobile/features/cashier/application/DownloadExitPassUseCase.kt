package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import java.io.File
import javax.inject.Inject

/** Descarga el pase de salida en PDF. */
class DownloadExitPassUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(workOrderId: String): Result<File> = cashierRepository.downloadExitPass(workOrderId)
}
