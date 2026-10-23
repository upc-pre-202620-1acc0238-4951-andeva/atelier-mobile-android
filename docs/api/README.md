# API REST de Atelier — Convenciones

Referencia del backend `atelier-platform` (Spring Boot 4, Java 25) tal como lo consume la app móvil de staff. Los archivos por contexto se generaron a partir del código fuente y del OpenAPI exportado, así que **el código manda** si algo difiere.

| Archivo | Bounded context | Endpoints |
| --- | --- | ---: |
| [iam.md](iam.md) | Identidad, sesión, taller, sucursales, roles, invitaciones | 30 |
| [crm.md](crm.md) | Clientes, vehículos, citas, afiliación | 24 |
| [operations.md](operations.md) | Órdenes de trabajo, tareas, bahías, servicios, cotización, rentabilidad | 50 |
| [inventory.md](inventory.md) | Repuestos, lotes FIFO, alertas, proveedores, compras | 27 |
| [hr.md](hr.md) | Asistencia, turnos, empleados, planilla | 30 |
| [invoicing.md](invoicing.md) | Comprobantes SUNAT, pagos, series, flujo de caja | 14 |
| [billing.md](billing.md) | Suscripción SaaS (Stripe) | 12 |
| [iot.md](iot.md) | OBD-II, telemetría, DTC, alertas, informes de salud con IA | 23 |

Swagger UI del servidor: `{BASE_URL}/swagger-ui.html` (OpenAPI en `{BASE_URL}/v3/api-docs`).

---

## 1. URL base y entornos

| Entorno | `BASE_URL` |
| --- | --- |
| Producción (Render) | La URL del servicio web en Render (`https://<servicio>.onrender.com`). Se configura en `local.properties` → `BuildConfig.API_BASE_URL` |
| Local (Docker) | `http://10.0.2.2:8080` desde el emulador Android (`localhost` del host) |

Todas las rutas empiezan con `/api/v1`. El plan gratuito de Render "duerme" el servicio: la **primera** petición puede tardar ~30–60 s. La app debe mostrar un estado de carga y usar timeouts de 60 s.

## 2. Autenticación y sesión

```text
sign-in / google-sign-in / invitations/accept
        │  { token (JWT), refreshToken, permissions[], activeTenant, membershipId }
        ▼
Authorization: Bearer <token>         ← en todas las demás peticiones
        │  401 UNAUTHORIZED
        ▼
POST /api/v1/auth/refresh { refreshToken }  → nuevo token + nuevo refreshToken (rotación)
        │  401 → sesión vencida: borrar sesión local e ir a Login
        ▼
POST /api/v1/auth/sign-out { refreshToken } → revoca el refresh del dispositivo
```

| Dato | Valor |
| --- | --- |
| Access token | JWT HS256, vigencia por defecto **24 h** (`JWT_EXPIRATION_MS`) |
| Claims del JWT | `sub` = userId, `email`, `tenantId`, `permissions` (array), `iat`, `exp` |
| Refresh token | Opaco, vigencia **30 días**, **se rota en cada refresh** (el anterior queda inválido; reusarlo devuelve 401) |
| Guardado en el dispositivo | Ambos tokens en DataStore cifrado (ver guía de arquitectura) |
| Push | Tras iniciar sesión: `PUT /api/v1/users/me/device-token { fcmToken }`. Al cerrar sesión: el mismo endpoint con `fcmToken: null` y luego `sign-out` |

Respuesta de sesión (`AuthenticatedUserResource`):

```json
{
  "userId": "7b1c…",
  "email": "mecanico@taller.pe",
  "fullName": "Andrés Vílchez",
  "token": "eyJhbGciOiJIUzI1NiJ9…",
  "tokenType": "Bearer",
  "activeTenant": { "id": "c9a2…", "name": "Taller Suárez", "taxId": "20123456789" },
  "permissions": ["ROLE_MECHANIC", "operations:tasks:read", "operations:tasks:track_time", "…"],
  "refreshToken": "q1Wm…",
  "membershipId": "5e0d…",
  "plan": { "planName": "Pro", "tier": "PRO", "status": "TRIALING", "accessGranted": true,
            "iotTelemetryEnabled": true, "aiDiagnosticsEnabled": false }
}
```

