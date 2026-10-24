package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import java.io.File
import javax.inject.Inject

/**
 * Proforma en PDF con mano de obra, repuestos, IGV y vigencia (US24 E1). Queda en `cacheDir/pdfs`; la UI la abre o
 * comparte con FileProvider (docs/04-mobile-integration.md §8). En línea.
 */
class DownloadQuotationPdfUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String): Result<File> = workOrderRepository.downloadQuotationPdf(id)
}
