# HR — Asistencia, turnos, empleados y planilla

Marcación con geocerca, justificaciones, turnos, perfiles de empleado y planilla.

Convenciones comunes (autenticación, errores, cabeceras, fechas, dinero): ver [README.md](README.md).

## Índice

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | [`/api/v1/hr/attendances/branch/{branchId}/daily`](#get-apiv1hrattendancesbranchbranchiddaily) | `hr:attendance:audit_all` | List attendance records for a specific branch and date |
| POST | [`/api/v1/hr/attendances/clock-in`](#post-apiv1hrattendancesclock-in) | `hr:attendance:clock_in` | Record physical clock-in with geofenced location |
| POST | [`/api/v1/hr/attendances/clock-out`](#post-apiv1hrattendancesclock-out) | `hr:attendance:clock_out` | Record physical clock-out |
| GET | [`/api/v1/hr/attendances/employee/{membershipId}/history`](#get-apiv1hrattendancesemployeemembershipidhistory) | `hr:attendance:audit_all` o `hr:attendance:read_own` | Get employee attendance history within a date range |
| GET | [`/api/v1/hr/attendances/employee/{membershipId}/status-today`](#get-apiv1hrattendancesemployeemembershipidstatus-today) | `hr:attendance:audit_all` o `hr:attendance:read_own` | Get active clock-in status for an employee today |
| POST | [`/api/v1/hr/attendances/{attendanceId}/justification`](#post-apiv1hrattendancesattendanceidjustification) | `hr:justifications:create` | Submit the reason of an own late or absent attendance for administrative review |
| POST | [`/api/v1/hr/attendances/{attendanceId}/justification/approve`](#post-apiv1hrattendancesattendanceidjustificationapprove) | `hr:justifications:approve` | Approve a submitted justification: the attendance becomes EXCUSED |
| POST | [`/api/v1/hr/attendances/{attendanceId}/justification/reject`](#post-apiv1hrattendancesattendanceidjustificationreject) | `hr:justifications:approve` | Dismiss a submitted justification: the attendance keeps its late or absent status |
| POST | [`/api/v1/hr/attendances/{attendanceId}/justify`](#post-apiv1hrattendancesattendanceidjustify) | `hr:justifications:approve` | Excuse a late or absent attendance directly (administrative justification) |
| POST | [`/api/v1/hr/employees`](#post-apiv1hremployees) | `hr:employees:manage` | Register and formalize an employee profile |
| GET | [`/api/v1/hr/employees/branch/{branchId}`](#get-apiv1hremployeesbranchbranchid) | `iam:members:read` | List all employee profiles assigned to a workshop branch |
| GET | [`/api/v1/hr/employees/membership/{membershipId}`](#get-apiv1hremployeesmembershipmembershipid) | `iam:members:read` o `hr:attendance:read_own` | Get employee profile by IAM membership ID |
| GET | [`/api/v1/hr/employees/{profileId}`](#get-apiv1hremployeesprofileid) | `iam:members:read` | Get employee profile by ID |
| PUT | [`/api/v1/hr/employees/{profileId}/salary`](#put-apiv1hremployeesprofileidsalary) | `iam:members:compensate` | Update employee base salary and compensation scheme |
| PUT | [`/api/v1/hr/employees/{profileId}/shift`](#put-apiv1hremployeesprofileidshift) | `hr:shifts:manage` | Assign or update scheduled work shift for an employee |
| PATCH | [`/api/v1/hr/employees/{profileId}/status`](#patch-apiv1hremployeesprofileidstatus) | `hr:employees:manage` | Update employment status (ACTIVE, ON_LEAVE, TERMINATED) |
| GET | [`/api/v1/hr/payrolls`](#get-apiv1hrpayrolls) | `hr:payroll:manage` | List all payroll payments with optional date and status filters |
| GET | [`/api/v1/hr/payrolls/export/sunat-rem`](#get-apiv1hrpayrollsexportsunat-rem) | `hr:plame:export` | Export SUNAT PLAME structured .rem text file (Formato 0601) |
| POST | [`/api/v1/hr/payrolls/generate`](#post-apiv1hrpayrollsgenerate) | `hr:payroll:manage` | Generate a new payroll draft settlement |
| GET | [`/api/v1/hr/payrolls/{payrollId}`](#get-apiv1hrpayrollspayrollid) | `hr:payroll:manage` | Get detailed payroll payment by ID |
| POST | [`/api/v1/hr/payrolls/{payrollId}/approve`](#post-apiv1hrpayrollspayrollidapprove) | `hr:payroll:manage` | Calculate and formally approve payroll settlement |
| POST | [`/api/v1/hr/payrolls/{payrollId}/bonuses`](#post-apiv1hrpayrollspayrollidbonuses) | `hr:payroll:manage` | Add bonus or commission to payroll draft |
| POST | [`/api/v1/hr/payrolls/{payrollId}/deductions`](#post-apiv1hrpayrollspayrolliddeductions) | `hr:payroll:manage` | Add deduction or withholding to payroll draft |
| POST | [`/api/v1/hr/payrolls/{payrollId}/disburse`](#post-apiv1hrpayrollspayrolliddisburse) | `hr:payroll:manage` | Mark payroll as disbursed and paid with bank reference |
| GET | [`/api/v1/hr/work-shifts`](#get-apiv1hrwork-shifts) | `hr:shifts:read` | List all work shifts |
| POST | [`/api/v1/hr/work-shifts`](#post-apiv1hrwork-shifts) | `hr:shifts:manage` | Create a new work shift |
| GET | [`/api/v1/hr/work-shifts/{shiftId}`](#get-apiv1hrwork-shiftsshiftid) | `hr:shifts:read` | Get work shift by ID |
| PUT | [`/api/v1/hr/work-shifts/{shiftId}`](#put-apiv1hrwork-shiftsshiftid) | `hr:shifts:manage` | Update an existing work shift |
| PATCH | [`/api/v1/hr/work-shifts/{shiftId}/activate`](#patch-apiv1hrwork-shiftsshiftidactivate) | `hr:shifts:manage` | Activate a work shift |
| PATCH | [`/api/v1/hr/work-shifts/{shiftId}/deactivate`](#patch-apiv1hrwork-shiftsshiftiddeactivate) | `hr:shifts:manage` | Deactivate a work shift |

## Endpoints

### GET /api/v1/hr/attendances/branch/{branchId}/daily

List attendance records for a specific branch and date

- **Permiso:** `hr:attendance:audit_all`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<AttendanceResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `branchId` | path | string(uuid) | sí |
| `date` | query | string(date) | no |

### POST /api/v1/hr/attendances/clock-in

Record physical clock-in with geofenced location

- **Permiso:** `hr:attendance:clock_in`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `ClockInRequest`
- **Respuesta exitosa:** 201 — `AttendanceResource`

### POST /api/v1/hr/attendances/clock-out

Record physical clock-out

- **Permiso:** `hr:attendance:clock_out`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `ClockOutRequest` (opcional)
- **Respuesta exitosa:** 200 — `AttendanceResource`

### GET /api/v1/hr/attendances/employee/{membershipId}/history

Get employee attendance history within a date range

- **Permiso:** `hr:attendance:audit_all` o `hr:attendance:read_own`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<AttendanceResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `membershipId` | path | string(uuid) | sí |
| `startDate` | query | string(date) | sí |
| `endDate` | query | string(date) | sí |

### GET /api/v1/hr/attendances/employee/{membershipId}/status-today

Get active clock-in status for an employee today

- **Permiso:** `hr:attendance:audit_all` o `hr:attendance:read_own`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `AttendanceResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `membershipId` | path | string(uuid) | sí |

### POST /api/v1/hr/attendances/{attendanceId}/justification

Submit the reason of an own late or absent attendance for administrative review

- **Permiso:** `hr:justifications:create`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `JustifyAttendanceRequest`
- **Respuesta exitosa:** 200 — `AttendanceResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `attendanceId` | path | string(uuid) | sí |

### POST /api/v1/hr/attendances/{attendanceId}/justification/approve

Approve a submitted justification: the attendance becomes EXCUSED

- **Permiso:** `hr:justifications:approve`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `AttendanceResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `attendanceId` | path | string(uuid) | sí |

### POST /api/v1/hr/attendances/{attendanceId}/justification/reject

Dismiss a submitted justification: the attendance keeps its late or absent status

- **Permiso:** `hr:justifications:approve`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `AttendanceResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `attendanceId` | path | string(uuid) | sí |

### POST /api/v1/hr/attendances/{attendanceId}/justify

Excuse a late or absent attendance directly (administrative justification)

- **Permiso:** `hr:justifications:approve`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `JustifyAttendanceRequest`
- **Respuesta exitosa:** 200 — `AttendanceResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `attendanceId` | path | string(uuid) | sí |

### POST /api/v1/hr/employees

Register and formalize an employee profile

- **Permiso:** `hr:employees:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `RegisterEmployeeProfileRequest`
- **Respuesta exitosa:** 201 — `EmployeeProfileResource`

### GET /api/v1/hr/employees/branch/{branchId}

List all employee profiles assigned to a workshop branch

- **Permiso:** `iam:members:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<EmployeeProfileResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `branchId` | path | string(uuid) | sí |

### GET /api/v1/hr/employees/membership/{membershipId}

Get employee profile by IAM membership ID

- **Permiso:** `iam:members:read` o `hr:attendance:read_own`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `EmployeeProfileResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `membershipId` | path | string(uuid) | sí |

### GET /api/v1/hr/employees/{profileId}

Get employee profile by ID

- **Permiso:** `iam:members:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `EmployeeProfileResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `profileId` | path | string(uuid) | sí |

### PUT /api/v1/hr/employees/{profileId}/salary

Update employee base salary and compensation scheme

- **Permiso:** `iam:members:compensate`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `UpdateSalaryRequest`
- **Respuesta exitosa:** 200 — `EmployeeProfileResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `profileId` | path | string(uuid) | sí |

### PUT /api/v1/hr/employees/{profileId}/shift

Assign or update scheduled work shift for an employee

- **Permiso:** `hr:shifts:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `AssignShiftRequest`
- **Respuesta exitosa:** 200 — `EmployeeProfileResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `profileId` | path | string(uuid) | sí |

### PATCH /api/v1/hr/employees/{profileId}/status

Update employment status (ACTIVE, ON_LEAVE, TERMINATED)

- **Permiso:** `hr:employees:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `UpdateEmploymentStatusRequest`
- **Respuesta exitosa:** 200 — `EmployeeProfileResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `profileId` | path | string(uuid) | sí |

### GET /api/v1/hr/payrolls

List all payroll payments with optional date and status filters

- **Permiso:** `hr:payroll:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — página Spring (`content: array<PayrollPaymentSummaryResource>`, `totalElements`, `totalPages`, `number`, `size`)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `periodStart` | query | string(date) | no |
| `periodEnd` | query | string(date) | no |
| `status` | query | string | no |
| `pageable` | query | Pageable | sí |

### GET /api/v1/hr/payrolls/export/sunat-rem

Export SUNAT PLAME structured .rem text file (Formato 0601)

- **Permiso:** `hr:plame:export`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — archivo `0601<periodo>.rem` (text/plain; ISO-8859-1)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `period` | query | string | sí |
| `branchId` | query | string(uuid) | no |

### POST /api/v1/hr/payrolls/generate

Generate a new payroll draft settlement

- **Permiso:** `hr:payroll:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `GeneratePayrollRequest`
- **Respuesta exitosa:** 201 — `PayrollPaymentResource`

### GET /api/v1/hr/payrolls/{payrollId}

Get detailed payroll payment by ID

- **Permiso:** `hr:payroll:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `PayrollPaymentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `payrollId` | path | string(uuid) | sí |

### POST /api/v1/hr/payrolls/{payrollId}/approve

Calculate and formally approve payroll settlement

- **Permiso:** `hr:payroll:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `PayrollPaymentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `payrollId` | path | string(uuid) | sí |

### POST /api/v1/hr/payrolls/{payrollId}/bonuses

Add bonus or commission to payroll draft

- **Permiso:** `hr:payroll:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `AddPayrollBonusRequest`
- **Respuesta exitosa:** 200 — `PayrollPaymentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `payrollId` | path | string(uuid) | sí |

### POST /api/v1/hr/payrolls/{payrollId}/deductions

Add deduction or withholding to payroll draft

- **Permiso:** `hr:payroll:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `AddPayrollDeductionRequest`
- **Respuesta exitosa:** 200 — `PayrollPaymentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `payrollId` | path | string(uuid) | sí |

### POST /api/v1/hr/payrolls/{payrollId}/disburse

Mark payroll as disbursed and paid with bank reference

- **Permiso:** `hr:payroll:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `DisbursePayrollRequest`
- **Respuesta exitosa:** 200 — `PayrollPaymentResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `payrollId` | path | string(uuid) | sí |

### GET /api/v1/hr/work-shifts

List all work shifts

- **Permiso:** `hr:shifts:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `array<WorkShiftResource>`

### POST /api/v1/hr/work-shifts

Create a new work shift

- **Permiso:** `hr:shifts:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `CreateWorkShiftResource`
- **Respuesta exitosa:** 201 — `WorkShiftResource`

### GET /api/v1/hr/work-shifts/{shiftId}

Get work shift by ID

- **Permiso:** `hr:shifts:read`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 200 — `WorkShiftResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `shiftId` | path | string(uuid) | sí |

### PUT /api/v1/hr/work-shifts/{shiftId}

Update an existing work shift

- **Permiso:** `hr:shifts:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Body** (application/json): `UpdateWorkShiftResource`
- **Respuesta exitosa:** 200 — `WorkShiftResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `shiftId` | path | string(uuid) | sí |

### PATCH /api/v1/hr/work-shifts/{shiftId}/activate

Activate a work shift

- **Permiso:** `hr:shifts:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 204 — sin contenido

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `shiftId` | path | string(uuid) | sí |

### PATCH /api/v1/hr/work-shifts/{shiftId}/deactivate

Deactivate a work shift

- **Permiso:** `hr:shifts:manage`
- **Cabecera opcional:** `X-Tenant-Id` (si se omite se usa el taller del JWT)
- **Respuesta exitosa:** 204 — sin contenido

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `shiftId` | path | string(uuid) | sí |

## Modelos

### AttendanceResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `branchId` | string(uuid) |
| `membershipId` | string(uuid) |
| `shiftId` | string(uuid) |
| `clockIn` | string(date-time, UTC) |
| `clockOut` | string(date-time, UTC) |
| `status` | string |
| `latitude` | number |
| `longitude` | number |
| `distanceToBranchMeters` | number |
| `justificationReason` | string |
| `justifiedBy` | string(uuid) |
| `justifiedAt` | string(date-time, UTC) |
| `justificationStatus` | string |
| `justificationReviewedBy` | string(uuid) |
| `justificationReviewedAt` | string(date-time, UTC) |
| `workedMinutes` | integer |
| `regularMinutes` | integer |
| `overtimeMinutes` | integer |

### ClockInRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `branchId` | string(uuid) | sí |  |
| `latitude` | number(double) | sí | ≥ -90, ≤ 90 |
| `longitude` | number(double) | sí | ≥ -180, ≤ 180 |
| `shiftId` | string(uuid) | sí |  |
| `clockInTime` | string(date-time) | no |  |

### ClockOutRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `attendanceId` | string(uuid) | no |  |
| `clockOutTime` | string(date-time) | no |  |

### JustifyAttendanceRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `reason` | string | sí | max 500 |

### RegisterEmployeeProfileRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `baseSalary` | number | sí | ≥ 0 |
| `branchId` | string(uuid) | sí |  |
| `jobTitle` | string | sí | max 100 |
| `membershipId` | string(uuid) | sí |  |
| `compensationType` | string | no |  |
| `currency` | string | no |  |
| `shiftId` | string(uuid) | no |  |

### EmployeeProfileResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `branchId` | string(uuid) |
| `membershipId` | string(uuid) |
| `assignedShiftId` | string(uuid) |
| `baseSalary` | number |
| `currency` | string |
| `compensationType` | string |
| `jobTitle` | string |
| `employmentStatus` | string |

### UpdateSalaryRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `baseSalary` | number | sí | ≥ 0 |
| `compensationType` | string | no |  |
| `currency` | string | no |  |

### AssignShiftRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `shiftId` | string(uuid) | sí |  |

### UpdateEmploymentStatusRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `employmentStatus` | string | sí | min 1 |

### PayrollPaymentSummaryResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `membershipId` | string(uuid) |
| `periodStart` | string(date) |
| `periodEnd` | string(date) |
| `baseAmount` | number |
| `totalPaid` | number |
| `currency` | string |
| `status` | string |

### GeneratePayrollRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `membershipId` | string(uuid) | sí |  |
| `periodEnd` | string(date) | sí |  |
| `periodStart` | string(date) | sí |  |

### PayrollPaymentResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `membershipId` | string(uuid) |
| `periodStart` | string(date) |
| `periodEnd` | string(date) |
| `baseAmount` | number |
| `deductions` | number |
| `bonuses` | number |
| `totalPaid` | number |
| `currency` | string |
| `status` | string |
| `paidAt` | string(date-time, UTC) |
| `paymentReference` | string |
| `items` | array<PayrollItemResource> |

### AddPayrollBonusRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `amount` | number | sí | ≥ 0 |
| `bonusType` | string | sí | min 1 |
| `concept` | string | sí | max 150 |
| `date` | string(date) | sí |  |
| `currency` | string | no |  |

### AddPayrollDeductionRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `amount` | number | sí | ≥ 0 |
| `concept` | string | sí | max 150 |
| `date` | string(date) | sí |  |
| `deductionType` | string | sí | min 1 |
| `currency` | string | no |  |

### DisbursePayrollRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `paymentReference` | string | sí | min 1 |
| `paidAt` | string(date-time) | no |  |

### WorkShiftResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `name` | string |
| `startTime` | string(time) |
| `endTime` | string(time) |
| `gracePeriodMinutes` | integer |
| `isActive` | boolean |
| `workDays` | array<string> |
| `restDays` | array<string> |

### CreateWorkShiftResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `endTime` | string | sí |  |
| `name` | string | sí | max 100 |
| `startTime` | string | sí |  |
| `gracePeriodMinutes` | integer(int32) | no | ≥ 0, ≤ 60 |
| `workDays` | array<string enum: MONDAY\|TUESDAY\|WEDNESDAY\|THURSDAY\|FRIDAY\|SATURDAY\|SUNDAY> | no |  |

### UpdateWorkShiftResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `endTime` | string | sí |  |
| `name` | string | sí | max 100 |
| `startTime` | string | sí |  |
| `gracePeriodMinutes` | integer(int32) | no | ≥ 0, ≤ 60 |
| `workDays` | array<string enum: MONDAY\|TUESDAY\|WEDNESDAY\|THURSDAY\|FRIDAY\|SATURDAY\|SUNDAY> | no |  |

### PayrollItemResource

| Campo | Tipo |
| --- | --- |
| `category` | string |
| `concept` | string |
| `amount` | number |
| `type` | string |
| `date` | string(date) |