- `permissions` incluye el código del rol de fábrica (`ROLE_*`) y los permisos atómicos. **La app decide la UI con los permisos, no con el rol.**
- `membershipId` es el id del colaborador dentro del taller. Lo piden los endpoints "propios" de HR: `/hr/attendances/employee/{membershipId}/…` y `/hr/employees/membership/{membershipId}`.
- `plan` es el plan del taller, para **todos los roles**: con `iotTelemetryEnabled` y `aiDiagnosticsEnabled` la app oculta el diagnóstico OBD-II y el informe IA cuando el plan no los incluye; si `accessGranted` es `false`, avisar que la suscripción no está vigente. Puede venir `null` (sin taller o si no se pudo leer el plan): en ese caso se muestran los módulos y el backend responde 403 si no corresponden. Se actualiza en cada `refresh`.

### Registro y verificación de correo

| Paso | Endpoint | Nota |
| --- | --- | --- |
| Alta del taller (dueño) | `POST /auth/sign-up` | Crea taller + dueño en `PENDING_VERIFICATION` y prueba gratuita (`planId` opcional; Enterprise no). Envía código OTP de **6 dígitos** válido **15 min** |
| Verificar correo | `POST /auth/verify-email { email, token }` | `token` = OTP de 6 dígitos |
| Reenviar código | `POST /auth/resend-verification { email }` | |
| Iniciar sesión sin verificar | `POST /auth/sign-in` | **403 `EMAIL_NOT_VERIFIED`** → llevar a la pantalla de verificación |
| Olvidé mi contraseña | `POST /auth/forgot-password { email }` → `POST /auth/reset-password { token, newPassword }` | Token de restablecimiento válido 2 h |
| Colaborador invitado | `GET /invitations/validate?token=` → `POST /invitations/accept` | Invitación válida **48 h**. Si el correo ya tiene cuenta, `password` debe ser su contraseña actual. 409 si trabaja activo en otro taller |
| Google | `POST /auth/google-sign-in { idToken }` | `idToken` de Credential Manager con el **Web client ID** como `serverClientId` |

## 3. Taller activo (multi-tenant)

- El taller sale del claim `tenantId` del JWT; la app **no** necesita enviarlo.
- Algunos endpoints aceptan la cabecera opcional `X-Tenant-Id`; si se envía, debe coincidir con el taller del JWT (si no, 403).
- Acceso a recursos de otro taller → **403** (`FORBIDDEN` o `ERR_CROSS_TENANT_ACCESS`).

## 4. Cabeceras

| Cabecera | Uso |
| --- | --- |
| `Authorization: Bearer <token>` | Obligatoria salvo en `/auth/*`, `/invitations/validate`, `/invitations/accept` y `GET /billing/plans` |
| `Content-Type: application/json` | En todos los cuerpos |
| `Accept-Language: es` | El backend traduce los mensajes de error (`es`, `en`). Mandar siempre `es` |
| `X-Tenant-Id` | Opcional (ver arriba) |

## 5. Formato de error (único en todos los contextos)

```json
{
  "code": "VALIDATION_FAILED",
  "message": "La solicitud contiene datos inválidos",
  "details": ["latitude: must be less than or equal to 90"],
  "timestamp": "2026-10-20T15:04:05.123Z"
}
```

