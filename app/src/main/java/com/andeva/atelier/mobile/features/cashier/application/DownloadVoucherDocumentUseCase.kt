package com.andeva.atelier.mobile.features.cashier.application

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import java.io.File
import javax.inject.Inject

/** Descarga un archivo fiscal (`pdfUrl`, `xmlUrl` o `cdrUrl`) del comprobante a `cacheDir/pdfs`. */
class DownloadVoucherDocumentUseCase @Inject constructor(
    private val cashierRepository: CashierRepository
) {
    suspend operator fun invoke(url: String, fileNamePrefix: String): Result<File> =
        cashierRepository.downloadVoucherDocument(url, fileNamePrefix)
}
