# CRM — Clientes, vehículos y citas

Clientes (persona natural o empresa), afiliación, vehículos, titularidad y citas.

Convenciones comunes (autenticación, errores, cabeceras, fechas, dinero): ver [README.md](README.md).

## Índice

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | [`/api/v1/appointments`](#get-apiv1appointments) | `crm:appointments:read` | Query appointments for branch and date agenda |
| POST | [`/api/v1/appointments`](#post-apiv1appointments) | `crm:appointments:create` | Schedule a service appointment |
| GET | [`/api/v1/appointments/{id}`](#get-apiv1appointmentsid) | `crm:appointments:read` | Retrieve appointment details by identifier |
| POST | [`/api/v1/appointments/{id}/arrive`](#post-apiv1appointmentsidarrive) | `crm:appointments:update` | Mark appointment as arrived at the workshop bay |
| POST | [`/api/v1/appointments/{id}/cancel`](#post-apiv1appointmentsidcancel) | `crm:appointments:update` | Cancel an appointment recording a justification |
| POST | [`/api/v1/appointments/{id}/confirm`](#post-apiv1appointmentsidconfirm) | `crm:appointments:update` | Confirm pending appointment |
| POST | [`/api/v1/appointments/{id}/reschedule`](#post-apiv1appointmentsidreschedule) | `crm:appointments:update` | Reschedule an existing appointment to a new date and time |
| GET | [`/api/v1/customers`](#get-apiv1customers) | `crm:customers:read` | List and search customer portfolio of active workshop |
| GET | [`/api/v1/customers/affiliation-lookup`](#get-apiv1customersaffiliation-lookup) | `crm:customers:read` | Look up an email: existing workshop customer, platform account and its vehicles |
| POST | [`/api/v1/customers/affiliations`](#post-apiv1customersaffiliations) | `crm:customers:create` | Affiliate a platform account as customer, linking the vehicles it already registered |
| POST | [`/api/v1/customers/companies`](#post-apiv1customerscompanies) | `crm:customers:create` | Register a corporate company customer |
| POST | [`/api/v1/customers/individuals`](#post-apiv1customersindividuals) | `crm:customers:create` | Register a natural person (individual) customer |
| GET | [`/api/v1/customers/{customerId}/memberships`](#get-apiv1customerscustomeridmemberships) | `crm:customers:read` | List corporate fleet members assigned to customer |
| POST | [`/api/v1/customers/{customerId}/memberships`](#post-apiv1customerscustomeridmemberships) | `crm:fleets:manage` | Invite or assign user to corporate customer fleet |
| DELETE | [`/api/v1/customers/{customerId}/memberships/{userId}`](#delete-apiv1customerscustomeridmembershipsuserid) | `crm:fleets:manage` | Revoke corporate fleet membership from user |
| GET | [`/api/v1/customers/{id}`](#get-apiv1customersid) | `crm:customers:read` | Retrieve customer profile by identifier |
| PUT | [`/api/v1/customers/{id}/contact`](#put-apiv1customersidcontact) | `crm:customers:update` | Update customer contact phone and email |
| GET | [`/api/v1/customers/{id}/vehicles`](#get-apiv1customersidvehicles) | `crm:customers:read` | List vehicles currently under active ownership of a customer |
| POST | [`/api/v1/vehicles`](#post-apiv1vehicles) | `crm:vehicles:create` | Register universal vehicle with initial customer or driver ownership |
| GET | [`/api/v1/vehicles/by-plate/{plate}`](#get-apiv1vehiclesby-plateplate) | `crm:vehicles:read` | Retrieve vehicle by normalized license plate |
| GET | [`/api/v1/vehicles/my-vehicles`](#get-apiv1vehiclesmy-vehicles) | autenticado (sin permiso específico) | Retrieve vehicles registered to or actively operated by authenticated user |
| GET | [`/api/v1/vehicles/{id}`](#get-apiv1vehiclesid) | `crm:vehicles:read` | Retrieve vehicle specifications by identifier |
| GET | [`/api/v1/vehicles/{id}/ownerships`](#get-apiv1vehiclesidownerships) | `crm:vehicles:read` | Retrieve chronological ownership transfer history of a vehicle |
| POST | [`/api/v1/vehicles/{id}/ownerships`](#post-apiv1vehiclesidownerships) | `crm:vehicles:update` | Transfer vehicle ownership creating a new custody period |

## Endpoints

### GET /api/v1/appointments

Query appointments for branch and date agenda

- **Permiso:** `crm:appointments:read`
- **Respuesta exitosa:** 200 — `array<AppointmentResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `branchId` | query | string(uuid) | sí |
| `date` | query | string(date) | no |
| `status` | query | string | no |

### POST /api/v1/appointments

Schedule a service appointment

- **Permiso:** `crm:appointments:create`
- **Body** (application/json): `ScheduleAppointmentResource`
- **Respuesta exitosa:** 201 — `AppointmentResource`

### GET /api/v1/appointments/{id}

Retrieve appointment details by identifier

- **Permiso:** `crm:appointments:read`
- **Respuesta exitosa:** 200 — `AppointmentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### POST /api/v1/appointments/{id}/arrive

Mark appointment as arrived at the workshop bay

- **Permiso:** `crm:appointments:update`
- **Respuesta exitosa:** 200 — `AppointmentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### POST /api/v1/appointments/{id}/cancel

Cancel an appointment recording a justification

- **Permiso:** `crm:appointments:update`
- **Body** (application/json): `CancelAppointmentResource`
- **Respuesta exitosa:** 200 — `AppointmentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### POST /api/v1/appointments/{id}/confirm

Confirm pending appointment

- **Permiso:** `crm:appointments:update`
- **Respuesta exitosa:** 200 — `AppointmentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### POST /api/v1/appointments/{id}/reschedule

Reschedule an existing appointment to a new date and time

- **Permiso:** `crm:appointments:update`
- **Body** (application/json): `RescheduleAppointmentResource`
- **Respuesta exitosa:** 200 — `AppointmentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### GET /api/v1/customers

List and search customer portfolio of active workshop

- **Permiso:** `crm:customers:read`
- **Respuesta exitosa:** 200 — `array<CustomerResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `type` | query | string | no |
| `search` | query | string | no |
| `status` | query | string | no |
| `page` | query | integer(int32) | no |
| `size` | query | integer(int32) | no |

### GET /api/v1/customers/affiliation-lookup

Look up an email: existing workshop customer, platform account and its vehicles

- **Permiso:** `crm:customers:read`
- **Respuesta exitosa:** 200 — `{ workshopCustomer: CustomerResource | null, platformAccount: { userId, email, fullName, status } | null, vehicles: array<VehicleResource>, nextStep: "ALREADY_CUSTOMER" | "AFFILIATE" | "REGISTER_WALK_IN" }` (ver modelos `CustomerResource` y `VehicleResource`)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `email` | query | string | sí |

### POST /api/v1/customers/affiliations

Affiliate a platform account as customer, linking the vehicles it already registered

- **Permiso:** `crm:customers:create`
- **Body** (application/json): `AffiliateCustomerResource`
- **Respuesta exitosa:** 201 — `CustomerResource`

### POST /api/v1/customers/companies

Register a corporate company customer

- **Permiso:** `crm:customers:create`
- **Body** (application/json): `CreateCompanyCustomerResource`
- **Respuesta exitosa:** 201 — `CustomerResource`

### POST /api/v1/customers/individuals

Register a natural person (individual) customer

- **Permiso:** `crm:customers:create`
- **Body** (application/json): `CreateIndividualCustomerResource`
- **Respuesta exitosa:** 201 — `CustomerResource`

### GET /api/v1/customers/{customerId}/memberships

List corporate fleet members assigned to customer

- **Permiso:** `crm:customers:read`
- **Respuesta exitosa:** 200 — `array<CustomerMembershipResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `customerId` | path | string(uuid) | sí |

### POST /api/v1/customers/{customerId}/memberships

Invite or assign user to corporate customer fleet

- **Permiso:** `crm:fleets:manage`
- **Body** (application/json): `InviteCustomerMemberResource`
- **Respuesta exitosa:** 201 — `CustomerMembershipResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `customerId` | path | string(uuid) | sí |

### DELETE /api/v1/customers/{customerId}/memberships/{userId}

Revoke corporate fleet membership from user

- **Permiso:** `crm:fleets:manage`
- **Respuesta exitosa:** 204 — sin contenido

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `customerId` | path | string(uuid) | sí |
| `userId` | path | string(uuid) | sí |

### GET /api/v1/customers/{id}

Retrieve customer profile by identifier

- **Permiso:** `crm:customers:read`
- **Respuesta exitosa:** 200 — `CustomerResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### PUT /api/v1/customers/{id}/contact

Update customer contact phone and email

- **Permiso:** `crm:customers:update`
- **Body** (application/json): `UpdateCustomerContactResource`
- **Respuesta exitosa:** 200 — `CustomerResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### GET /api/v1/customers/{id}/vehicles

List vehicles currently under active ownership of a customer

- **Permiso:** `crm:customers:read`
- **Respuesta exitosa:** 200 — `array<VehicleResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### POST /api/v1/vehicles

Register universal vehicle with initial customer or driver ownership

- **Permiso:** `crm:vehicles:create`
- **Body** (application/json): `CreateVehicleResource`
- **Respuesta exitosa:** 201 — `VehicleResource`

### GET /api/v1/vehicles/by-plate/{plate}

Retrieve vehicle by normalized license plate

- **Permiso:** `crm:vehicles:read`
- **Respuesta exitosa:** 200 — `VehicleResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `plate` | path | string | sí |

### GET /api/v1/vehicles/my-vehicles

Retrieve vehicles registered to or actively operated by authenticated user

- **Permiso:** autenticado (sin permiso específico)
- **Respuesta exitosa:** 200 — `array<VehicleResource>`

### GET /api/v1/vehicles/{id}

Retrieve vehicle specifications by identifier

- **Permiso:** `crm:vehicles:read`
- **Respuesta exitosa:** 200 — `VehicleResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### GET /api/v1/vehicles/{id}/ownerships

Retrieve chronological ownership transfer history of a vehicle

- **Permiso:** `crm:vehicles:read`
- **Respuesta exitosa:** 200 — `array<VehicleOwnershipResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

### POST /api/v1/vehicles/{id}/ownerships

Transfer vehicle ownership creating a new custody period

- **Permiso:** `crm:vehicles:update`
- **Body** (application/json): `TransferVehicleOwnershipResource`
- **Respuesta exitosa:** 201 — `VehicleResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

## Modelos

### AppointmentResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `branchId` | string(uuid) |
| `customerId` | string(uuid) |
| `vehicleId` | string(uuid) |
| `scheduledAt` | string(date-time, UTC) |
| `estimatedDurationMinutes` | integer |
| `reason` | string |
| `status` | string |
| `cancellationReason` | string |

### ScheduleAppointmentResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `branchId` | string(uuid) | sí |  |
| `customerId` | string(uuid) | sí |  |
| `scheduledAt` | string(date-time) | sí |  |
| `vehicleId` | string(uuid) | sí |  |
| `estimatedDurationMinutes` | integer(int32) | no | ≥ 15 |
| `reason` | string | no | max 1000 |

### CancelAppointmentResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `cancellationReason` | string | sí | max 500 |

### RescheduleAppointmentResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `newScheduledAt` | string(date-time) | sí |  |

### CustomerResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `type` | string |
| `firstName` | string |
| `lastName` | string |
| `companyName` | string |
| `displayName` | string |
| `taxId` | string |
| `email` | string |
| `phone` | string |
| `status` | string |

### VehicleResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `plate` | string |
| `vin` | string |
| `brand` | string |
| `model` | string |
| `year` | integer |
| `engineType` | string |
| `currentOwnerId` | string(uuid) |
| `currentMileage` | integer |

### AffiliateCustomerResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `email` | string(email) | sí | min 1 |
| `taxId` | string | sí | min 1 |
| `phone` | string | no |  |

### CreateCompanyCustomerResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `companyName` | string | sí | max 150 |
| `taxId` | string | sí | min 11, max 20 |
| `email` | string(email) | no | max 150 |
| `phone` | string | no | max 20 |

### CreateIndividualCustomerResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `firstName` | string | sí | max 100 |
| `lastName` | string | sí | max 100 |
| `taxId` | string | sí | min 8, max 20 |
| `email` | string(email) | no | max 150 |
| `phone` | string | no | max 20 |

### CustomerMembershipResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `customerId` | string(uuid) |
| `userId` | string(uuid) |
| `role` | string |
| `status` | string |

### InviteCustomerMemberResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `role` | string | sí | min 1 |
| `userId` | string(uuid) | sí |  |

### UpdateCustomerContactResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `email` | string(email) | no | max 150 |
| `phone` | string | no | max 20 |

### CreateVehicleResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `brand` | string | sí | max 50 |
| `engineType` | string | sí |  |
| `model` | string | sí | max 50 |
| `plate` | string | sí | max 15 |
| `customerId` | string(uuid) | no |  |
| `userId` | string(uuid) | no |  |
| `vin` | string | no | max 17 |
| `year` | integer(int32) | no | ≥ 1950 |

### VehicleOwnershipResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `vehicleId` | string(uuid) |
| `customerId` | string(uuid) |
| `userId` | string(uuid) |
| `startDate` | string(date) |
| `endDate` | string(date) |

### TransferVehicleOwnershipResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `newOwnerId` | string(uuid) | sí |  |
| `transferDate` | string(date) | sí |  |