| HTTP | `code` típicos | Qué hace la app |
| --- | --- | --- |
| 400 | `VALIDATION_FAILED`, `BAD_REQUEST`, `MALFORMED_JSON_REQUEST`, `MISSING_PARAMETER`, `INVALID_PARAMETER`, `ERR_INVALID_*` | Mostrar `details` junto a cada campo (formato `campo: mensaje`) |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS`, `TOKEN_EXPIRED` | Intentar **un** refresh y repetir; si vuelve 401, cerrar sesión |
| 403 | `FORBIDDEN`, `ACCESS_DENIED`, `ERR_ACCESS_DENIED`, `EMAIL_NOT_VERIFIED`, `SUBSCRIPTION_REQUIRED`, `QUOTA_EXCEEDED`, `ERR_CROSS_TENANT_ACCESS` | Falta permiso, cuota del plan agotada o correo sin verificar: mostrar `message` |
| 404 | `NOT_FOUND`, `*_NOT_FOUND` | Recurso inexistente o de otro taller |
| 409 | `CONFLICT`, `*_CONFLICT`, `ERR_ACTIVE_INSTALLATION_CONFLICT`, `ERR_DEVICE_ALREADY_INSTALLED`, `ERR_SERIES_ALREADY_EXISTS` | Regla de unicidad: stock insuficiente al imputar, segunda orden abierta del vehículo, marcación de ingreso duplicada, bahía ocupada… |
| 422 | `UNPROCESSABLE_ENTITY`, `BUSINESS_RULE_VIOLATION`, `ERR_INSUFFICIENT_TELEMETRY`, `ERR_VOID_WINDOW_EXPIRED`, códigos `ERR_*` de dominio | Regla de negocio: fuera de geocerca (el `message` trae la distancia), telemetría insuficiente, pase de salida de orden no pagada, transición de estado inválida… |
| 500 | `INTERNAL_ERROR`, `INTERNAL_SERVER_ERROR` | Mensaje genérico y opción de reintentar |

Regla para la app: **decidir por el código HTTP + el endpoint**, y usar `code` solo donde el backend lo garantiza (`EMAIL_NOT_VERIFIED`, `ERR_INSUFFICIENT_TELEMETRY`, `QUOTA_EXCEEDED`). Muchos errores de los servicios de aplicación usan el código genérico del estado (`CONFLICT`, `UNPROCESSABLE_ENTITY`, `NOT_FOUND`…); por ejemplo, la marcación fuera de geocerca es `422 UNPROCESSABLE_ENTITY` y la marcación duplicada es `409 CONFLICT`. El texto para el usuario es siempre `message`.

## 6. Tipos de datos

| Tipo | Formato JSON | Ejemplo |
| --- | --- | --- |
| Identificadores | `string` UUID | `"5e0d2c1a-…"` |
| Instantes (`Instant`) | ISO-8601 en **UTC** | `"2026-10-20T13:00:00Z"` → mostrar en `America/Lima` |
| Fechas (`LocalDate`) | `yyyy-MM-dd` | `"2026-10-20"` |
| Horas (`LocalTime`) | `HH:mm:ss` | `"08:00:00"` (hora local de Lima) |
| Dinero | `number` + campo `currency` (`PEN`, `USD`) | `118.00`, `"PEN"` |
| Enums | `string` en MAYÚSCULAS | `"IN_PROGRESS"` |

- **Zona horaria del negocio:** `America/Lima` (UTC−5, sin horario de verano). Los cortes de "hoy", "este mes" y las cuotas mensuales se calculan en Lima.
- **IGV 18 %:** en la orden de trabajo los precios de tareas y repuestos son **sin IGV**; el backend calcula `subtotal`, `taxAmount` (IGV) y `totalAmount`. En los comprobantes (`invoicing`) el precio unitario de cada línea **incluye** IGV.
- Paginación: solo `GET /hr/payrolls` devuelve una página de Spring (`content`, `totalElements`, `totalPages`, `number`, `size`; parámetros `page`, `size`, `sort`). El resto devuelve arrays completos.

## 7. Archivos y fotos

La app **sube las fotos directamente a Supabase Storage** y luego envía al backend solo la URL.

1. Subir a `https://<proyecto>.supabase.co/storage/v1/object/<bucket>/<ruta>` (bucket `atelier`).
2. Llamar al endpoint con la URL pública: `POST /work-orders/{id}/intake-images`, `POST /tasks/{id}/evidence-images`, `POST /work-orders/{id}/proposals`.
3. El backend **rechaza** (400) cualquier URL que no sea `https`, del host Supabase configurado y del bucket `atelier`.

Ruta sugerida: `{tenantId}/work-orders/{workOrderId}/{intake|tasks/{taskId}|proposals}/{uuid}.jpg`.

PDFs (`application/pdf`, con `Content-Disposition`): proforma `GET /work-orders/{id}/quotation/pdf`, pase de salida `GET /work-orders/{id}/exit-pass/pdf`, informe de salud `GET /iot/health-reports/{id}/pdf`, flota `GET /iot/health-reports/fleet/pdf?customerId=`, flujo de caja `GET /invoicing/financial-reports/cash-flow/pdf`. La app los descarga con el mismo `Authorization` y los abre con un `Intent` de visor PDF.

## 8. Enumeraciones

