package com.andeva.atelier.mobile.features.cashier.infrastructure.remote

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** `ElectronicVoucherResource` de docs/api/invoicing.md. */
data class ElectronicVoucherDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("customerId") val customerId: String?,
    @SerializedName("workOrderId") val workOrderId: String?,
    @SerializedName("voucherType") val voucherType: String?,
    @SerializedName("serie") val serie: String?,
    @SerializedName("number") val number: Int?,
    @SerializedName("subtotal") val subtotal: BigDecimal?,
    @SerializedName("igvAmount") val igvAmount: BigDecimal?,
    @SerializedName("totalAmount") val totalAmount: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("customerInfo") val customerInfo: CustomerFiscalInfoDto?,
    @SerializedName("digitalReceipts") val digitalReceipts: DigitalReceiptUrlsDto?,
    @SerializedName("sunatResponse") val sunatResponse: SunatResponseDto?,
    @SerializedName("lines") val lines: List<VoucherLineDto>?,
    @SerializedName("payments") val payments: List<VoucherPaymentDto>?,
    @SerializedName("issuedAt") val issuedAt: String?,
    @SerializedName("paidAmount") val paidAmount: BigDecimal?,
    @SerializedName("pendingBalance") val pendingBalance: BigDecimal?,
    @SerializedName("paymentStatus") val paymentStatus: String?
)

/** `CustomerFiscalInfoResponse` y `CustomerFiscalInfoRequest`. */
data class CustomerFiscalInfoDto(
    @SerializedName("documentType") val documentType: String?,
    @SerializedName("taxId") val taxId: String?,
    @SerializedName("legalName") val legalName: String?,
    @SerializedName("fiscalAddress") val fiscalAddress: String?
)

/** `DigitalReceiptUrlsResponse`. */
data class DigitalReceiptUrlsDto(
    @SerializedName("pdfUrl") val pdfUrl: String?,
    @SerializedName("xmlUrl") val xmlUrl: String?,
    @SerializedName("cdrUrl") val cdrUrl: String?
)

/** `SunatResponseDto`. */
data class SunatResponseDto(
    @SerializedName("responseCode") val responseCode: String?,
    @SerializedName("description") val description: String?
)

/** `VoucherLineResource`. */
data class VoucherLineDto(
    @SerializedName("id") val id: String?,
    @SerializedName("itemId") val itemId: String?,
    @SerializedName("itemType") val itemType: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("quantity") val quantity: BigDecimal?,
    @SerializedName("unitValue") val unitValue: BigDecimal?,
    @SerializedName("unitPrice") val unitPrice: BigDecimal?,
    @SerializedName("igvAmount") val igvAmount: BigDecimal?,
    @SerializedName("totalLine") val totalLine: BigDecimal?
)

/** `VoucherPaymentResource`. */
data class VoucherPaymentDto(
    @SerializedName("id") val id: String,
    @SerializedName("voucherId") val voucherId: String?,
    @SerializedName("amount") val amount: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("paymentMethod") val paymentMethod: String?,
    @SerializedName("transactionReference") val transactionReference: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("paidAt") val paidAt: String?
)

/** `IssueVoucherRequest`; `voucherType` viaja como código SUNAT ("01"/"03"). */
data class IssueVoucherRequestDto(
    @SerializedName("branchId") val branchId: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("customerId") val customerId: String,
    @SerializedName("customerInfo") val customerInfo: CustomerFiscalInfoDto,
    @SerializedName("lines") val lines: List<VoucherLineRequestDto>,
    @SerializedName("serie") val serie: String,
    @SerializedName("voucherType") val voucherType: String,
    @SerializedName("workOrderId") val workOrderId: String?
)

/** `VoucherLineRequest`: `unitPriceWithIgv` lleva IGV. */
data class VoucherLineRequestDto(
    @SerializedName("description") val description: String,
    @SerializedName("itemType") val itemType: String,
    @SerializedName("quantity") val quantity: BigDecimal,
    @SerializedName("unitPriceWithIgv") val unitPriceWithIgv: BigDecimal,
    @SerializedName("itemId") val itemId: String?
)

/** `RegisterPaymentRequest`. */
data class RegisterPaymentRequestDto(
    @SerializedName("amount") val amount: BigDecimal,
    @SerializedName("branchId") val branchId: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("paymentMethod") val paymentMethod: String,
    @SerializedName("voucherId") val voucherId: String,
    @SerializedName("transactionReference") val transactionReference: String?
)

/** `DailyReconciliationResource`. */
data class DailyReconciliationDto(
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("totalCollected") val totalCollected: BigDecimal?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("paymentCount") val paymentCount: Int?,
    @SerializedName("breakdownByMethod") val breakdownByMethod: Map<String, BigDecimal>?,
    @SerializedName("payments") val payments: List<VoucherPaymentDto>?
)

/** `SeriesConfigurationResource`. */
data class SeriesConfigurationDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("voucherType") val voucherType: String?,
    @SerializedName("serie") val serie: String?,
    @SerializedName("currentCorrelative") val currentCorrelative: Int?,
    @SerializedName("active") val active: Boolean?
)

/** `ConfigureSeriesRequest`. */
data class ConfigureSeriesRequestDto(
    @SerializedName("branchId") val branchId: String,
    @SerializedName("serie") val serie: String,
    @SerializedName("voucherType") val voucherType: String,
    @SerializedName("initialCorrelative") val initialCorrelative: Int?
)

/** `UpdateSeriesStatusRequest`. */
data class UpdateSeriesStatusRequestDto(@SerializedName("active") val active: Boolean)
