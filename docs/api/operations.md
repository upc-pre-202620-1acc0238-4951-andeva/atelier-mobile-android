# Operations — Órdenes de trabajo, tareas, bahías y servicios

Ciclo de vida de la orden de trabajo, tareas del mecánico, repuestos consumidos, evidencias, propuestas, cotización y pase de salida.

Convenciones comunes (autenticación, errores, cabeceras, fechas, dinero): ver [README.md](README.md).

## Índice

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | [`/api/v1/services`](#get-apiv1services) | `operations:services:read` | List all standard catalog services for tenant |
| POST | [`/api/v1/services`](#post-apiv1services) | `operations:services:manage` | Register standard catalog service in workshop |
| GET | [`/api/v1/services/{serviceId}`](#get-apiv1servicesserviceid) | `operations:services:read` | Get standard service catalog item by id |
| PUT | [`/api/v1/services/{serviceId}`](#put-apiv1servicesserviceid) | `operations:services:manage` | Update standard service catalog item |
| GET | [`/api/v1/tasks/my`](#get-apiv1tasksmy) | `operations:tasks:read` | List the open tasks assigned to the authenticated mechanic, in progress first |
| GET | [`/api/v1/tasks/{taskId}`](#get-apiv1taskstaskid) | `operations:tasks:read` | Get detailed technical task by id |
| PUT | [`/api/v1/tasks/{taskId}`](#put-apiv1taskstaskid) | `operations:work_orders:update` | Update task parameters and assigned mechanic |
| POST | [`/api/v1/tasks/{taskId}/complete`](#post-apiv1taskstaskidcomplete) | `operations:tasks:complete` | Mark technical task as completed with actual labor hours |
| POST | [`/api/v1/tasks/{taskId}/evidence-images`](#post-apiv1taskstaskidevidence-images) | `operations:tasks:upload_photos` | Attach photographic evidence to technical task |
| POST | [`/api/v1/tasks/{taskId}/hold`](#post-apiv1taskstaskidhold) | `operations:tasks:hold_request` | Request suspension of the task for a defective or missing part (awaits administrative approval) |
| POST | [`/api/v1/tasks/{taskId}/hold/approve`](#post-apiv1taskstaskidholdapprove) | `operations:tasks:hold_validate` | Approve the suspension request: the task goes ON_HOLD until the part arrives |
| POST | [`/api/v1/tasks/{taskId}/hold/reject`](#post-apiv1taskstaskidholdreject) | `operations:tasks:hold_validate` | Dismiss the suspension request: the task stays IN_PROGRESS |
| PUT | [`/api/v1/tasks/{taskId}/mechanic`](#put-apiv1taskstaskidmechanic) | `operations:bays:reassign` | Assign or change mechanic for technical task |
| POST | [`/api/v1/tasks/{taskId}/pause`](#post-apiv1taskstaskidpause) | `operations:tasks:track_time` | Pause the labor clock for a break or the end of the shift (resume with /start) |
| POST | [`/api/v1/tasks/{taskId}/products`](#post-apiv1taskstaskidproducts) | `operations:work_orders:update` o `operations:tasks:charge_parts` | Add consumed product or spare part to technical task (a mechanic only to a task of their own) |
| DELETE | [`/api/v1/tasks/{taskId}/products/{productId}`](#delete-apiv1taskstaskidproductsproductid) | `operations:work_orders:update` | Remove consumed product or spare part from technical task |
| PUT | [`/api/v1/tasks/{taskId}/products/{productId}`](#put-apiv1taskstaskidproductsproductid) | `operations:work_orders:update` | Update quantity of consumed spare part |
| POST | [`/api/v1/tasks/{taskId}/reopen`](#post-apiv1taskstaskidreopen) | `operations:work_orders:update` | Reopen completed task for quality rework |
| POST | [`/api/v1/tasks/{taskId}/resume`](#post-apiv1taskstaskidresume) | `operations:tasks:hold_validate` | Resume paused task after parts arrival |
| POST | [`/api/v1/tasks/{taskId}/start`](#post-apiv1taskstaskidstart) | `operations:tasks:track_time` | Start or resume execution of a technical task |
| GET | [`/api/v1/work-bays`](#get-apiv1work-bays) | `operations:bays:read` | List work bays by branch with availability filtering |
| POST | [`/api/v1/work-bays`](#post-apiv1work-bays) | `operations:bays:manage` | Register a new physical work bay in workshop branch |
| GET | [`/api/v1/work-bays/{bayId}`](#get-apiv1work-baysbayid) | `operations:bays:read` | Get physical work bay details by id |
| PUT | [`/api/v1/work-bays/{bayId}/maintenance`](#put-apiv1work-baysbayidmaintenance) | `operations:bays:manage` | Place work bay under maintenance |
| PUT | [`/api/v1/work-bays/{bayId}/restore`](#put-apiv1work-baysbayidrestore) | `operations:bays:manage` | Restore work bay to available status |
| GET | [`/api/v1/work-orders`](#get-apiv1work-orders) | `operations:work_orders:read` | List and search work orders by branch or tenant |
| POST | [`/api/v1/work-orders`](#post-apiv1work-orders) | `operations:work_orders:create` | Open formal work order upon technical vehicle reception |
| GET | [`/api/v1/work-orders/{workOrderId}`](#get-apiv1work-ordersworkorderid) | `operations:work_orders:read` | Get exhaustive work order details with tasks, proposals, and images |
| PUT | [`/api/v1/work-orders/{workOrderId}`](#put-apiv1work-ordersworkorderid) | `operations:work_orders:update` | Update verified mileage or diagnostic summary |
| DELETE | [`/api/v1/work-orders/{workOrderId}/bay`](#delete-apiv1work-ordersworkorderidbay) | `operations:bays:reassign` | Release and deallocate current work bay |
| PUT | [`/api/v1/work-orders/{workOrderId}/bay`](#put-apiv1work-ordersworkorderidbay) | `operations:bays:reassign` | Assign or relocate vehicle to physical work bay |
| POST | [`/api/v1/work-orders/{workOrderId}/cancel`](#post-apiv1work-ordersworkorderidcancel) | `operations:work_orders:cancel` | Cancel work order releasing occupied bay and inventory reservations |
| POST | [`/api/v1/work-orders/{workOrderId}/complete`](#post-apiv1work-ordersworkorderidcomplete) | `operations:work_orders:update` | Complete work order closing all technical bay operations |
| POST | [`/api/v1/work-orders/{workOrderId}/deliver`](#post-apiv1work-ordersworkorderiddeliver) | `operations:vehicle_handover:execute` | Deliver vehicle back to owner following payment confirmation |
| GET | [`/api/v1/work-orders/{workOrderId}/exit-pass/pdf`](#get-apiv1work-ordersworkorderidexit-passpdf) | `invoicing:exit_passes:issue` | Issue the vehicle exit pass (only for paid orders) |
| POST | [`/api/v1/work-orders/{workOrderId}/intake-images`](#post-apiv1work-ordersworkorderidintake-images) | `operations:inspections:upload_photos` | Attach reception intake photograph metadata from Firebase Storage |
| POST | [`/api/v1/work-orders/{workOrderId}/mark-as-paid`](#post-apiv1work-ordersworkorderidmark-as-paid) | `invoicing:payments:create` | Mark work order as formally paid enabling vehicle exit |
| GET | [`/api/v1/work-orders/{workOrderId}/part-movements`](#get-apiv1work-ordersworkorderidpart-movements) | `inventory:batches:read` o `inventory:batches:restore` | Audit trail of the parts charged to the work order and returned to their batch (US33) |
| GET | [`/api/v1/work-orders/{workOrderId}/profitability`](#get-apiv1work-ordersworkorderidprofitability) | `inventory:batches:read` o `invoicing:cashflow:export_pdf` | Parts revenue against their FIFO cost of sales and gross profit of the work order (US35) |
| GET | [`/api/v1/work-orders/{workOrderId}/proposals`](#get-apiv1work-ordersworkorderidproposals) | `operations:work_orders:read` | List technical defect proposals for work order |
| POST | [`/api/v1/work-orders/{workOrderId}/proposals`](#post-apiv1work-ordersworkorderidproposals) | `operations:proposals:create` | Submit defect proposal or hidden flaw discovered during bay inspection |
| POST | [`/api/v1/work-orders/{workOrderId}/proposals/{proposalId}/approve`](#post-apiv1work-ordersworkorderidproposalsproposalidapprove) | `operations:proposals:resolve` | Approve proposal converting it to formal authorized task |
| POST | [`/api/v1/work-orders/{workOrderId}/proposals/{proposalId}/notify`](#post-apiv1work-ordersworkorderidproposalsproposalidnotify) | `operations:proposals:notify` | Email a pending defect proposal with its photo evidence to the customer |
| POST | [`/api/v1/work-orders/{workOrderId}/proposals/{proposalId}/reject`](#post-apiv1work-ordersworkorderidproposalsproposalidreject) | `operations:proposals:resolve` | Reject proposal per customer decision |
| POST | [`/api/v1/work-orders/{workOrderId}/quotation/approve`](#post-apiv1work-ordersworkorderidquotationapprove) | `operations:quotations:approve` | Record the customer's acceptance: the work order starts (IN_PROGRESS) |
| GET | [`/api/v1/work-orders/{workOrderId}/quotation/pdf`](#get-apiv1work-ordersworkorderidquotationpdf) | `operations:quotations:read` | Download the quotation (proforma) PDF with labor, parts, IGV and validity |
| POST | [`/api/v1/work-orders/{workOrderId}/quotation/reject`](#post-apiv1work-ordersworkorderidquotationreject) | `operations:quotations:approve` | Record the customer's refusal: the work order is canceled and its materials released |
| POST | [`/api/v1/work-orders/{workOrderId}/quotation/send`](#post-apiv1work-ordersworkorderidquotationsend) | `operations:quotations:send` | Email the quotation PDF to the customer's contact email |
| POST | [`/api/v1/work-orders/{workOrderId}/start`](#post-apiv1work-ordersworkorderidstart) | `operations:work_orders:update` | Transition work order into active execution (IN_PROGRESS) |
| POST | [`/api/v1/work-orders/{workOrderId}/tasks`](#post-apiv1work-ordersworkorderidtasks) | `operations:work_orders:create` | Add authorized technical task to work order |

## Endpoints

### GET /api/v1/services

List all standard catalog services for tenant

- **Permiso:** `operations:services:read`
- **Respuesta exitosa:** 200 — `array<ServiceResource>`

### POST /api/v1/services

Register standard catalog service in workshop

- **Permiso:** `operations:services:manage`
- **Body** (application/json): `CreateServiceResource`
- **Respuesta exitosa:** 201 — `ServiceResource`

### GET /api/v1/services/{serviceId}

Get standard service catalog item by id

- **Permiso:** `operations:services:read`
- **Respuesta exitosa:** 200 — `ServiceResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `serviceId` | path | string(uuid) | sí |

### PUT /api/v1/services/{serviceId}

Update standard service catalog item

- **Permiso:** `operations:services:manage`
- **Body** (application/json): `UpdateServiceResource`
- **Respuesta exitosa:** 200 — `ServiceResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `serviceId` | path | string(uuid) | sí |

### GET /api/v1/tasks/my

List the open tasks assigned to the authenticated mechanic, in progress first

- **Permiso:** `operations:tasks:read`
- **Respuesta exitosa:** 200 — `array<AssignedTaskResource>`

### GET /api/v1/tasks/{taskId}

Get detailed technical task by id

- **Permiso:** `operations:tasks:read`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### PUT /api/v1/tasks/{taskId}

Update task parameters and assigned mechanic

- **Permiso:** `operations:work_orders:update`
- **Body** (application/json): `UpdateWorkOrderTaskResource`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/complete

Mark technical task as completed with actual labor hours

- **Permiso:** `operations:tasks:complete`
- **Body** (application/json): `CompleteTaskResource`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/evidence-images

Attach photographic evidence to technical task

- **Permiso:** `operations:tasks:upload_photos`
- **Body** (application/json): `AttachTaskEvidenceResource`
- **Respuesta exitosa:** 201 — `WorkOrderTaskImageResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/hold

Request suspension of the task for a defective or missing part (awaits administrative approval)

- **Permiso:** `operations:tasks:hold_request`
- **Body** (application/json): `HoldTaskResource`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/hold/approve

Approve the suspension request: the task goes ON_HOLD until the part arrives

- **Permiso:** `operations:tasks:hold_validate`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/hold/reject

Dismiss the suspension request: the task stays IN_PROGRESS

- **Permiso:** `operations:tasks:hold_validate`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### PUT /api/v1/tasks/{taskId}/mechanic

Assign or change mechanic for technical task

- **Permiso:** `operations:bays:reassign`
- **Body** (application/json): `AssignTaskMechanicResource`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/pause

Pause the labor clock for a break or the end of the shift (resume with /start)

- **Permiso:** `operations:tasks:track_time`
- **Body** (application/json): `PauseTaskResource`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/products

Add consumed product or spare part to technical task (a mechanic only to a task of their own)

- **Permiso:** `operations:work_orders:update` o `operations:tasks:charge_parts`
- **Body** (application/json): `AddTaskProductResource`
- **Respuesta exitosa:** 201 — `TaskProductResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### DELETE /api/v1/tasks/{taskId}/products/{productId}

Remove consumed product or spare part from technical task

- **Permiso:** `operations:work_orders:update`
- **Respuesta exitosa:** 204 — sin contenido

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |
| `productId` | path | string(uuid) | sí |

### PUT /api/v1/tasks/{taskId}/products/{productId}

Update quantity of consumed spare part

- **Permiso:** `operations:work_orders:update`
- **Body** (application/json): `UpdateTaskProductResource`
- **Respuesta exitosa:** 200 — `TaskProductResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |
| `productId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/reopen

Reopen completed task for quality rework

- **Permiso:** `operations:work_orders:update`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |
| `reason` | query | string | no |

### POST /api/v1/tasks/{taskId}/resume

Resume paused task after parts arrival

- **Permiso:** `operations:tasks:hold_validate`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### POST /api/v1/tasks/{taskId}/start

Start or resume execution of a technical task

- **Permiso:** `operations:tasks:track_time`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `taskId` | path | string(uuid) | sí |

### GET /api/v1/work-bays

List work bays by branch with availability filtering

- **Permiso:** `operations:bays:read`
- **Respuesta exitosa:** 200 — `array<WorkBayResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `branchId` | query | string(uuid) | no |
| `availableOnly` | query | boolean | no |

### POST /api/v1/work-bays

Register a new physical work bay in workshop branch

- **Permiso:** `operations:bays:manage`
- **Body** (application/json): `CreateWorkBayResource`
- **Respuesta exitosa:** 201 — `WorkBayResource`

### GET /api/v1/work-bays/{bayId}

Get physical work bay details by id

- **Permiso:** `operations:bays:read`
- **Respuesta exitosa:** 200 — `WorkBayResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `bayId` | path | string(uuid) | sí |

### PUT /api/v1/work-bays/{bayId}/maintenance

Place work bay under maintenance

- **Permiso:** `operations:bays:manage`
- **Body** (application/json): `MaintenanceBayResource`
- **Respuesta exitosa:** 200 — `WorkBayResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `bayId` | path | string(uuid) | sí |

### PUT /api/v1/work-bays/{bayId}/restore

Restore work bay to available status

- **Permiso:** `operations:bays:manage`
- **Respuesta exitosa:** 200 — `WorkBayResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `bayId` | path | string(uuid) | sí |

### GET /api/v1/work-orders

List and search work orders by branch or tenant

- **Permiso:** `operations:work_orders:read`
- **Respuesta exitosa:** 200 — `array<WorkOrderSummaryResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `branchId` | query | string(uuid) | no |

### POST /api/v1/work-orders

Open formal work order upon technical vehicle reception

- **Permiso:** `operations:work_orders:create`
- **Body** (application/json): `CreateWorkOrderResource`
- **Respuesta exitosa:** 201 — `WorkOrderResource`

### GET /api/v1/work-orders/{workOrderId}

Get exhaustive work order details with tasks, proposals, and images

- **Permiso:** `operations:work_orders:read`
- **Respuesta exitosa:** 200 — `WorkOrderDetailResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### PUT /api/v1/work-orders/{workOrderId}

Update verified mileage or diagnostic summary

- **Permiso:** `operations:work_orders:update`
- **Body** (application/json): `UpdateWorkOrderResource`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### DELETE /api/v1/work-orders/{workOrderId}/bay

Release and deallocate current work bay

- **Permiso:** `operations:bays:reassign`
- **Respuesta exitosa:** 204 — sin contenido

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### PUT /api/v1/work-orders/{workOrderId}/bay

Assign or relocate vehicle to physical work bay

- **Permiso:** `operations:bays:reassign`
- **Body** (application/json): `AssignWorkBayResource`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/cancel

Cancel work order releasing occupied bay and inventory reservations

- **Permiso:** `operations:work_orders:cancel`
- **Body** (application/json): `CancelWorkOrderResource`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/complete

Complete work order closing all technical bay operations

- **Permiso:** `operations:work_orders:update`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/deliver

Deliver vehicle back to owner following payment confirmation

- **Permiso:** `operations:vehicle_handover:execute`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### GET /api/v1/work-orders/{workOrderId}/exit-pass/pdf

Issue the vehicle exit pass (only for paid orders)

- **Permiso:** `invoicing:exit_passes:issue`
- **Respuesta exitosa:** 200 — archivo `application/pdf` (Content-Disposition con nombre de archivo)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/intake-images

Attach reception intake photograph metadata from Firebase Storage

- **Permiso:** `operations:inspections:upload_photos`
- **Body** (application/json): `AttachImageResource`
- **Respuesta exitosa:** 201 — `WorkOrderImageResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/mark-as-paid

Mark work order as formally paid enabling vehicle exit

- **Permiso:** `invoicing:payments:create`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### GET /api/v1/work-orders/{workOrderId}/part-movements

Audit trail of the parts charged to the work order and returned to their batch (US33)

- **Permiso:** `inventory:batches:read` o `inventory:batches:restore`
- **Respuesta exitosa:** 200 — `array<PartMovementResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### GET /api/v1/work-orders/{workOrderId}/profitability

Parts revenue against their FIFO cost of sales and gross profit of the work order (US35)

- **Permiso:** `inventory:batches:read` o `invoicing:cashflow:export_pdf`
- **Respuesta exitosa:** 200 — `WorkOrderProfitabilityResource` (un objeto, no un array)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### GET /api/v1/work-orders/{workOrderId}/proposals

List technical defect proposals for work order

- **Permiso:** `operations:work_orders:read`
- **Respuesta exitosa:** 200 — `array<TaskProposalResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/proposals

Submit defect proposal or hidden flaw discovered during bay inspection

- **Permiso:** `operations:proposals:create`
- **Body** (application/json): `SubmitTaskProposalResource`
- **Respuesta exitosa:** 201 — `TaskProposalResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/proposals/{proposalId}/approve

Approve proposal converting it to formal authorized task

- **Permiso:** `operations:proposals:resolve`
- **Body** (application/json): `ApproveTaskProposalResource`
- **Respuesta exitosa:** 200 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |
| `proposalId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/proposals/{proposalId}/notify

Email a pending defect proposal with its photo evidence to the customer

- **Permiso:** `operations:proposals:notify`
- **Respuesta exitosa:** 200 — `{ message, recipient }`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |
| `proposalId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/proposals/{proposalId}/reject

Reject proposal per customer decision

- **Permiso:** `operations:proposals:resolve`
- **Body** (application/json): `RejectTaskProposalResource`
- **Respuesta exitosa:** 200 — `TaskProposalResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |
| `proposalId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/quotation/approve

Record the customer's acceptance: the work order starts (IN_PROGRESS)

- **Permiso:** `operations:quotations:approve`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### GET /api/v1/work-orders/{workOrderId}/quotation/pdf

Download the quotation (proforma) PDF with labor, parts, IGV and validity

- **Permiso:** `operations:quotations:read`
- **Respuesta exitosa:** 200 — archivo `application/pdf` (Content-Disposition con nombre de archivo)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/quotation/reject

Record the customer's refusal: the work order is canceled and its materials released

- **Permiso:** `operations:quotations:approve`
- **Body** (application/json): `CancelWorkOrderResource`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/quotation/send

Email the quotation PDF to the customer's contact email

- **Permiso:** `operations:quotations:send`
- **Respuesta exitosa:** 200 — `{ message, recipient }` — recipient = correo del cliente

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/start

Transition work order into active execution (IN_PROGRESS)

- **Permiso:** `operations:work_orders:update`
- **Respuesta exitosa:** 200 — `WorkOrderResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

### POST /api/v1/work-orders/{workOrderId}/tasks

Add authorized technical task to work order

- **Permiso:** `operations:work_orders:create`
- **Body** (application/json): `CreateWorkOrderTaskResource`
- **Respuesta exitosa:** 201 — `WorkOrderTaskResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `workOrderId` | path | string(uuid) | sí |

## Modelos

### ServiceResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `name` | string |
| `basePrice` | number |
| `currency` | string |
| `estimatedMinutes` | integer |

### CreateServiceResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `basePrice` | number | sí | ≥ 0 |
| `currency` | string | sí | min 1, patrón `PEN\|USD` |
| `name` | string | sí | max 150 |
| `estimatedMinutes` | integer(int32) | no | ≥ 0 |

### UpdateServiceResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `basePrice` | number | sí | ≥ 0 |
| `currency` | string | sí | min 1, patrón `PEN\|USD` |
| `name` | string | sí | max 150 |
| `estimatedMinutes` | integer(int32) | no | ≥ 0 |

### AssignedTaskResource

| Campo | Tipo |
| --- | --- |
| `taskId` | string(uuid) |
| `workOrderId` | string(uuid) |
| `workOrderNumber` | integer |
| `workOrderStatus` | string |
| `vehicleId` | string(uuid) |
| `branchId` | string(uuid) |
| `bayId` | string(uuid) |
| `status` | string |
| `description` | string |
| `estimatedHours` | number |
| `startedAt` | string(date-time, UTC) |
| `totalPausedSeconds` | integer |

### WorkOrderTaskResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `workOrderId` | string(uuid) |
| `serviceId` | string(uuid) |
| `serviceName` | string |
| `mechanicId` | string(uuid) |
| `mechanicName` | string |
| `status` | string |
| `description` | string |
| `price` | number |
| `currency` | string |
| `holdReason` | string |
| `missingItemDescription` | string |
| `totalPausedSeconds` | integer |
| `startedAt` | string(date-time, UTC) |
| `completedAt` | string(date-time, UTC) |
| `pendingSuspensionReason` | string |
| `pendingSuspensionRequestedAt` | string(date-time, UTC) |
| `products` | array<TaskProductResource> |
| `evidenceImages` | array<WorkOrderTaskImageResource> |

### UpdateWorkOrderTaskResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `description` | string | no | max 1000 |
| `mechanicId` | string(uuid) | no | membershipId del mecánico |
| `price` | number | no | ≥ 0 |

### CompleteTaskResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `actualLaborHours` | number | sí | ≥ 0 |
| `notes` | string | no | max 1000 |

### AttachTaskEvidenceResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `imageUrl` | string | sí | min 1 |
| `description` | string | no | max 500 |

### WorkOrderTaskImageResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `taskId` | string(uuid) |
| `imageUrl` | string |
| `description` | string |
| `uploadedAt` | string(date-time, UTC) |

### HoldTaskResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `missingItemDescription` | string | sí | max 500 |
| `inventoryItemId` | string(uuid) | no |  |

### AssignTaskMechanicResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `mechanicId` | string(uuid) | sí | membershipId del mecánico |

### PauseTaskResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `reason` | string | sí | min 1, patrón `BREAK\|END_OF_SHIFT` |

### AddTaskProductResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `productId` | string(uuid) | sí |  |
| `quantity` | number | sí | ≥ 0 |
| `currency` | string | no | patrón `PEN\|USD` |
| `unitPrice` | number | no | ≥ 0 |

### TaskProductResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `taskId` | string(uuid) |
| `productId` | string(uuid) |
| `productName` | string |
| `quantity` | number |
| `unitPrice` | number |
| `totalAmount` | number |
| `currency` | string |

### UpdateTaskProductResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `quantity` | number | sí | ≥ 0 |

### WorkBayResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `branchId` | string(uuid) |
| `name` | string |
| `bayType` | string |
| `status` | string |
| `currentWorkOrderId` | string(uuid) |

### CreateWorkBayResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `bayType` | string | sí | min 1, patrón `LIFT\|PAINT_BOOTH\|WASHING\|ALIGNMENT\|MECHANICAL_LIFT\|WASH_BAY\|DIAGNOSTIC_PIT\|ALIGNMENT_STATION` |
| `branchId` | string(uuid) | sí |  |
| `name` | string | sí | max 100 |

### MaintenanceBayResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `reason` | string | sí | max 1000 |

### WorkOrderSummaryResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `internalNumber` | integer |
| `vehicleId` | string(uuid) |
| `customerId` | string(uuid) |
| `branchId` | string(uuid) |
| `currentBayId` | string(uuid) |
| `bayName` | string |
| `status` | string |
| `totalAmount` | number |
| `currency` | string |
| `createdAt` | string(date-time, UTC) |

### CreateWorkOrderResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `mileageIn` | integer(int32) | sí | ≥ 0 |
| `vehicleId` | string(uuid) | sí |  |
| `appointmentId` | string(uuid) | no |  |
| `branchId` | string(uuid) | no |  |
| `customerId` | string(uuid) | no |  |
| `diagnosticSummary` | string | no | max 2000 |

### WorkOrderResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `internalNumber` | integer |
| `vehicleId` | string(uuid) |
| `customerId` | string(uuid) |
| `branchId` | string(uuid) |
| `currentBayId` | string(uuid) |
| `mileageIn` | integer |
| `status` | string |
| `subtotal` | number |
| `taxAmount` | number |
| `totalAmount` | number |
| `currency` | string |

### WorkOrderDetailResource

> Nombres de referencia sin valor real: `tasks[].serviceName` = "Standard Service", `proposals[].serviceName` = "Suggested Service", `mechanicName` = `null`, `products[].productName` = "Product {uuid}". Resolver el servicio con `GET /services` (hueco H11 de [03](../03-traceability.md#huecos-abiertos)). Una tarea con `pendingSuspensionRequestedAt` no nulo tiene una solicitud de suspensión pendiente (H5).

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `internalNumber` | integer |
| `vehicleId` | string(uuid) |
| `customerId` | string(uuid) |
| `branchId` | string(uuid) |
| `currentBayId` | string(uuid) |
| `bayName` | string |
| `mileageIn` | integer |
| `diagnosticSummary` | string |
| `status` | string |
| `subtotal` | number |
| `taxAmount` | number |
| `totalAmount` | number |
| `currency` | string |
| `tasks` | array<WorkOrderTaskResource> |
| `proposals` | array<TaskProposalResource> |
| `intakeImages` | array<WorkOrderImageResource> |
| `createdAt` | string(date-time, UTC) |
| `updatedAt` | string(date-time, UTC) |

### UpdateWorkOrderResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `diagnosticSummary` | string | no | max 2000 |
| `mileageIn` | integer(int32) | no | ≥ 0 |

### AssignWorkBayResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `bayId` | string(uuid) | sí |  |

### CancelWorkOrderResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `reason` | string | sí | max 1000 |

### AttachImageResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `imageUrl` | string | sí | min 1 |
| `description` | string | no | max 500 |

### WorkOrderImageResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `workOrderId` | string(uuid) |
| `imageUrl` | string |
| `description` | string |
| `uploadedAt` | string(date-time, UTC) |

### PartMovementResource

| Campo | Tipo |
| --- | --- |
| `allocationId` | string(uuid) |
| `productId` | string(uuid) |
| `taskId` | string(uuid) |
| `batchId` | string(uuid) |
| `chargedQuantity` | number |
| `returnedQuantity` | number |
| `unitCost` | number |
| `currency` | string |
| `chargedAt` | string(date-time, UTC) |
| `lastReturnedAt` | string(date-time, UTC) |
| `lastReturnedBy` | string(uuid) |
| `lastReturnReason` | string |

### WorkOrderProfitabilityResource

| Campo | Tipo |
| --- | --- |
| `workOrderId` | string(uuid) |
| `laborRevenue` | number |
| `partsRevenue` | number |
| `partsCostOfSales` | number |
| `partsGrossProfit` | number |
| `subtotal` | number |
| `currency` | string |
| `lines` | array<PartProfitabilityResource> |

### TaskProposalResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `workOrderId` | string(uuid) |
| `taskId` | string(uuid) |
| `serviceId` | string(uuid) |
| `serviceName` | string |
| `mechanicId` | string(uuid) |
| `mechanicName` | string |
| `description` | string |
| `severity` | string |
| `imageUrl` | string |
| `status` | string |
| `customerNotes` | string |
| `createdAt` | string(date-time, UTC) |
| `updatedAt` | string(date-time, UTC) |

### SubmitTaskProposalResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `description` | string | sí | max 2000 |
| `imageUrl` | string | sí | min 1 |
| `mechanicId` | string(uuid) | sí | membershipId del mecánico (en la app: el de la sesión) |
| `severity` | string | sí | min 1, patrón `LOW\|MEDIUM\|CRITICAL` |
| `suggestedServiceId` | string(uuid) | no |  |

### ApproveTaskProposalResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `finalPrice` | number | sí | ≥ 0 |
| `laborHours` | number | sí | ≥ 0 |
| `serviceId` | string(uuid) | sí |  |
| `mechanicId` | string(uuid) | no | membershipId del mecánico |
| `notes` | string | no | max 1000 |

### RejectTaskProposalResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `customerNotes` | string | sí | max 1000 |

### CreateWorkOrderTaskResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `description` | string | sí | max 1000 |
| `serviceId` | string(uuid) | sí |  |
| `currency` | string | no | patrón `PEN\|USD` |
| `mechanicId` | string(uuid) | no | membershipId del mecánico |
| `price` | number | no | ≥ 0 |

### PartProfitabilityResource

| Campo | Tipo |
| --- | --- |
| `taskId` | string(uuid) |
| `productId` | string(uuid) |
| `quantity` | number |
| `unitPrice` | number |
| `revenue` | number |
| `costOfSales` | number |
| `grossProfit` | number |

