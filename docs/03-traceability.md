# Matriz de trazabilidad — Historia → escenario → endpoint → permiso

Cómo se cumple cada criterio de aceptación de [01-requirements/user-stories.md](01-requirements/user-stories.md) con el backend actual (rama `hotfix/1.0.3-unified-error-body`). Las rutas omiten el prefijo `/api/v1`.

| Estado | Significado |
| --- | --- |
| ✅ | El backend lo cubre; la app solo llama al endpoint indicado |
| 📱 | Lo resuelve la app (sensor, Bluetooth, cola offline, UI); el endpoint indicado es el punto de sincronización |
| ⛔ | Fuera del alcance de la app de staff |

Las notificaciones push (FCM) llevan `data.type`; sus tipos están en [04-mobile-integration.md](04-mobile-integration.md#2-notificaciones-push-fcm).

---

## EP01 — Autenticación y control de acceso móvil

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US01 E1 Registro de colaborador invitado | `GET /invitations/validate?token=` → `POST /invitations/accept` (devuelve sesión completa) | público | ✅ |
| US01 E2 Membresía activa en otro taller | `POST /invitations/accept` → **409** (también "ya aceptada") | público | ✅ |
| US01 E3 Invitación vencida (48 h) o revocada | `GET /invitations/validate` / `POST /invitations/accept` → **410** vencida, **404** inexistente o revocada, **400** token vacío ([api/iam.md](api/iam.md)) | público | ✅ |
| US02 E1 Validación OTP y activación | `POST /auth/verify-email { email, token }` (OTP de 6 dígitos, 15 min) | público | ✅ |
| US02 E2 OTP incorrecto o caducado | `POST /auth/verify-email` → 400; `POST /auth/resend-verification` | público | ✅ |
| US03 E1 Login y contexto de sede | `POST /auth/sign-in` (permisos, taller, `membershipId`) → `GET /hr/employees/membership/{membershipId}` (sede y turno; 404 si el usuario no tiene perfil laboral → entra sin sede) → `GET /branches` (lo carga `attendance`, Paso 4, con la caché `branches`) | público · `hr:attendance:read_own` · `iam:branches:read` | ✅ |
| US03 E2 Credenciales inválidas | `POST /auth/sign-in` → **401**; correo sin verificar → **403 `EMAIL_NOT_VERIFIED`** (iam.md también lo lista bajo 401: la app decide por el `code`) | público | ✅ |
| US04 E1 Cierre de sesión | `PUT /users/me/device-token { fcmToken: null }` → `POST /auth/sign-out { refreshToken }` → borrar DataStore | autenticado | ✅ |
| US04 E2 Expiración del token | 401 → `POST /auth/refresh` (rotación); si falla → Login | público | ✅ |
| US05 E1 Solicitud de recuperación | `POST /auth/forgot-password { email }` (correo con token, 2 h) | público | ✅ |
| US05 E2 Token expirado o adulterado | `POST /auth/reset-password` → 400 | público | ✅ |

## EP02 — Control de asistencia satelital y turnos

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US06 E1 Ingreso puntual dentro de geocerca | `POST /hr/attendances/clock-in { branchId, shiftId, latitude, longitude }` → `status: ON_TIME` | `hr:attendance:clock_in` | ✅ |
| US06 E2 Ingreso con tardanza | Mismo endpoint → `status: LATE` (fuera de la tolerancia del turno) | `hr:attendance:clock_in` | ✅ |
| US06 E3 Marcación duplicada | Mismo endpoint → **409** | `hr:attendance:clock_in` | ✅ |
| US08 E1 Salida ordinaria | `POST /hr/attendances/clock-out` → `workedMinutes`, `regularMinutes`, `overtimeMinutes` en la asistencia | `hr:attendance:clock_out` | ✅ |
| US08 E2 Sobretiempo | Salida más de 30 min después del fin del turno → `overtimeMinutes` y push `OVERTIME_RECORDED` a quienes tienen `hr:payroll:manage` | `hr:attendance:clock_out` | ✅ |
| US08 E3 Salida sin ingreso | `POST /hr/attendances/clock-out` → **404** | `hr:attendance:clock_out` | ✅ |
| US09 E1 Cronograma semanal y descansos | `GET /hr/employees/membership/{membershipId}` (`assignedShiftId`) + `GET /hr/work-shifts/{shiftId}` (`startTime`, `endTime`, `gracePeriodMinutes`, `workDays`, `restDays`) | `hr:attendance:read_own`, `hr:shifts:read` | ✅ |
| US09 E2 Recordatorio antes del turno | Push `SHIFT_REMINDER` 15 min antes del inicio | — | ✅ |
| US10 E1 Registrar descargo | `POST /hr/attendances/{attendanceId}/justification { reason }` | `hr:justifications:create` | ✅ |
| US10 E2 Dictamen de la administración | `POST …/justification/approve` · `…/justification/reject` → push `JUSTIFICATION_REVIEWED` al colaborador | `hr:justifications:approve` | ✅ |

## EP03 — Recepción pericial e inspección en patio

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US11 E1 Unidad recurrente | `GET /vehicles/by-plate/{plate}` → `GET /customers/{currentOwnerId}`; citas: `GET /appointments`, `POST /appointments/{id}/arrive` (crea la orden en borrador) | `crm:vehicles:read`, `crm:customers:read`, `crm:appointments:update` | ✅ |
| US11 E2 Primer ingreso | `POST /customers/individuals` o `/customers/companies` → `POST /vehicles` | `crm:customers:create`, `crm:vehicles:create` | ✅ |
| US12 E1 Orden con kilometraje y diagnóstico | `POST /work-orders { vehicleId, customerId, branchId, mileageIn, diagnosticSummary }` (409 si el vehículo ya tiene una orden abierta) | `operations:work_orders:create` | ✅ |
| US12 E2 Tareas y asignación a mecánico | `POST /work-orders/{id}/tasks { serviceId, mechanicId, description, price? }` → push `TASK_ASSIGNED` | `operations:work_orders:create` | ✅ |
| US13 E1 Fotos de recepción | Subida a Supabase → `POST /work-orders/{id}/intake-images { imageUrl, description }` | `operations:inspections:upload_photos` | ✅ |
| US13 E2 Consultar expediente | `GET /work-orders/{id}` → `intakeImages[]` | `operations:work_orders:read` | ✅ |
| US14 E1 Propuesta con evidencia | `POST /work-orders/{id}/proposals { mechanicId, description, severity: LOW·MEDIUM·CRITICAL, imageUrl, suggestedServiceId? }` | `operations:proposals:create` | ✅ |
| US14 E2 Propuesta sin foto | Mismo endpoint sin `imageUrl` → **400** | `operations:proposals:create` | ✅ |
| US15 E1 Aprobación y alta automática | `POST /work-orders/{id}/proposals/{pid}/notify` (correo al cliente) → `POST …/approve { serviceId, finalPrice, laborHours }` (crea la tarea) | `operations:proposals:notify`, `operations:proposals:resolve` | ✅ |
| US15 E2 Desestimación | `POST …/proposals/{pid}/reject { customerNotes }` | `operations:proposals:resolve` | ✅ |
| US44 E1 Cliente con cuenta en la app | `GET /customers/affiliation-lookup?email=` → `nextStep: AFFILIATE` → `POST /customers/affiliations { email, taxId?, phone? }` | `crm:customers:read`, `crm:customers:create` | ✅ |
| US44 E2 Cliente presencial (walk-in) | `nextStep: REGISTER_WALK_IN` → `POST /customers/individuals` + `POST /vehicles` | `crm:customers:create`, `crm:vehicles:create` | ✅ |
| US44 E3 Conciliación posterior | Ocurre cuando el conductor crea su cuenta en la app de clientes | — | ⛔ |

## EP04 — Diagnóstico electrónico y telemetría OBD-II

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US16 E1 Emparejar escáner | Bluetooth (ELM327) en la app; `GET /iot/installations/vehicle/{vehicleId}/active` o `POST /iot/installations/install { deviceId, vehicleId, currentOdometerKm }` | `iot:devices:read`, `iot:installations:manage` | 📱 + ✅ |
| US16 E2 Pérdida de señal | Detección en la app (timeout de lectura, reconexión) | — | 📱 |
| US17 E1 Decodificar DTC | Modo 03 OBD-II en la app → `POST /iot/faults { vehicleId, dtcCode, severity, description }` por cada código | `iot:telemetry:ingest` | 📱 + ✅ |
| US17 E2 Sin averías | Modo 03 vacío → la app muestra "sin códigos"; `GET /iot/faults/vehicle/{vehicleId}/active` vacío | `iot:faults:read` | 📱 |
| US18 E1 Telemetría en vivo y umbrales | PIDs en vivo en la app (RPM, temperatura, voltaje…), umbrales en la app; se encola para el lote | — | 📱 |
| US18 E2 Congelar muestra | Snapshot local; se envía dentro del lote de telemetría | `iot:telemetry:ingest` | 📱 |
| US19 E1 Guardado local sin red | Room (tabla de lecturas pendientes) | — | 📱 |
| US19 E2 Sincronización en bloque | WorkManager con red → `POST /iot/telemetry/batch { vehicleId, readings[] }` (lecturas repetidas por `timestamp` se ignoran) | `iot:telemetry:ingest` | ✅ |
| US20 E1 Borrado de fallas | Modo 04 OBD-II en la app → re-lectura modo 03 → `POST /iot/faults/{id}/resolve` por cada DTC borrado | `iot:faults:resolve` | 📱 + ✅ |
| US20 E2 Falla persistente | La re-lectura vuelve a traer el DTC → no se resuelve (o se registra de nuevo con `POST /iot/faults`) | `iot:telemetry:ingest` | 📱 + ✅ |
| US21 E1 Dictamen predictivo con IA | `POST /iot/health-reports/generate?vehicleId= { daysToAnalyze: 7–90 }` (o `/generate-async`) → `GET /iot/health-reports/{reportId}` | `iot:health_reports:generate`, `iot:health_reports:read` | ✅ |
| US21 E2 Descargar PDF | `GET /iot/health-reports/{reportId}/pdf` | `iot:health_reports:read` | ✅ |
| US21 E3 Telemetría insuficiente | `POST /iot/health-reports/generate` → **422 `ERR_INSUFFICIENT_TELEMETRY`**; cuota mensual de IA agotada → 403 | `iot:health_reports:generate` | ✅ |

## EP05 — Presupuestos y cotizaciones

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US22 E1 Cálculo en cascada | `POST /work-orders/{id}/tasks`, `POST /tasks/{taskId}/products` → `GET /work-orders/{id}` (`subtotal`, `taxAmount`, `totalAmount`) | `operations:work_orders:create`, `operations:work_orders:update` | ✅ |
| US22 E2 Recálculo ante cambios | `PUT /tasks/{taskId}` (precio), `PUT /tasks/{taskId}/products/{productId}` (cantidad), `DELETE …/products/{productId}` | `operations:work_orders:update` | ✅ |
| US23 E1 Mecánico imputa repuesto | `POST /tasks/{taskId}/products { productId, quantity }` (solo en una tarea abierta asignada a él; si no, 403) → push `TASK_PART_CHARGED` a quien tenga `inventory:batches:restore` | `operations:tasks:charge_parts` (mecánico) o `operations:work_orders:update` | ✅ |
| US23 E2 Administrador retira el producto | `DELETE /tasks/{taskId}/products/{productId}` (reintegra al lote) | `operations:work_orders:update` | ✅ |
| US23 E3 Imputación directa por el administrador | `POST /tasks/{taskId}/products` | `operations:work_orders:update` | ✅ |
| US24 E1 Proforma en PDF | `GET /work-orders/{id}/quotation/pdf` | `operations:quotations:read` | ✅ |
| US24 E2 Envío al cliente | `POST /work-orders/{id}/quotation/send` (correo con el PDF; 422 si el cliente no tiene correo) | `operations:quotations:send` | ✅ |
| US25 E1 Conformidad → en progreso | `POST /work-orders/{id}/quotation/approve` | `operations:quotations:approve` | ✅ |
| US25 E2 Desistimiento → cancelada | `POST /work-orders/{id}/quotation/reject { reason }` | `operations:quotations:approve` | ✅ |

## EP06 — Ejecución de tareas en bahía

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US26 E1 Mis asignaciones | `GET /tasks/my` (en curso → en pausa → asignadas); placa con `GET /vehicles/{vehicleId}` | `operations:tasks:read`, `crm:vehicles:read` | ✅ |
| US26 E2 Aviso de nueva tarea | Push `TASK_ASSIGNED { workOrderId, taskId }` | — | ✅ |
| US27 E1 Inicio de cronómetro | `POST /tasks/{taskId}/start` | `operations:tasks:track_time` | ✅ |
| US27 E2 Pausa por refrigerio | `POST /tasks/{taskId}/pause { reason: BREAK }` → push `TASK_PAUSED` a supervisores | `operations:tasks:track_time` | ✅ |
| US27 E3 Fin de turno anticipado | `POST /tasks/{taskId}/pause { reason: END_OF_SHIFT }` con el turno vigente → rechazado | `operations:tasks:track_time` | ✅ |
| US27 E4 Pausa automática al marcar salida | `POST /hr/attendances/clock-out` pausa las tareas en curso del mecánico → push `TASK_PAUSED` | `hr:attendance:clock_out` | ✅ |
| US27 E5 Reanudación | `POST /tasks/{taskId}/start` (acumula `totalPausedSeconds`) → push `TASK_RESUMED` a supervisores | `operations:tasks:track_time` | ✅ |
| US28 E1/E2 Fotos de desmontaje y montaje | Supabase → `POST /tasks/{taskId}/evidence-images { imageUrl, description }` (indicar "desmontaje"/"montaje" en `description`) | `operations:tasks:upload_photos` | ✅ |
| US29 E1 Solicitud y aprobación de suspensión | `POST /tasks/{taskId}/hold { missingItemDescription, inventoryItemId? }` → push `TASK_SUSPENSION_REQUESTED` → `POST …/hold/approve` (pasa a `ON_HOLD`) | `operations:tasks:hold_request`, `operations:tasks:hold_validate` | ✅ |
| US29 E2 Desestimación | `POST /tasks/{taskId}/hold/reject` (sigue en progreso) | `operations:tasks:hold_validate` | ✅ |
| US29 E3 Reanudación con repuesto conforme | `POST /tasks/{taskId}/resume` (el mecánico no puede levantarla con `/start`) → push `TASK_RESUMED` al mecánico | `operations:tasks:hold_validate` | ✅ |
| US30 E1 Cierre técnico | `POST /tasks/{taskId}/complete { actualLaborHours, notes? }` | `operations:tasks:complete` | ✅ |
| US30 E2 Orden completada al cerrar la última tarea | Automático: la orden pasa a `COMPLETED` | — | ✅ |

## EP07 — Abastecimiento y descargo de repuestos FIFO

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US31 E1 Búsqueda con saldo | `GET /inventory/items?search=&category=` (`totalStock`, `basePrice`) | `inventory:parts:read` | ✅ |
| US31 E2 Saldo en cero | La app deshabilita el ítem si `totalStock = 0`; el backend responde **409** si se intenta imputar | `inventory:parts:read` | 📱 + ✅ |
| US32 E1 Descargo FIFO | Al imputar (`POST /tasks/{taskId}/products`) se reserva desde el lote más antiguo; `GET /inventory/batches/by-part/{partId}` | `inventory:batches:read` | ✅ |
| US32 E2 Multi-lote | Igual que E1; el descargo recorre varios lotes | — | ✅ |
| US32 E3 Saldo insuficiente | **409** "Insufficient stock for SKU: requested X, available Y" | — | ✅ |
| US33 E1 Reintegro al lote de origen | `DELETE /tasks/{taskId}/products/{productId}` → las unidades vuelven al lote del que salieron (el más reciente primero) | `operations:work_orders:update` | ✅ |
| US33 E2 Auditoría del reintegro | `GET /work-orders/{id}/part-movements`: cada descargo por lote con `returnedQuantity`, `lastReturnedBy` (usuario), `lastReturnedAt` y motivo | `inventory:batches:read` o `inventory:batches:restore` | ✅ |
| US34 E1 Alerta por stock mínimo | Al bajar del mínimo → push `LOW_STOCK` a quienes tienen `inventory:alerts:read`; bandeja: `GET /inventory/alerts` | `inventory:alerts:read` | ✅ |
| US34 E2 Desactivación tras la compra | `PUT /inventory/purchase-orders/{id}/receive` → el ítem sale de `GET /inventory/alerts` | `inventory:batches:receive` | ✅ |
| US35 E1 Precio de catálogo automático | `POST /tasks/{taskId}/products` sin `unitPrice` → usa `basePrice` | `operations:work_orders:update` | ✅ |
| US35 E2 Costo de ventas y margen FIFO | `GET /work-orders/{id}/profitability` (`partsRevenue`, `partsCostOfSales`, `partsGrossProfit` y detalle por línea) | `inventory:batches:read` o `invoicing:cashflow:export_pdf` | ✅ |
| US35 E3 Precio inmutable | El `unitPrice` queda fijado en la línea | — | ✅ |

## EP08 — Facturación electrónica y cierre

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US36 E1/E2 Liquidación en cascada | `GET /work-orders/{id}` (`subtotal`, `taxAmount` IGV 18 %, `totalAmount`), recalculado en cada cambio | `operations:work_orders:read` | ✅ |
| US37 E1 Factura a empresa | `POST /invoicing/vouchers { voucherType: "01", serie: "F001", customerInfo (RUC), lines[], workOrderId }` | `invoicing:invoices:issue_sunat` | ✅ |
| US37 E2 Boleta a persona | Igual con `voucherType: "03"`, serie `B001`, DNI | `invoicing:invoices:issue_sunat` | ✅ |
| US37 E3 Contingencia SUNAT | El comprobante queda `ISSUED` y se reintenta solo (outbox); `GET /invoicing/vouchers/{id}` muestra el estado | `invoicing:invoices:read` | ✅ |
| US38 E1 Pago íntegro | `POST /invoicing/payments { voucherId, branchId, amount, currency, paymentMethod, transactionReference }` | `invoicing:payments:create` | ✅ (el comprobante muestra `paidAmount`, `pendingBalance` y `paymentStatus`: `PENDING`, `PARTIAL`, `PAID`) |
| US38 E2 Pagos fraccionados | Varios `POST /invoicing/payments`; `paymentStatus` queda `PARTIAL` hasta saldar; el backend rechaza montos mayores al saldo | `invoicing:payments:create` | ✅ |
| US39 E1 Orden completada y aviso al asesor | Automático a `COMPLETED` al cerrar la última tarea → push `WORK_ORDER_COMPLETED` a quienes tienen `operations:vehicle_handover:execute` | — | ✅ |
| US39 E2 Orden pagada y pase de salida | El pago que salda el comprobante marca la orden `PAID` (o `POST /work-orders/{id}/mark-as-paid`, que da **422** si el comprobante tiene saldo) → `GET /work-orders/{id}/exit-pass/pdf` → `POST /work-orders/{id}/deliver` (libera la bahía) | `invoicing:payments:create`, `invoicing:exit_passes:issue`, `operations:vehicle_handover:execute` | ✅ |
| US39 E3 Salida con deuda | `GET /work-orders/{id}/exit-pass/pdf` → **422** si la orden no está `PAID` | `invoicing:exit_passes:issue` | ✅ |

## EP09 — Monitoreo operativo y gestión gerencial

| Historia · Escenario | Cómo se cumple | Permiso | Estado |
| --- | --- | --- | --- |
| US40 E1 Reasignar bahía | `PUT /work-orders/{id}/bay { bayId }`; mecánico: `PUT /tasks/{taskId}/mechanic` | `operations:bays:reassign` | ✅ |
| US40 E2 Bahía ocupada o en mantenimiento | `PUT /work-orders/{id}/bay` → **409** | `operations:bays:reassign` | ✅ |
| US40 E3 Tablero en tiempo real | `GET /work-bays` (`status`, `currentWorkOrderId`) con refresco periódico (15–30 s) mientras la pantalla está visible | `operations:bays:read` | 📱 + ✅ |
| US41 E1 Flujo de caja mensual en PDF | `GET /invoicing/financial-reports/cash-flow/pdf?from=&to=` | `invoicing:cashflow:export_pdf` | ✅ |
| US41 E2 Consolidado multisede anual | Mismo endpoint con el rango del año: consolida todas las sedes del taller | `invoicing:cashflow:export_pdf` | ✅ |
| US42 E1 Series tras el registro | `POST /invoicing/series-configurations { branchId, voucherType, serie, initialCorrelative? }`; PSE simulado administrado por la plataforma | `invoicing:fiscal_config:manage` | ✅ |
| US42 E2 Serie duplicada | Mismo endpoint → **409** | `invoicing:fiscal_config:manage` | ✅ |
| US43 E1 Informe pericial en PDF | `GET /iot/health-reports/{reportId}/pdf` | `iot:health_reports:read` | ✅ |
| US43 E2 Consolidado de flota | `GET /iot/health-reports/fleet/pdf?customerId=` (unidades críticas primero) | `iot:health_reports:read` | ✅ |

---

## Huecos cerrados

Los diez huecos detectados en la primera revisión (G1–G10) ya están implementados en el backend (commit `feat: close the staff-app gaps of the user stories (G1-G10)` en la rama del hotfix 1.0.3):

| Id | Historia | Qué se agregó |
| --- | --- | --- |
| G1 | US08 E1/E2 | `workedMinutes`, `regularMinutes` y `overtimeMinutes` en la asistencia; push `OVERTIME_RECORDED` a planilla |
| G2 | US09 E1 | `workDays` y `restDays` en el turno (request y response); sin recordatorio en día de descanso |
| G3 | US23 E1 | Permiso `operations:tasks:charge_parts` (mecánico y mecánico jefe) para imputar repuestos a su propia tarea |
| G4 | US27 E5, US29 E3 | Push `TASK_RESUMED` (supervisores tras una pausa; mecánico al levantarse la suspensión) |
| G5 | US33 | Registro por lote de cada repuesto imputado: el reintegro vuelve al lote de origen (antes **no** se devolvía stock) y queda quién y cuándo; `GET /work-orders/{id}/part-movements` |
| G6 | US34 E1 | Push `LOW_STOCK` a quienes tienen `inventory:alerts:read` |
| G7 | US35 E2 | `GET /work-orders/{id}/profitability` con costo de ventas FIFO y ganancia bruta |
| G8 | US38 | `paidAmount`, `pendingBalance` y `paymentStatus` en el comprobante |
| G9 | US39 | Push `WORK_ORDER_COMPLETED`; `mark-as-paid` exige el comprobante saldado y el pago que lo salda marca la orden como pagada |
| G10 | US06, US08 (sin red) | `clockInTime` opcional para marcaciones encoladas sin red (máx. 12 h) |

Solo quedan fuera del backend los escenarios marcados 📱 (los resuelve la app) y ⛔ (fuera de alcance).

## Huecos abiertos

| Id | Historia | Hueco | Cómo lo resuelve la app hoy |
| --- | --- | --- | --- |
| H1 | US01, US05 | Los correos de invitación y de restablecer contraseña traen un token, pero no hay deep link / App Link definido hacia la app | El colaborador pega el token en un campo; `ValidateInvitationUseCase`, `AcceptInvitationUseCase` y `ResetPasswordUseCase` lo reciben como texto |
| H2 | US06, US08 | [api/hr.md](api/hr.md) no documenta qué responde `GET /hr/attendances/employee/{membershipId}/status-today` cuando no hay jornada activa | El repositorio de `attendance` trata cualquier error o cuerpo vacío como "sin jornada" y usa `history` del día para lo demás (incluida la verificación de `CLOCK_OUT`). Confirmar contra el backend |
| H3 | US30 | `POST /tasks/{taskId}/complete` exige `actualLaborHours`, pero no se documenta si `startedAt` es el primer inicio o la última reanudación | `SuggestLaborHoursUseCase` sugiere `ahora − startedAt − totalPausedSeconds` y el mecánico confirma o corrige antes de cerrar. Confirmar contra el backend |
| H4 | US27 | No está documentado qué `status` deja `POST /tasks/{taskId}/pause` | Se asume `ON_HOLD` con `holdReason` `BREAK`/`END_OF_SHIFT` (enum `HoldReason`, [api/README.md](api/README.md) §8): `Task.isPausedByMechanic` vs `Task.isSuspended`. Confirmar contra el backend |
| H5 | US29 | No hay endpoint para que el supervisor liste las suspensiones pendientes (`GET /tasks/my` solo trae las propias) | El supervisor llega por el push `TASK_SUSPENSION_REQUESTED` y `GetTaskDetailUseCase`; en el paso 6, también desde el detalle de la orden |
| H6 | US29 | US29 E3 dice que reanuda el técnico, pero `POST /tasks/{taskId}/resume` exige `operations:tasks:hold_validate`; US29 nombra al mecánico jefe y `operations:tasks:hold_request` solo lo tiene el rol Mecánico ([02](02-roles-and-permissions.md)) | La app decide por permiso: *Solicitar suspensión* con `hold_request`, *Levantar suspensión* con `hold_validate` |
| H7 | US27 E3 | No se documenta el código HTTP del rechazo de una pausa `END_OF_SHIFT` anticipada | La app muestra el `message` del `ApiException` y no cambia la caché |
| H8 | US23, US33 | En `PUT`/`DELETE /tasks/{taskId}/products/{productId}` no se aclara si `productId` es el id de la línea (`TaskProductResource.id`) o el del catálogo (`TaskProductResource.productId`) | `UpdateTaskPartQuantityUseCase` y `RemovePartFromTaskUseCase` reciben el `productId` del catálogo, como sugiere el nombre del parámetro. Confirmar contra el backend |
