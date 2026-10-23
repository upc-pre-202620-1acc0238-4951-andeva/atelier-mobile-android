# IAM — Identidad, talleres, sucursales, roles e invitaciones

Autenticación, sesión, talleres (tenants), sucursales, membresías, roles, permisos e invitaciones.

Convenciones comunes (autenticación, errores, cabeceras, fechas, dinero): ver [README.md](README.md).

## Índice

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| POST | [`/api/v1/auth/forgot-password`](#post-apiv1authforgot-password) | público | Request password reset token via email |
| POST | [`/api/v1/auth/google-sign-in`](#post-apiv1authgoogle-sign-in) | público | Authenticate user using Google ID token |
| POST | [`/api/v1/auth/refresh`](#post-apiv1authrefresh) | público | Renew the session with the refresh token (the token is rotated) |
| POST | [`/api/v1/auth/resend-verification`](#post-apiv1authresend-verification) | público | Re-send the email verification code |
| POST | [`/api/v1/auth/reset-password`](#post-apiv1authreset-password) | público | Confirm password reset and set new password |
| POST | [`/api/v1/auth/sign-in`](#post-apiv1authsign-in) | público | Authenticate user with local email and password credentials |
| POST | [`/api/v1/auth/sign-out`](#post-apiv1authsign-out) | público | Sign out the device, revoking its refresh token |
| POST | [`/api/v1/auth/sign-up`](#post-apiv1authsign-up) | público | Register and provision a new workshop Tenant with initial administrator |
| POST | [`/api/v1/auth/verify-email`](#post-apiv1authverify-email) | público | Confirm email verification using one-time token |
| GET | [`/api/v1/branches`](#get-apiv1branches) | `iam:branches:read` | List all physical workshop branches for active tenant |
| POST | [`/api/v1/branches`](#post-apiv1branches) | `iam:branches:manage` | Create a new physical branch with GPS geofence |
| GET | [`/api/v1/branches/{id}`](#get-apiv1branchesid) | `iam:branches:read` | Get branch details by ID |
| PUT | [`/api/v1/branches/{id}`](#put-apiv1branchesid) | `iam:branches:manage` | Update branch GPS coordinates and attendance geofence radius |
| POST | [`/api/v1/invitations/accept`](#post-apiv1invitationsaccept) | público | Accept invitation and register collaborator account credentials |
| POST | [`/api/v1/invitations/tenant/{tenantId}`](#post-apiv1invitationstenanttenantid) | `iam:members:invite` | Invite staff member by email to workshop tenant |
| GET | [`/api/v1/invitations/validate`](#get-apiv1invitationsvalidate) | público | Validate cryptographic invitation token |
| GET | [`/api/v1/memberships`](#get-apiv1memberships) | `iam:members:read` | List all staff memberships for a tenant |
| DELETE | [`/api/v1/memberships/{id}`](#delete-apiv1membershipsid) | `iam:members:deactivate` | Deactivate staff membership |
| GET | [`/api/v1/memberships/{id}`](#get-apiv1membershipsid) | `iam:members:read` | Get staff membership details by ID |
| PUT | [`/api/v1/memberships/{id}/compensation`](#put-apiv1membershipsidcompensation) | `iam:members:compensate` | Update salary compensation for a staff membership |
| PUT | [`/api/v1/memberships/{id}/roles`](#put-apiv1membershipsidroles) | `iam:members:manage_roles` | Assign security roles to a staff membership |
| GET | [`/api/v1/permissions`](#get-apiv1permissions) | `iam:permissions:read` | List all atomic platform permissions |
| GET | [`/api/v1/tenants/current`](#get-apiv1tenantscurrent) | `iam:tenants:read` | Get corporate profile of the active workshop tenant in session |
| PUT | [`/api/v1/tenants/current`](#put-apiv1tenantscurrent) | `iam:tenants:update` | Update corporate profile and business names of active workshop tenant |
| GET | [`/api/v1/tenants/{tenantId}/roles`](#get-apiv1tenantstenantidroles) | `iam:roles:read` | List all security roles in a workshop tenant |
| POST | [`/api/v1/tenants/{tenantId}/roles`](#post-apiv1tenantstenantidroles) | `iam:roles:create` | Create custom RBAC role for workshop tenant |
| DELETE | [`/api/v1/tenants/{tenantId}/roles/{roleId}`](#delete-apiv1tenantstenantidrolesroleid) | `iam:roles:manage` | Delete an unused custom role |
| PUT | [`/api/v1/tenants/{tenantId}/roles/{roleId}/permissions`](#put-apiv1tenantstenantidrolesroleidpermissions) | `iam:roles:manage` | Update granted permissions for a tenant role |
| POST | [`/api/v1/tenants/{tenantId}/roles/{roleId}/reset-defaults`](#post-apiv1tenantstenantidrolesroleidreset-defaults) | `iam:roles:manage` | Reset system role to factory default permissions |
| PUT | [`/api/v1/users/me/device-token`](#put-apiv1usersmedevice-token) | autenticado (sin permiso específico) | Register or clear the push notification (FCM) token of the current device |

## Endpoints

### POST /api/v1/auth/forgot-password

Request password reset token via email

- **Permiso:** público
- **Body** (application/json): `ForgotPasswordResource`
- **Respuesta exitosa:** 200 — `MessageResponseResource`

**Errores documentados:** **400** Invalid email format

### POST /api/v1/auth/google-sign-in

Authenticate user using Google ID token

- **Permiso:** público
- **Body** (application/json): `GoogleSignInResource`
- **Respuesta exitosa:** 200 — `AuthenticatedUserResource`

**Errores documentados:** **400** Missing or malformed Google ID token; **401** Invalid or expired Google ID token

### POST /api/v1/auth/refresh

Renew the session with the refresh token (the token is rotated)

- **Permiso:** público
- **Body** (application/json): `RefreshTokenResource`
- **Respuesta exitosa:** 200 — `AuthenticatedUserResource`

**Errores documentados:** **401** Refresh token invalid, expired or revoked

### POST /api/v1/auth/resend-verification

Re-send the email verification code

- **Permiso:** público
- **Body** (application/json): `ResendVerificationResource`
- **Respuesta exitosa:** 200 — `MessageResponseResource`

**Errores documentados:** **400** Invalid email format

### POST /api/v1/auth/reset-password

Confirm password reset and set new password

- **Permiso:** público
- **Body** (application/json): `ResetPasswordResource`
- **Respuesta exitosa:** 200 — `MessageResponseResource`

**Errores documentados:** **400** Invalid, expired, or already used reset token

### POST /api/v1/auth/sign-in

Authenticate user with local email and password credentials

- **Permiso:** público
- **Body** (application/json): `SignInResource`
- **Respuesta exitosa:** 200 — `AuthenticatedUserResource`

**Errores documentados:** **400** Malformed credentials or missing parameters; **401** Invalid credentials or unverified/suspended account

### POST /api/v1/auth/sign-out

Sign out the device, revoking its refresh token

- **Permiso:** público
- **Body** (application/json): `RefreshTokenResource`
- **Respuesta exitosa:** 200 — `MessageResponseResource`

### POST /api/v1/auth/sign-up

Register and provision a new workshop Tenant with initial administrator

- **Permiso:** público
- **Body** (application/json): `CreateTenantResource`
- **Respuesta exitosa:** 201 — `TenantResource`

**Errores documentados:** **400** Validation constraint violations in submitted payload; **409** Tax ID (RUC) or admin email already registered in system

### POST /api/v1/auth/verify-email

Confirm email verification using one-time token

- **Permiso:** público
- **Body** (application/json): `VerifyEmailResource`
- **Respuesta exitosa:** 200 — `MessageResponseResource`

**Errores documentados:** **400** Invalid, expired, or previously consumed token

### GET /api/v1/branches

List all physical workshop branches for active tenant

- **Permiso:** `iam:branches:read`
- **Respuesta exitosa:** 200 — `array<BranchResource>`

**Errores documentados:** **401** Authentication required; **403** Missing authority iam:branches:read

### POST /api/v1/branches

Create a new physical branch with GPS geofence

- **Permiso:** `iam:branches:manage`
- **Body** (application/json): `CreateBranchResource`
- **Respuesta exitosa:** 201 — `BranchResource`

**Errores documentados:** **400** Geodesic coordinate or radius validation failure; **401** Authentication required; **403** Missing authority iam:branches:manage or subscription quota exceeded

### GET /api/v1/branches/{id}

Get branch details by ID

- **Permiso:** `iam:branches:read`
- **Respuesta exitosa:** 200 — `BranchResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Authentication required; **403** Missing authority iam:branches:read; **404** Branch not found or belongs to another tenant

### PUT /api/v1/branches/{id}

Update branch GPS coordinates and attendance geofence radius

- **Permiso:** `iam:branches:manage`
- **Body** (application/json): `UpdateBranchLocationResource`
- **Respuesta exitosa:** 200 — `BranchResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **400** Validation constraint failure on coordinates or radius; **401** Authentication required; **403** Missing authority iam:branches:manage; **404** Branch not found or unauthorized

### POST /api/v1/invitations/accept

Accept invitation and register collaborator account credentials

- **Permiso:** público
- **Body** (application/json): `AcceptInvitationResource`
- **Respuesta exitosa:** 201 — `AuthenticatedUserResource`

**Errores documentados:** **400** Validation constraint failure on password or names; **404** Invitation token not found; **409** Invitation already accepted previously; **410** Invitation token expired

### POST /api/v1/invitations/tenant/{tenantId}

Invite staff member by email to workshop tenant

- **Permiso:** `iam:members:invite`
- **Body** (application/json): `InviteStaffResource`
- **Respuesta exitosa:** 201 — `InvitationResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `tenantId` | path | string(uuid) | sí |

**Errores documentados:** **400** Invalid email format or missing role; **401** Authentication required; **403** Missing authority iam:members:invite or subscription quota exceeded; **409** Invitee already has active membership or pending invitation

### GET /api/v1/invitations/validate

Validate cryptographic invitation token

- **Permiso:** público
- **Respuesta exitosa:** 200 — `InvitationValidationResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `token` | query | string | sí |

**Errores documentados:** **400** Missing or empty token parameter; **404** Invitation token does not exist or was revoked; **410** Invitation token has expired past its 48-hour validity

### GET /api/v1/memberships

List all staff memberships for a tenant

- **Permiso:** `iam:members:read`
- **Respuesta exitosa:** 200 — `array<MembershipResource>`

**Errores documentados:** **401** Authentication required; **403** Missing authority iam:members:read

### DELETE /api/v1/memberships/{id}

Deactivate staff membership

- **Permiso:** `iam:members:deactivate`
- **Respuesta exitosa:** 204 — sin contenido

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Authentication required; **403** Missing authority iam:members:manage_roles or cross-tenant access; **404** Membership not found; **409** Cannot deactivate last workshop owner

### GET /api/v1/memberships/{id}

Get staff membership details by ID

- **Permiso:** `iam:members:read`
- **Respuesta exitosa:** 200 — `MembershipResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Authentication required; **403** Missing authority iam:members:read or cross-tenant access; **404** Membership not found or belongs to another tenant

### PUT /api/v1/memberships/{id}/compensation

Update salary compensation for a staff membership

- **Permiso:** `iam:members:compensate`
- **Body** (application/json): `UpdateCompensationResource`
- **Respuesta exitosa:** 200 — `MembershipResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **400** Invalid salary type or negative amount; **401** Authentication required; **403** Missing authority iam:members:compensate or cross-tenant access; **404** Membership not found

### PUT /api/v1/memberships/{id}/roles

Assign security roles to a staff membership

- **Permiso:** `iam:members:manage_roles`
- **Body** (application/json): `AssignRolesResource`
- **Respuesta exitosa:** 200 — `MembershipResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **400** Empty role ID list; **401** Authentication required; **403** Missing authority iam:members:manage_roles or cross-tenant access; **404** Membership or Role not found

### GET /api/v1/permissions

List all atomic platform permissions

- **Permiso:** `iam:permissions:read`
- **Respuesta exitosa:** 200 — `array<PermissionResource>`

**Errores documentados:** **401** Authentication required; **403** Missing authority iam:permissions:read

### GET /api/v1/tenants/current

Get corporate profile of the active workshop tenant in session

- **Permiso:** `iam:tenants:read`
- **Respuesta exitosa:** 200 — `TenantResource`

**Errores documentados:** **401** Authentication required or invalid JWT; **403** Missing authority iam:tenants:read; **404** Tenant not found

### PUT /api/v1/tenants/current

Update corporate profile and business names of active workshop tenant

- **Permiso:** `iam:tenants:update`
- **Body** (application/json): `UpdateTenantProfileResource`
- **Respuesta exitosa:** 200 — `TenantResource`

**Errores documentados:** **400** Validation constraint failure on names; **401** Authentication required; **403** Missing authority iam:tenants:update

### GET /api/v1/tenants/{tenantId}/roles

List all security roles in a workshop tenant

- **Permiso:** `iam:roles:read`
- **Respuesta exitosa:** 200 — `array<RoleResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `tenantId` | path | string(uuid) | sí |

**Errores documentados:** **401** Authentication required; **403** Missing authority iam:roles:read

### POST /api/v1/tenants/{tenantId}/roles

Create custom RBAC role for workshop tenant

- **Permiso:** `iam:roles:create`
- **Body** (application/json): `CreateRoleResource`
- **Respuesta exitosa:** 201 — `RoleResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `tenantId` | path | string(uuid) | sí |

**Errores documentados:** **400** Validation constraint failure; **401** Authentication required; **403** Missing authority iam:roles:create; **409** Role name already exists in tenant

### DELETE /api/v1/tenants/{tenantId}/roles/{roleId}

Delete an unused custom role

- **Permiso:** `iam:roles:manage`
- **Respuesta exitosa:** 204 — sin contenido

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `tenantId` | path | string(uuid) | sí |
| `roleId` | path | string(uuid) | sí |

**Errores documentados:** **401** Authentication required; **403** Missing authority iam:roles:manage; **404** Role not found; **409** Cannot delete system factory role or role in active use

### PUT /api/v1/tenants/{tenantId}/roles/{roleId}/permissions

Update granted permissions for a tenant role

- **Permiso:** `iam:roles:manage`
- **Body** (application/json): `UpdateRolePermissionsResource`
- **Respuesta exitosa:** 200 — `RoleResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `tenantId` | path | string(uuid) | sí |
| `roleId` | path | string(uuid) | sí |

**Errores documentados:** **400** Empty permission list; **401** Authentication required; **403** Missing authority iam:roles:manage; **404** Role or permission not found

### POST /api/v1/tenants/{tenantId}/roles/{roleId}/reset-defaults

Reset system role to factory default permissions

- **Permiso:** `iam:roles:manage`
- **Respuesta exitosa:** 200 — `RoleResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `tenantId` | path | string(uuid) | sí |
| `roleId` | path | string(uuid) | sí |

**Errores documentados:** **400** Role is not a factory system role; **401** Authentication required; **403** Missing authority iam:roles:manage; **404** Role not found

### PUT /api/v1/users/me/device-token

Register or clear the push notification (FCM) token of the current device

- **Permiso:** autenticado (sin permiso específico)
- **Body** (application/json): `RegisterDeviceTokenResource`
- **Respuesta exitosa:** 204 — sin contenido

**Errores documentados:** **401** Authentication required or invalid JWT

## Modelos

### ForgotPasswordResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `email` | string(email) | sí | min 1 |

### MessageResponseResource

| Campo | Tipo |
| --- | --- |
| `message` | string |
| `timestamp` | string(date-time, UTC) |

### GoogleSignInResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `idToken` | string | sí | min 1 |

### AuthenticatedUserResource

| Campo | Tipo |
| --- | --- |
| `userId` | string(uuid) |
| `email` | string |
| `fullName` | string |
| `token` | string |
| `tokenType` | string |
| `activeTenant` | TenantSummaryResource |
| `permissions` | array<string> |
| `refreshToken` | string |
| `membershipId` | string(uuid) |
| `plan` | SessionPlanResource |

### RefreshTokenResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `refreshToken` | string | sí | min 1 |

### ResendVerificationResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `email` | string(email) | sí | min 1 |

### ResetPasswordResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `newPassword` | string | sí | min 8, max 64 |
| `token` | string | sí | min 1 |

### SignInResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `email` | string(email) | sí | min 1 |
| `password` | string | sí | min 1 |

### CreateTenantResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `adminEmail` | string(email) | sí | max 150 |
| `adminFirstName` | string | sí | max 100 |
| `adminLastName` | string | sí | max 100 |
| `adminPassword` | string | sí | min 8, max 64 |
| `adminPhone` | string | sí | min 1, patrón `^\+?[0-9]{9,15}$` |
| `legalName` | string | sí | max 150 |
| `name` | string | sí | max 100 |
| `taxId` | string | sí | min 1, patrón `^(10\|15\|17\|20)\d{9}$` |
| `planId` | string(uuid) | no |  |

### TenantResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `name` | string |
| `legalName` | string |
| `taxId` | string |
| `status` | string |
| `stripeCustomerId` | string |
| `createdAt` | string(date-time, UTC) |

### VerifyEmailResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `token` | string | sí | min 1 |
| `email` | string(email) | no |  |

### BranchResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `name` | string |
| `sunatCode` | string |
| `latitude` | number |
| `longitude` | number |
| `geofenceRadiusMeters` | integer |
| `isActive` | boolean |

### CreateBranchResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `latitude` | number(double) | sí | ≥ -90, ≤ 90 |
| `longitude` | number(double) | sí | ≥ -180, ≤ 180 |
| `name` | string | sí | max 100 |
| `geofenceRadiusMeters` | integer(int32) | no | ≥ 10, ≤ 1000 |
| `sunatCode` | string | no | patrón `^\d{4}$` |

### UpdateBranchLocationResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `latitude` | number(double) | sí | ≥ -90, ≤ 90 |
| `longitude` | number(double) | sí | ≥ -180, ≤ 180 |
| `geofenceRadiusMeters` | integer(int32) | no | ≥ 10, ≤ 1000 |

### AcceptInvitationResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `firstName` | string | sí | max 100 |
| `lastName` | string | sí | max 100 |
| `password` | string | sí | min 8, max 64 |
| `token` | string | sí | min 1 |
| `phone` | string | no | patrón `^\+?[0-9]{9,15}$` |

### InviteStaffResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `email` | string(email) | sí | max 150 |
| `roleId` | string(uuid) | sí |  |

### InvitationResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `email` | string |
| `status` | string |
| `expiresAt` | string(date-time, UTC) |

### InvitationValidationResource

| Campo | Tipo |
| --- | --- |
| `valid` | boolean |
| `email` | string |
| `tenantName` | string |
| `roleName` | string |

### MembershipResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `userId` | string(uuid) |
| `employeeName` | string |
| `email` | string |
| `status` | string |
| `salaryType` | string |
| `baseSalary` | number |
| `currency` | string |
| `roles` | array<RoleResource> |

### UpdateCompensationResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `baseSalary` | number | sí | ≥ 0 |
| `currency` | string | sí | min 1, patrón `^(PEN\|USD)$` |
| `salaryType` | string | sí | min 1, patrón `^(FIXED\|HOURLY)$` |

### AssignRolesResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `roleIds` | array<string(uuid)> | sí |  |

### PermissionResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `name` | string |
| `description` | string |
| `category` | string |

### UpdateTenantProfileResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `legalName` | string | sí | max 150 |
| `name` | string | sí | max 100 |

### RoleResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `code` | string |
| `name` | string |
| `description` | string |
| `isSystemRole` | boolean |
| `permissions` | array<RolePermissionResource> |

### CreateRoleResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `name` | string | sí | max 100 |
| `permissionIds` | array<string(uuid)> | sí |  |
| `description` | string | no | max 255 |

### UpdateRolePermissionsResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `permissionIds` | array<string(uuid)> | sí |  |

### RegisterDeviceTokenResource (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `fcmToken` | string | no | max 4096 |

### TenantSummaryResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `name` | string |
| `taxId` | string |

### SessionPlanResource

| Campo | Tipo |
| --- | --- |
| `planName` | string |
| `tier` | string |
| `status` | string |
| `accessGranted` | boolean |
| `iotTelemetryEnabled` | boolean |
| `aiDiagnosticsEnabled` | boolean |

### RolePermissionResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `name` | string |
| `category` | string |

