# Invoicing — Comprobantes electrónicos SUNAT

Emisión de boletas y facturas (PSE gestionado por la plataforma, modo simulado), pagos, series y flujo de caja.

Convenciones comunes (autenticación, errores, cabeceras, fechas, dinero): ver [README.md](README.md).

## Índice

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | [`/api/v1/invoicing/financial-reports/cash-flow`](#get-apiv1invoicingfinancial-reportscash-flow) | `invoicing:cashflow:export_pdf` | Get interactive JSON workshop cash flow statement for date range |
| GET | [`/api/v1/invoicing/financial-reports/cash-flow/pdf`](#get-apiv1invoicingfinancial-reportscash-flowpdf) | `invoicing:cashflow:export_pdf` | Download formal A4 vector PDF workshop cash flow statement |
| POST | [`/api/v1/invoicing/payments`](#post-apiv1invoicingpayments) | `invoicing:payments:create` | Register customer payment against an electronic voucher |
| GET | [`/api/v1/invoicing/payments/branch/{branchId}/daily`](#get-apiv1invoicingpaymentsbranchbranchiddaily) | `invoicing:invoices:read` | Get daily cash reconciliation breakdown for a workshop branch |
| GET | [`/api/v1/invoicing/payments/voucher/{voucherId}`](#get-apiv1invoicingpaymentsvouchervoucherid) | `invoicing:invoices:read` | Get full payment history for an electronic voucher |
| POST | [`/api/v1/invoicing/series-configurations`](#post-apiv1invoicingseries-configurations) | `invoicing:fiscal_config:manage` | Configure a new authorized fiscal series for a branch |
| GET | [`/api/v1/invoicing/series-configurations/branch/{branchId}`](#get-apiv1invoicingseries-configurationsbranchbranchid) | `invoicing:fiscal_config:manage` o `invoicing:invoices:issue_sunat` | List all registered fiscal series for a workshop branch |
| PUT | [`/api/v1/invoicing/series-configurations/{id}/status`](#put-apiv1invoicingseries-configurationsidstatus) | `invoicing:fiscal_config:manage` | Toggle active/inactive status of a fiscal series |
| GET | [`/api/v1/invoicing/vouchers`](#get-apiv1invoicingvouchers) | `invoicing:invoices:read` | List electronic vouchers for authenticated tenant with optional filters |
| POST | [`/api/v1/invoicing/vouchers`](#post-apiv1invoicingvouchers) | `invoicing:invoices:issue_sunat` | Issue electronic voucher (Factura 01 or Boleta 03) |
| POST | [`/api/v1/invoicing/vouchers/credit-notes`](#post-apiv1invoicingvoucherscredit-notes) | `invoicing:invoices:issue_sunat` | Issue electronic credit note referencing an accepted voucher |
| GET | [`/api/v1/invoicing/vouchers/work-order/{workOrderId}`](#get-apiv1invoicingvoucherswork-orderworkorderid) | `invoicing:invoices:read` | Get electronic vouchers linked to a repair work order |
| GET | [`/api/v1/invoicing/vouchers/{id}`](#get-apiv1invoicingvouchersid) | `invoicing:invoices:read` | Get electronic voucher by identifier |
| POST | [`/api/v1/invoicing/vouchers/{id}/void`](#post-apiv1invoicingvouchersidvoid) | `invoicing:invoices:issue_sunat` | Void electronic voucher via comunicaciÃ³n de baja within 7 calendar days |

## Endpoints

### GET /api/v1/invoicing/financial-reports/cash-flow

Get interactive JSON workshop cash flow statement for date range

- **Permiso:** `invoicing:cashflow:export_pdf`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `CashFlowSummaryResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `startDate` | query | string(date) | no |
| `from` | query | string(date) | no |
| `endDate` | query | string(date) | no |
| `to` | query | string(date) | no |

**Errores documentados:** **400** Invalid date range parameters; **401** Unauthorized; **403** Forbidden

### GET /api/v1/invoicing/financial-reports/cash-flow/pdf

Download formal A4 vector PDF workshop cash flow statement

- **Permiso:** `invoicing:cashflow:export_pdf`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — archivo `application/pdf` (Content-Disposition con nombre de archivo)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `startDate` | query | string(date) | no |
| `from` | query | string(date) | no |
| `endDate` | query | string(date) | no |
| `to` | query | string(date) | no |

**Errores documentados:** **400** Invalid date range parameters; **401** Unauthorized; **403** Forbidden

### POST /api/v1/invoicing/payments

Register customer payment against an electronic voucher

- **Permiso:** `invoicing:payments:create`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `RegisterPaymentRequest`
- **Respuesta exitosa:** 201 — `VoucherPaymentResource`

**Errores documentados:** **400** Invalid payment parameters or negative amount; **401** Unauthorized; **403** Forbidden; **404** Electronic voucher not found; **409** Voucher already paid or payment exceeds balance

### GET /api/v1/invoicing/payments/branch/{branchId}/daily

Get daily cash reconciliation breakdown for a workshop branch

- **Permiso:** `invoicing:invoices:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `DailyReconciliationResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `branchId` | path | string(uuid) | sí |
| `date` | query | string(date) | no |

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### GET /api/v1/invoicing/payments/voucher/{voucherId}

Get full payment history for an electronic voucher

- **Permiso:** `invoicing:invoices:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<VoucherPaymentResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `voucherId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Voucher not found

### POST /api/v1/invoicing/series-configurations

Configure a new authorized fiscal series for a branch

- **Permiso:** `invoicing:fiscal_config:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `ConfigureSeriesRequest`
- **Respuesta exitosa:** 201 — `SeriesConfigurationResource`

**Errores documentados:** **400** Validation error or invalid prefix; **401** Unauthorized; **403** Forbidden; **409** Series already registered

### GET /api/v1/invoicing/series-configurations/branch/{branchId}

List all registered fiscal series for a workshop branch

- **Permiso:** `invoicing:fiscal_config:manage` o `invoicing:invoices:issue_sunat`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<SeriesConfigurationResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `branchId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### PUT /api/v1/invoicing/series-configurations/{id}/status

Toggle active/inactive status of a fiscal series

- **Permiso:** `invoicing:fiscal_config:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `UpdateSeriesStatusRequest`
- **Respuesta exitosa:** 200 — cuerpo vacío

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Series not found

### GET /api/v1/invoicing/vouchers

List electronic vouchers for authenticated tenant with optional filters

- **Permiso:** `invoicing:invoices:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<ElectronicVoucherResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `branchId` | query | string(uuid) | no |
| `voucherType` | query | string | no |
| `status` | query | string | no |
| `from` | query | string(date) | no |
| `to` | query | string(date) | no |

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### POST /api/v1/invoicing/vouchers

Issue electronic voucher (Factura 01 or Boleta 03)

- **Permiso:** `invoicing:invoices:issue_sunat`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `IssueVoucherRequest`
- **Respuesta exitosa:** 201 — `ElectronicVoucherResource`

**Errores documentados:** **400** Bad request or validation error; **401** Unauthorized; **403** Forbidden; **404** Series not found; **409** Correlative exhausted; **502** SUNAT integration failure

### POST /api/v1/invoicing/vouchers/credit-notes

Issue electronic credit note referencing an accepted voucher

- **Permiso:** `invoicing:invoices:issue_sunat`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `IssueCreditNoteRequest`
- **Respuesta exitosa:** 201 — `ElectronicVoucherResource`

**Errores documentados:** **400** Validation error; **401** Unauthorized; **403** Forbidden; **404** Reference voucher or series not found

### GET /api/v1/invoicing/vouchers/work-order/{workOrderId}

Get electronic vouchers linked to a repair work order

- **Permiso:** `invoicing:invoices:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<ElectronicVoucherResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### GET /api/v1/invoicing/vouchers/{id}

Get electronic voucher by identifier

- **Permiso:** `invoicing:invoices:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `ElectronicVoucherResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Voucher not found

### POST /api/v1/invoicing/vouchers/{id}/void

Void electronic voucher via comunicaciÃ³n de baja within 7 calendar days

- **Permiso:** `invoicing:invoices:issue_sunat`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `VoidVoucherRequest`
- **Respuesta exitosa:** 200 — `ElectronicVoucherResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **400** Validation or window expired; **401** Unauthorized; **403** Forbidden; **404** Voucher not found

## Modelos

### CashFlowSummaryResource

| Campo | Tipo |
| --- | --- |
| `totalIncome` | number |
| `totalExpenses` | number |
| `accountsReceivable` | number |
| `netCashFlow` | number |
| `currency` | string |
| `movements` | array<CashFlowMovementResource> |

### RegisterPaymentRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `amount` | number | sí | ≥ 0.01 |
| `branchId` | string(uuid) | sí |  |
| `currency` | string | sí | min 1, patrón `PEN\|USD` |
| `paymentMethod` | string | sí | min 1 |
| `voucherId` | string(uuid) | sí |  |
| `transactionReference` | string | no | max 100 |

### VoucherPaymentResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `voucherId` | string(uuid) |
| `amount` | number |
| `currency` | string |
| `paymentMethod` | string |
| `transactionReference` | string |
| `status` | string |
| `paidAt` | string(date-time, UTC) |

### DailyReconciliationResource

| Campo | Tipo |
| --- | --- |
| `branchId` | string(uuid) |
| `date` | string(date) |
| `totalCollected` | number |
| `currency` | string |
| `paymentCount` | integer |
| `breakdownByMethod` | map<string, number> |
| `payments` | array<VoucherPaymentResource> |

### ConfigureSeriesRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `branchId` | string(uuid) | sí |  |
| `serie` | string | sí | min 4, max 4 |
| `voucherType` | string | sí | min 1 |
| `initialCorrelative` | integer(int32) | no | ≥ 0 |

### SeriesConfigurationResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `branchId` | string(uuid) |
| `voucherType` | string |
| `serie` | string |
| `currentCorrelative` | integer |
| `active` | boolean |
| `createdAt` | string(date-time, UTC) |

### UpdateSeriesStatusRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `active` | boolean | sí |  |

### ElectronicVoucherResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `branchId` | string(uuid) |
| `customerId` | string(uuid) |
| `workOrderId` | string(uuid) |
| `voucherType` | string |
| `serie` | string |
| `number` | integer |
| `subtotal` | number |
| `igvAmount` | number |
| `totalAmount` | number |
| `currency` | string |
| `status` | string |
| `customerInfo` | CustomerFiscalInfoResponse |
| `digitalReceipts` | DigitalReceiptUrlsResponse |
| `sunatResponse` | SunatResponseDto |
| `lines` | array<VoucherLineResource> |
| `payments` | array<VoucherPaymentResource> |
| `issuedAt` | string(date-time, UTC) |
| `paidAmount` | number |
| `pendingBalance` | number |
| `paymentStatus` | string |

### IssueVoucherRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `branchId` | string(uuid) | sí |  |
| `currency` | string | sí | min 1, patrón `PEN\|USD` |
| `customerId` | string(uuid) | sí |  |
| `customerInfo` | CustomerFiscalInfoRequest | sí |  |
| `lines` | array<VoucherLineRequest> | sí |  |
| `serie` | string | sí | min 4, max 4 |
| `voucherType` | string | sí | min 1, patrón `01\|03` |
| `workOrderId` | string(uuid) | no |  |

### IssueCreditNoteRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `branchId` | string(uuid) | sí |  |
| `customerId` | string(uuid) | sí |  |
| `lines` | array<VoucherLineRequest> | sí |  |
| `reason` | string | sí | min 1 |
| `reasonDescription` | string | sí | max 250 |
| `referenceVoucherId` | string(uuid) | sí |  |
| `serie` | string | sí | min 4, max 4 |

### VoidVoucherRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `reason` | string | sí | max 250 |

### CashFlowMovementResource

| Campo | Tipo |
| --- | --- |
| `transactionId` | string(uuid) |
| `movementDate` | string(date-time, UTC) |
| `type` | string |
| `category` | string |
| `concept` | string |
| `referenceNumber` | string |
| `amount` | number |
| `runningBalance` | number |

### CustomerFiscalInfoResponse

| Campo | Tipo |
| --- | --- |
| `taxId` | string |
| `legalName` | string |
| `fiscalAddress` | string |
| `documentType` | string |

### DigitalReceiptUrlsResponse

| Campo | Tipo |
| --- | --- |
| `pdfUrl` | string |
| `xmlUrl` | string |
| `cdrUrl` | string |

### SunatResponseDto

| Campo | Tipo |
| --- | --- |
| `responseCode` | string |
| `description` | string |
| `digitalSignatureHash` | string |

### VoucherLineResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `itemId` | string(uuid) |
| `itemType` | string |
| `description` | string |
| `quantity` | number |
| `unitValue` | number |
| `unitPrice` | number |
| `igvAmount` | number |
| `totalLine` | number |

### CustomerFiscalInfoRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `documentType` | string | sí | min 1 |
| `legalName` | string | sí | max 200 |
| `taxId` | string | sí | min 8, max 11 |
| `fiscalAddress` | string | no | max 250 |

### VoucherLineRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `description` | string | sí | max 200 |
| `itemType` | string | sí | min 1, patrón `PRODUCT\|SERVICE` |
| `quantity` | number | sí | ≥ 0.01 |
| `unitPriceWithIgv` | number | sí | ≥ 0.01 |
| `itemId` | string(uuid) | no |  |

