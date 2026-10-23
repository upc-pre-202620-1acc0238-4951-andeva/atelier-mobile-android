# Billing — Suscripción SaaS (Stripe)

Planes, suscripción del taller y facturas de la plataforma. En la app móvil solo se consulta; el pago se hace en la web.

Convenciones comunes (autenticación, errores, cabeceras, fechas, dinero): ver [README.md](README.md).

## Índice

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | [`/api/v1/billing/invoices`](#get-apiv1billinginvoices) | `billing:invoices:read` | List workshop billing invoices |
| GET | [`/api/v1/billing/invoices/{id}`](#get-apiv1billinginvoicesid) | `billing:invoices:read` | Get invoice details by identifier |
| GET | [`/api/v1/billing/invoices/{id}/pdf`](#get-apiv1billinginvoicesidpdf) | `billing:invoices:read` | Redirect to official Stripe invoice PDF |
| GET | [`/api/v1/billing/plans`](#get-apiv1billingplans) | público | Get all active subscription plans |
| POST | [`/api/v1/billing/plans`](#post-apiv1billingplans) | `billing:plans:write` | Create a new commercial subscription plan |
| GET | [`/api/v1/billing/plans/{id}`](#get-apiv1billingplansid) | público | Get subscription plan by ID |
| PUT | [`/api/v1/billing/plans/{id}`](#put-apiv1billingplansid) | `billing:plans:write` | Update an existing commercial subscription plan |
| POST | [`/api/v1/billing/subscriptions/cancel`](#post-apiv1billingsubscriptionscancel) | `billing:subscriptions:manage_stripe` | Cancel subscription or schedule period-end termination |
| POST | [`/api/v1/billing/subscriptions/checkout-session`](#post-apiv1billingsubscriptionscheckout-session) | `billing:subscriptions:manage_stripe` | Create Stripe Checkout session |
| POST | [`/api/v1/billing/subscriptions/customer-portal`](#post-apiv1billingsubscriptionscustomer-portal) | `billing:subscriptions:manage_stripe` | Create Stripe Customer Portal session |
| GET | [`/api/v1/billing/subscriptions/me`](#get-apiv1billingsubscriptionsme) | `billing:subscriptions:read` | Get current workshop subscription |
| POST | [`/api/v1/billing/webhooks/stripe`](#post-apiv1billingwebhooksstripe) | público | Process Stripe webhook notification |

## Endpoints

### GET /api/v1/billing/invoices

List workshop billing invoices

- **Permiso:** `billing:invoices:read`
- **Respuesta exitosa:** 200 — `array<SaasInvoiceSummaryResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `status` | query | string | no |

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### GET /api/v1/billing/invoices/{id}

Get invoice details by identifier

- **Permiso:** `billing:invoices:read`
- **Respuesta exitosa:** 200 — `SaasInvoiceResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **400** Bad request / Invalid UUID; **401** Unauthorized; **403** Forbidden (Tenant isolation breach); **404** Invoice not found

### GET /api/v1/billing/invoices/{id}/pdf

Redirect to official Stripe invoice PDF

- **Permiso:** `billing:invoices:read`
- **Respuesta exitosa:** 302 — redirección 302 al PDF oficial de Stripe

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **302** Redirecting to official invoice PDF; **400** Bad request; **401** Unauthorized; **403** Forbidden (Tenant isolation breach); **404** Invoice or PDF not found

### GET /api/v1/billing/plans

Get all active subscription plans

- **Permiso:** público
- **Respuesta exitosa:** 200 — `array<SubscriptionPlanResource>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `billingCycle` | query | string | no |

**Errores documentados:** **500** Internal server error

### POST /api/v1/billing/plans

Create a new commercial subscription plan

- **Permiso:** `billing:plans:write`
- **Body** (application/json): `CreateSubscriptionPlanRequest`
- **Respuesta exitosa:** 201 — `SubscriptionPlanResource`

**Errores documentados:** **400** Invalid payload or validation error; **401** Unauthorized; **403** Forbidden (Requires platform authority billing:plans:write); **409** Duplicate plan name or Stripe price ID; **422** Invalid plan pricing or quota limits

### GET /api/v1/billing/plans/{id}

Get subscription plan by ID

- **Permiso:** público
- **Respuesta exitosa:** 200 — `SubscriptionPlanResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **404** Plan not found; **500** Internal server error

### PUT /api/v1/billing/plans/{id}

Update an existing commercial subscription plan

- **Permiso:** `billing:plans:write`
- **Body** (application/json): `UpdateSubscriptionPlanRequest`
- **Respuesta exitosa:** 200 — `SubscriptionPlanResource`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **400** Invalid payload or validation error; **401** Unauthorized; **403** Forbidden (Requires platform authority billing:plans:write); **404** Plan not found; **422** Invalid plan pricing or quota limits

### POST /api/v1/billing/subscriptions/cancel

Cancel subscription or schedule period-end termination

- **Permiso:** `billing:subscriptions:manage_stripe`
- **Body** (application/json): `CancelSubscriptionRequest`
- **Respuesta exitosa:** 204 — sin contenido

**Errores documentados:** **401** Unauthorized; **403** Forbidden (Requires billing:subscriptions:manage_stripe); **404** Subscription not found; **409** Subscription already canceled

### POST /api/v1/billing/subscriptions/checkout-session

Create Stripe Checkout session

- **Permiso:** `billing:subscriptions:manage_stripe`
- **Body** (application/json): `CreateCheckoutSessionRequest`
- **Respuesta exitosa:** 200 — `CheckoutSessionResponse`

**Errores documentados:** **400** Bad request; **401** Unauthorized; **404** Plan not found; **409** Duplicate subscription conflict

### POST /api/v1/billing/subscriptions/customer-portal

Create Stripe Customer Portal session

- **Permiso:** `billing:subscriptions:manage_stripe`
- **Body** (application/json): `CustomerPortalRequest`
- **Respuesta exitosa:** 200 — `CustomerPortalResponse`

**Errores documentados:** **400** Bad request; **401** Unauthorized; **404** Customer or subscription not found

### GET /api/v1/billing/subscriptions/me

Get current workshop subscription

- **Permiso:** `billing:subscriptions:read`
- **Respuesta exitosa:** 200 — `TenantSubscriptionResource`

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Subscription not found

### POST /api/v1/billing/webhooks/stripe

Process Stripe webhook notification

- **Permiso:** público
- **Body** (application/json): `string`
- **Respuesta exitosa:** 200 — uso exclusivo de Stripe (no llamar desde la app)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `Stripe-Signature` | header | string | no |

**Errores documentados:** **400** Bad request / malformed payload; **401** Unauthorized / Invalid webhook signature; **422** Unprocessable entity / handling error

## Modelos

### SaasInvoiceSummaryResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `stripeInvoiceId` | string |
| `amountPaid` | number |
| `currency` | string |
| `status` | string |
| `paidAt` | string(date-time, UTC) |

### SaasInvoiceResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `subscriptionId` | string(uuid) |
| `tenantId` | string(uuid) |
| `stripeInvoiceId` | string |
| `amountPaid` | number |
| `currency` | string |
| `status` | string |
| `invoicePdfUrl` | string |
| `hostedInvoiceUrl` | string |
| `paidAt` | string(date-time, UTC) |
| `createdAt` | string(date-time, UTC) |

### SubscriptionPlanResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `stripePriceId` | string |
| `name` | string |
| `tier` | string |
| `price` | number |
| `currency` | string |
| `billingCycle` | string |
| `quotaLimits` | TenantQuotaLimitsDto |
| `features` | array<PlanFeatureResource> |
| `isActive` | boolean |
| `createdAt` | string(date-time, UTC) |
| `updatedAt` | string(date-time, UTC) |

### CreateSubscriptionPlanRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `billingCycle` | string | sí | min 1, patrón `^(MONTHLY\|YEARLY)$` |
| `currency` | string | sí | min 3, max 3 |
| `name` | string | sí | min 3, max 100 |
| `price` | number | sí | ≥ 0 |
| `quotaLimits` | TenantQuotaLimitsDto | sí |  |
| `stripePriceId` | string | sí | min 1, patrón `^price_[a-zA-Z0-9_]+$` |
| `tier` | string | sí | min 1, patrón `^(GO\|PRO\|MAX\|ENTERPRISE)$` |
| `features` | array<PlanFeatureRequest> | no |  |

### UpdateSubscriptionPlanRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `billingCycle` | string | sí | min 1, patrón `^(MONTHLY\|YEARLY)$` |
| `name` | string | sí | min 3, max 100 |
| `price` | number | sí | ≥ 0 |
| `quotaLimits` | TenantQuotaLimitsDto | sí |  |
| `isActive` | boolean | no |  |

### CancelSubscriptionRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `cancelImmediately` | boolean | no |  |
| `cancellationReason` | string | no |  |

### CreateCheckoutSessionRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `cancelUrl` | string | sí | min 1 |
| `planId` | string(uuid) | sí |  |
| `successUrl` | string | sí | min 1 |

### CheckoutSessionResponse

| Campo | Tipo |
| --- | --- |
| `checkoutUrl` | string |
| `sessionId` | string |

### CustomerPortalRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `returnUrl` | string | sí | min 1 |

### CustomerPortalResponse

| Campo | Tipo |
| --- | --- |
| `portalUrl` | string |

### TenantSubscriptionResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `planId` | string(uuid) |
| `planName` | string |
| `tier` | string |
| `status` | string |
| `currentPeriodStart` | string(date-time, UTC) |
| `currentPeriodEnd` | string(date-time, UTC) |
| `cancelAtPeriodEnd` | boolean |
| `trialEndDate` | string(date-time, UTC) |
| `quotaLimits` | TenantQuotaLimitsDto |
| `isAccessGranted` | boolean |

### TenantQuotaLimitsDto

| Campo | Tipo |
| --- | --- |
| `maxBranches` | integer |
| `maxActiveStaff` | integer |
| `maxActiveObd2Devices` | integer |
| `maxPhotosPerWorkOrder` | integer |
| `maxMonthlyAiReports` | integer |
| `companyRegistrationAllowed` | boolean |
| `multiWarehouseAllowed` | boolean |
| `marketplaceListed` | boolean |
| `maxMonthlyWorkOrders` | integer |
| `iotTelemetryEnabled` | boolean |
| `aiDiagnosticsEnabled` | boolean |

### PlanFeatureResource

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `featureKey` | string |
| `name` | string |
| `description` | string |
| `isEnabled` | boolean |

### PlanFeatureRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `featureKey` | string | sí | min 1 |
| `name` | string | sí | min 1 |
| `description` | string | no |  |
| `isEnabled` | boolean | no |  |