| Contexto | Enum | Valores |
| --- | --- | --- |
| billing | BillingCycle | MONTHLY, YEARLY |
| billing | PlanTier | GO, PRO, MAX, ENTERPRISE |
| billing | SubscriptionStatus | TRIALING, ACTIVE, PAST_DUE, CANCELED, UNPAID, INCOMPLETE |
| crm | AppointmentStatus | PENDING, CONFIRMED, ARRIVED, CANCELED |
| crm | CustomerType | INDIVIDUAL, COMPANY |
| crm | EngineType | GASOLINE, DIESEL, ELECTRIC, HYBRID |
| crm | FleetRole | FLEET_ADMIN, FLEET_OPERATOR |
| hr | AttendanceStatus | ON_TIME, LATE, EXCUSED, ABSENT |
| hr | JustificationStatus | PENDING, APPROVED, REJECTED |
| hr | EmploymentStatus | ACTIVE, ON_LEAVE, TERMINATED |
| hr | PayrollStatus | DRAFT, APPROVED, PAID, CANCELLED |
| iam | InvitationStatus | PENDING, ACCEPTED, EXPIRED, REVOKED |
| iam | UserStatus | PENDING_VERIFICATION, ACTIVE, SUSPENDED |
| iam | SalaryType | FIXED, HOURLY |
| inventory | ItemCategory | LUBRICANTS, BRAKES, SUSPENSION, ENGINE, ELECTRICAL, TIRES, FILTERS, BODYWORK, ACCESSORIES |
| inventory | PurchaseOrderStatus | DRAFT, ISSUED, RECEIVED, CANCELED |
| invoicing | VoucherType | FACTURA, BOLETA, NOTA_CREDITO |
| invoicing | VoucherStatus | DRAFT, ISSUED, ACCEPTED_SUNAT, REJECTED_SUNAT, VOIDED |
| invoicing | PaymentMethod | CASH, CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, DIGITAL_WALLET_YAPE, DIGITAL_WALLET_PLIN |
| invoicing | DocumentType | DNI, CE, RUC, PASAPORTE |
| invoicing | `paymentStatus` del comprobante | PENDING, PARTIAL, PAID |
| hr | `workDays` / `restDays` del turno | MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY |
| iot | DeviceStatus | ACTIVE, INACTIVE, LOST, BROKEN |
| iot | ConnectionType | BLUETOOTH_BLE, SIM_CELLULAR, WIFI |
| iot | FaultSeverity | LOW, MEDIUM, CRITICAL |
| iot | AlertSeverity | LOW, MEDIUM, HIGH, CRITICAL |
| iot | AlertStatus | DISPATCHED, ACKNOWLEDGED, RESOLVED, DISMISSED |
| iot | AlertType | ENGINE_OVERHEATING_RISK, BATTERY_FAILURE_RISK, CATALYTIC_SYSTEM_DEGRADATION, CYLINDER_MISFIRE_HAZARD, BRAKE_WEAR, OIL_LIFE_DEPLETION, EMISSIONS_FAILURE |
| iot | RiskLevel | LOW, MODERATE, HIGH, CRITICAL |
| operations | WorkOrderStatus | DRAFT, IN_PROGRESS, COMPLETED, PAID, CANCELED |
| operations | WorkOrderTaskStatus | PENDING, ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED |
| operations | HoldReason | BREAK, END_OF_SHIFT (pausas del mecánico), DEFECTIVE_PART (suspensión aprobada), WAITING_PARTS / WAITING_FOR_PARTS (heredados) |
| operations | ProposalStatus | PENDING_REVIEW, APPROVED, REJECTED |
| operations | ProposalSeverity | LOW, MEDIUM, HIGH, CRITICAL |
| operations | BayStatus | AVAILABLE, OCCUPIED, MAINTENANCE |
| operations | BayType | LIFT, PAINT_BOOTH, WASHING, ALIGNMENT |
| operations | EvidenceType | INITIAL_INSPECTION, DEFECT, IN_PROGRESS, COMPLETED |
| shared | Currency | PEN, USD |
| shared | MeasurementUnit | UNIT, LITER, GALLON, KILOGRAM, METER, HOUR |
| shared | TaxIdType | RUC, DNI, CE, PASSPORT |

## 9. Cómo están escritos los archivos por contexto

Cada endpoint indica:

- **Permiso**: el permiso atómico requerido (`hasAuthority`). "o" = basta uno de ellos.
- **Body**: el modelo de request; sus campos, obligatoriedad y restricciones están en la sección *Modelos* del mismo archivo.
- **Respuesta exitosa**: código HTTP y modelo. `array<X>` = lista JSON.
- **Parámetros**: path y query.
- **Errores documentados**: los que declara el controlador (además de los generales de la sección 5).
