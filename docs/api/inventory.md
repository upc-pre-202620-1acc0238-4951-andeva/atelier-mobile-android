# Inventory — Repuestos, lotes FIFO, proveedores y compras

Catálogo de repuestos, lotes con costeo FIFO, alertas de stock, proveedores y órdenes de compra.

Convenciones comunes (autenticación, errores, cabeceras, fechas, dinero): ver [README.md](README.md).

## Índice

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | [`/api/v1/inventory/alerts`](#get-apiv1inventoryalerts) | `inventory:alerts:read` | List items that have reached or dropped below reorder threshold |
| POST | [`/api/v1/inventory/alerts/evaluate`](#post-apiv1inventoryalertsevaluate) | `inventory:alerts:resolve` | Trigger system-wide stock reorder threshold evaluation for workshop |
| POST | [`/api/v1/inventory/alerts/{id}/resolve`](#post-apiv1inventoryalertsidresolve) | `inventory:alerts:resolve` | Acknowledge and mark low stock alert as resolved or replenishment in progress |
| GET | [`/api/v1/inventory/batches/by-part/{partId}`](#get-apiv1inventorybatchesby-partpartid) | `inventory:batches:read` | List all active batches for an inventory item ordered by FIFO |
| POST | [`/api/v1/inventory/batches/dispatch`](#post-apiv1inventorybatchesdispatch) | `inventory:batches:dispatch_fifo` | Dispatch stock using strict First-In, First-Out (FIFO) algorithm |
| POST | [`/api/v1/inventory/batches/receive`](#post-apiv1inventorybatchesreceive) | `inventory:batches:receive` | Direct physical batch reception into inventory item |
| POST | [`/api/v1/inventory/batches/restore`](#post-apiv1inventorybatchesrestore) | `inventory:batches:restore` | Restore unused allocated stock back to its originating batch |
| GET | [`/api/v1/inventory/items`](#get-apiv1inventoryitems) | `inventory:parts:read` | List and filter parts catalog |
| POST | [`/api/v1/inventory/items`](#post-apiv1inventoryitems) | `inventory:parts:manage` | Register new automotive part or consumable in workshop catalog |
| GET | [`/api/v1/inventory/items/by-category/{category}`](#get-apiv1inventoryitemsby-categorycategory) | `inventory:parts:read` | List parts belonging to a specific automotive category |
| GET | [`/api/v1/inventory/items/by-sku/{sku}`](#get-apiv1inventoryitemsby-skusku) | `inventory:parts:read` | Lookup part by commercial SKU barcode |
| GET | [`/api/v1/inventory/items/low-stock`](#get-apiv1inventoryitemslow-stock) | `inventory:parts:read` | Get low-stock items |
| GET | [`/api/v1/inventory/items/valuation`](#get-apiv1inventoryitemsvaluation) | `inventory:batches:read` | Get inventory valuation |
| GET | [`/api/v1/inventory/items/{id}`](#get-apiv1inventoryitemsid) | `inventory:parts:read` | Get detailed technical specifications and active batches for a part |
| PUT | [`/api/v1/inventory/items/{id}`](#put-apiv1inventoryitemsid) | `inventory:parts:manage` | Update technical specifications, base price, or minimum stock |
| GET | [`/api/v1/inventory/items/{id}/batches`](#get-apiv1inventoryitemsidbatches) | `inventory:batches:read` | List batches for an inventory item |
| GET | [`/api/v1/inventory/purchase-orders`](#get-apiv1inventorypurchase-orders) | `inventory:purchase_orders:read` | List purchase orders filtered by status or supplier |
| POST | [`/api/v1/inventory/purchase-orders`](#post-apiv1inventorypurchase-orders) | `inventory:purchase_orders:create` | Draft new multi-item purchase order to supplier |
| GET | [`/api/v1/inventory/purchase-orders/{id}`](#get-apiv1inventorypurchase-ordersid) | `inventory:purchase_orders:read` | Get detailed purchase order with lines and reception status |
| PUT | [`/api/v1/inventory/purchase-orders/{id}/cancel`](#put-apiv1inventorypurchase-ordersidcancel) | `inventory:purchase_orders:create` | Cancel pending purchase order before delivery |
| PUT | [`/api/v1/inventory/purchase-orders/{id}/issue`](#put-apiv1inventorypurchase-ordersidissue) | `inventory:purchase_orders:create` | Issue a draft purchase order to the supplier |
| POST | [`/api/v1/inventory/purchase-orders/{id}/items`](#post-apiv1inventorypurchase-ordersiditems) | `inventory:purchase_orders:create` | Add a line to a draft purchase order |
| PUT | [`/api/v1/inventory/purchase-orders/{id}/receive`](#put-apiv1inventorypurchase-ordersidreceive) | `inventory:batches:receive` | Confirm technical goods reception, physical count, and invoice registration |
| GET | [`/api/v1/inventory/suppliers`](#get-apiv1inventorysuppliers) | `inventory:suppliers:manage` | List registered and homologated automotive suppliers |
| POST | [`/api/v1/inventory/suppliers`](#post-apiv1inventorysuppliers) | `inventory:suppliers:manage` | Register new homologated supplier under RUC/tax identification |
| GET | [`/api/v1/inventory/suppliers/{id}`](#get-apiv1inventorysuppliersid) | `inventory:suppliers:manage` | Get detailed profile of registered supplier |
| PUT | [`/api/v1/inventory/suppliers/{id}`](#put-apiv1inventorysuppliersid) | `inventory:suppliers:manage` | Update supplier contact information and fiscal address |

## Endpoints

### GET /api/v1/inventory/alerts

List items that have reached or dropped below reorder threshold

- **Permiso:** `inventory:alerts:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<StockAlertResource>`

### POST /api/v1/inventory/alerts/evaluate

Trigger system-wide stock reorder threshold evaluation for workshop

- **Permiso:** `inventory:alerts:resolve`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `StockEvaluationSummaryResource`

### POST /api/v1/inventory/alerts/{id}/resolve

Acknowledge and mark low stock alert as resolved or replenishment in progress

- **Permiso:** `inventory:alerts:resolve`
- **Body** (application/json): `ResolveAlertResource` (opcional)
- **Respuesta exitosa:** 200 — `{ alertId, itemId, itemName, sku, status: "RESOLVED", notes, resolvedAt }`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### GET /api/v1/inventory/batches/by-part/{partId}

List all active batches for an inventory item ordered by FIFO

- **Permiso:** `inventory:batches:read`
- **Respuesta exitosa:** 200 — `array<BatchResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `partId` | path | string(uuid) | sí |

### POST /api/v1/inventory/batches/dispatch

Dispatch stock using strict First-In, First-Out (FIFO) algorithm

- **Permiso:** `inventory:batches:dispatch_fifo`
- **Body** (application/json): `DispatchBatchFifoResource`
- **Respuesta exitosa:** 200 — `StockAllocationResource`

### POST /api/v1/inventory/batches/receive

Direct physical batch reception into inventory item

- **Permiso:** `inventory:batches:receive`
- **Body** (application/json): `ReceiveBatchResource`
- **Respuesta exitosa:** 201 — `BatchResource`

### POST /api/v1/inventory/batches/restore

Restore unused allocated stock back to its originating batch

- **Permiso:** `inventory:batches:restore`
- **Body** (application/json): `RestoreBatchResource`
- **Respuesta exitosa:** 200 — `{ status, message, batchId, restoredQuantity }`

### GET /api/v1/inventory/items

List and filter parts catalog

- **Permiso:** `inventory:parts:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<PartResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `search` | query | string | no |
| `category` | query | string | no |
| `status` | query | string | no |
| `lowStockOnly` | query | boolean | no |
| `page` | query | integer(int32) | no |
| `size` | query | integer(int32) | no |

### POST /api/v1/inventory/items

Register new automotive part or consumable in workshop catalog

- **Permiso:** `inventory:parts:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `CreatePartResource`
- **Respuesta exitosa:** 201 — `PartResource`

### GET /api/v1/inventory/items/by-category/{category}

List parts belonging to a specific automotive category

- **Permiso:** `inventory:parts:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<PartResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `category` | path | string | sí |

### GET /api/v1/inventory/items/by-sku/{sku}

Lookup part by commercial SKU barcode

- **Permiso:** `inventory:parts:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `PartResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `sku` | path | string | sí |

### GET /api/v1/inventory/items/low-stock

Get low-stock items

- **Permiso:** `inventory:parts:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<PartResource>`

### GET /api/v1/inventory/items/valuation

Get inventory valuation

- **Permiso:** `inventory:batches:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `{ valuation: number }` (PEN)

### GET /api/v1/inventory/items/{id}

Get detailed technical specifications and active batches for a part

- **Permiso:** `inventory:parts:read`
- **Respuesta exitosa:** 200 — `PartDetailResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### PUT /api/v1/inventory/items/{id}

Update technical specifications, base price, or minimum stock

- **Permiso:** `inventory:parts:manage`
- **Body** (application/json): `UpdatePartResource`
- **Respuesta exitosa:** 200 — `PartResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### GET /api/v1/inventory/items/{id}/batches

List batches for an inventory item

- **Permiso:** `inventory:batches:read`
- **Respuesta exitosa:** 200 — `BatchResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### GET /api/v1/inventory/purchase-orders

List purchase orders filtered by status or supplier

- **Permiso:** `inventory:purchase_orders:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<PurchaseOrderResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `status` | query | string | no |
| `supplierId` | query | string(uuid) | no |

### POST /api/v1/inventory/purchase-orders

Draft new multi-item purchase order to supplier

- **Permiso:** `inventory:purchase_orders:create`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `CreatePurchaseOrderResource`
- **Respuesta exitosa:** 201 — `PurchaseOrderResource`

### GET /api/v1/inventory/purchase-orders/{id}

Get detailed purchase order with lines and reception status

- **Permiso:** `inventory:purchase_orders:read`
- **Respuesta exitosa:** 200 — `PurchaseOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### PUT /api/v1/inventory/purchase-orders/{id}/cancel

Cancel pending purchase order before delivery

- **Permiso:** `inventory:purchase_orders:create`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `CancelPurchaseOrderResource` (opcional)
- **Respuesta exitosa:** 200 — `PurchaseOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### PUT /api/v1/inventory/purchase-orders/{id}/issue

Issue a draft purchase order to the supplier

- **Permiso:** `inventory:purchase_orders:create`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `PurchaseOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### POST /api/v1/inventory/purchase-orders/{id}/items

Add a line to a draft purchase order

- **Permiso:** `inventory:purchase_orders:create`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `CreatePurchaseOrderItemResource`
- **Respuesta exitosa:** 200 — `PurchaseOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### PUT /api/v1/inventory/purchase-orders/{id}/receive

Confirm technical goods reception, physical count, and invoice registration

- **Permiso:** `inventory:batches:receive`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `ReceivePurchaseOrderResource` (opcional)
- **Respuesta exitosa:** 200 — `PurchaseOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### GET /api/v1/inventory/suppliers

List registered and homologated automotive suppliers

- **Permiso:** `inventory:suppliers:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<SupplierResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `search` | query | string | no |
| `status` | query | string | no |

### POST /api/v1/inventory/suppliers

Register new homologated supplier under RUC/tax identification

- **Permiso:** `inventory:suppliers:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `RegisterSupplierResource`
- **Respuesta exitosa:** 201 — `SupplierResource`

### GET /api/v1/inventory/suppliers/{id}

Get detailed profile of registered supplier

- **Permiso:** `inventory:suppliers:manage`
- **Respuesta exitosa:** 200 — `SupplierResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### PUT /api/v1/inventory/suppliers/{id}

Update supplier contact information and fiscal address

- **Permiso:** `inventory:suppliers:manage`
- **Body** (application/json): `UpdateSupplierResource`
- **Respuesta exitosa:** 200 — `SupplierResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

## Modelos

### StockAlertResource

| Campo | Tipo |
| --- | --- |
| `itemId` | string(uuid) |
| `name` | string |
| `sku` | string |
| `category` | ItemCategory |
| `currentStock` | number |
| `minimumStock` | number |
| `deficit` | number |
| `evaluatedAt` | string(date-time, UTC) |

### StockEvaluationSummaryResource

| Campo | Tipo |
| --- | --- |
| `totalItemsEvaluated` | integer |
| `lowStockItemsCount` | integer |
| `totalInventoryValuation` | number |
| `evaluatedAt` | string(date-time, UTC) |

### ResolveAlertResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `purchaseOrderId` | string(uuid) | no |  |
| `resolutionNotes` | string | no |  |

### BatchResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `itemId` | string(uuid) |
| `supplierId` | string(uuid) |
| `purchaseOrderId` | string(uuid) |
| `batchNumber` | string |
| `initialQty` | number |
| `remainingQty` | number |
| `unitCost` | number |
| `arrivalDate` | string(date-time, UTC) |
| `receiptImageUrl` | string |

### DispatchBatchFifoResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `itemId` | string(uuid) | sí |  |
| `quantity` | number | sí | ≥ 0.01 |
| `taskId` | string(uuid) | no |  |
| `workOrderId` | string(uuid) | no |  |

### StockAllocationResource

| Campo | Tipo |
| --- | --- |
| `allocationId` | string(uuid) |
| `allocatedQuantity` | number |
| `totalCogs` | number |
| `deductions` | array<BatchDeductionResource> |

### ReceiveBatchResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `batchNumber` | string | sí | min 1 |
| `itemId` | string(uuid) | sí |  |
| `quantity` | number | sí | ≥ 0.01 |
| `unitCost` | number | sí | ≥ 0 |
| `arrivalDate` | string(date-time) | no |  |
| `purchaseOrderId` | string(uuid) | no |  |
| `receiptImageUrl` | string | no |  |
| `supplierId` | string(uuid) | no |  |

### RestoreBatchResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `itemId` | string(uuid) | sí |  |
| `quantity` | number | sí | ≥ 0.01 |
| `batchId` | string(uuid) | no |  |
| `reason` | string | no |  |
| `workOrderId` | string(uuid) | no |  |

### PartResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `name` | string |
| `sku` | string |
| `category` | ItemCategory |
| `basePrice` | number |
| `totalStock` | number |
| `minimumStock` | number |
| `unitOfMeasure` | string |
| `status` | InventoryItemStatus |

### CreatePartResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `basePrice` | number | sí | ≥ 0 |
| `category` | string enum: LUBRICANTS\|BRAKES\|SUSPENSION\|ENGINE\|ELECTRICAL\|TIRES\|FILTERS\|BODYWORK\|ACCESSORIES | sí |  |
| `minimumStock` | number | sí | ≥ 0 |
| `name` | string | sí | min 1 |
| `sku` | string | sí | min 1 |
| `unitOfMeasure` | string | no |  |

### PartDetailResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `name` | string |
| `sku` | string |
| `category` | ItemCategory |
| `basePrice` | number |
| `totalStock` | number |
| `minimumStock` | number |
| `unitOfMeasure` | string |
| `status` | InventoryItemStatus |
| `batches` | array<BatchResource> |

### UpdatePartResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `basePrice` | number | sí | ≥ 0 |
| `category` | string enum: LUBRICANTS\|BRAKES\|SUSPENSION\|ENGINE\|ELECTRICAL\|TIRES\|FILTERS\|BODYWORK\|ACCESSORIES | sí |  |
| `minimumStock` | number | sí | ≥ 0 |
| `name` | string | sí | min 1 |
| `status` | string | no |  |
| `unitOfMeasure` | string | no |  |

### PurchaseOrderResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `supplierId` | string(uuid) |
| `branchId` | string(uuid) |
| `orderNumber` | string |
| `status` | PurchaseOrderStatus |
| `totalCost` | number |
| `receiptImageUrl` | string |
| `receiptNumber` | string |
| `receivedAt` | string(date-time, UTC) |
| `items` | array<PurchaseOrderItemResource> |

### CreatePurchaseOrderResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `branchId` | string(uuid) | sí |  |
| `items` | array<CreatePurchaseOrderItemResource> | sí |  |
| `supplierId` | string(uuid) | sí |  |
| `expectedDeliveryDate` | string(date) | no |  |
| `notes` | string | no |  |
| `orderNumber` | string | no |  |

### CancelPurchaseOrderResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `reason` | string | no |  |

### CreatePurchaseOrderItemResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `itemId` | string(uuid) | sí |  |
| `quantity` | number | sí | ≥ 0.01 |
| `unitCost` | number | sí | ≥ 0 |

### ReceivePurchaseOrderResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `receiptImageUrl` | string | sí | min 1 |
| `receiptNumber` | string | sí | min 1 |
| `receivedAt` | string(date-time) | no |  |

### SupplierResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `businessName` | string |
| `taxId` | string |
| `contactName` | string |
| `phone` | string |
| `email` | string |
| `address` | string |
| `active` | boolean |

### RegisterSupplierResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `businessName` | string | sí | min 1 |
| `taxId` | string | sí | min 1, patrón `^\d{8,20}$` |
| `address` | string | no |  |
| `contactName` | string | no |  |
| `email` | string | no |  |
| `phone` | string | no |  |

### UpdateSupplierResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `active` | boolean | no |  |
| `address` | string | no |  |
| `businessName` | string | no |  |
| `contactName` | string | no |  |
| `email` | string | no |  |
| `phone` | string | no |  |

### BatchDeductionResource

| Campo | Tipo |
| --- | --- |
| `batchId` | string(uuid) |
| `quantityDeducted` | number |
| `unitCost` | number |
| `subtotal` | number |

### PurchaseOrderItemResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `itemId` | string(uuid) |
| `quantity` | number |
| `unitCost` | number |
| `totalCost` | number |

