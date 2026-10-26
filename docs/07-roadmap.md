# Roadmap de construcción — Atelier Mobile

Orden de construcción de la app, paso a paso. **Cada paso se hace en su propia sesión** y en su propia rama. Al cerrar un paso se actualiza este archivo: es lo único que una sesión nueva necesita para saber dónde va el proyecto.

## Cómo empezar una sesión

```text
Lee docs/07-roadmap.md y haz el paso N. Primero un plan (con revisión de Gemini), luego implementa.
```

Reglas para todas las sesiones:

1. **Fuente de verdad:** `docs/` (01–06 y `api/`). `atelier-documentation.md` y `atelier-ecosystem-details.md` son solo contexto general y están desactualizados.
2. **Nada de diseño:** el tema, `core/designsystem/` (componentes, íconos, colores) y `docs/branding/` son de la otra parte del equipo. Las pantallas solo llevan estado, navegación y permisos.
3. **Antes de empezar:** el paso anterior debe estar commiteado y el árbol limpio (`git status`). La rama nueva sale de `develop` (o de la rama del paso anterior si aún no se mergeó).
4. **Una sesión por rama:** no trabajar dos sesiones a la vez sobre los mismos archivos.
5. **Reutilizar el núcleo** (`core/`, paso 1). No crear otro cliente HTTP, otra cola ni otro manejo de sesión.
6. **Al terminar:** `./gradlew assembleDebug testDebugUnitTest` en verde, docs actualizados si cambió algo, y este archivo marcado.

## Estado

| # | Paso | Estado | Rama |
| --- | --- | --- | --- |
| 0 | Configuración general (Gradle, paquete `com.andeva.atelier.mobile`) | ✅ Hecho | `release/0.1.0`, `release/0.1.1` |
| 1 | Núcleo `core/` | ✅ Hecho | `feature/core`, `release/0.2.0` |
| 2 | `auth` — sesión y acceso | ✅ Hecho; `presentation` solo A5 login, A3 registro por invitación y A4 código (falta A6 recuperar, A5b sede, Google y probar con backend) | `feature/auth` |
| 3 | `main` — contenedor con pestañas por permiso | ✅ Hecho sin `presentation` (falta UI) | `feature/main` |
| 4 | `attendance` — asistencia y turnos | ✅ Hecho sin `presentation` (falta UI y probar con backend) | `feature/attendance` |
| 5 | `tasks` — tareas del mecánico | ✅ Hecho sin `presentation` (falta UI y probar con backend) | `feature/tasks` |
| 6 | `reception` + `workorders` — recepción y órdenes | ✅ Hecho sin `presentation` (falta UI y probar con backend) | `feature/reception-workorders` |
| 7 | `inventory` — catálogo, lotes y alertas | ✅ Hecho sin `presentation` (falta UI y probar con backend) | `feature/inventory` |
| 8 | `diagnostics` — OBD-II, DTC e informe IA | ✅ Hecho sin `presentation` (falta UI y probar con un ELM327 y el backend) | `feature/diagnostics` |
| 9 | `bays`, `cashier`, `reports`, `team` | 🔄 En curso: `bays` hecho sin `presentation` (falta UI y probar con backend) | `feature/bays`, una rama por feature |

Leyenda: ⬜ pendiente · 🔄 en curso · ✅ hecho.

---

## Paso 0 — Configuración general ✅

Catálogo de versiones y dependencias de [05](05-android-architecture.md) §2, paquete `com.andeva.atelier.mobile`, minSdk 26, `BuildConfig` desde `local.properties` ([04](04-mobile-integration.md) §1) y `google-services` solo si existe `app/google-services.json`.

## Paso 1 — Núcleo `core/` ✅

Base que usan todas las features. Detalle en [05](05-android-architecture.md) §3 y §5 y en [06](06-offline-sync.md) §3–§5.

| Pieza | Dónde | Para qué la usa una feature |
| --- | --- | --- |
| `Session`, `Permission`, `Plan` | `core/domain` | Habilitar UI por permiso y plan (`session.has(...)`, `canUseTelemetry()`) |
| `ApiException`, `apiCall`, `apiCallUnit` | `core/domain`, `core/data/remote` | Llamar a la API y mostrar el `message` del backend |
| `SessionManager` | `core/data/local` | Guardar la respuesta de sign-in (`save(dto)`), sede/turno (`updateAssignment`), `clear()` |
| `AuthRefreshService` | `core/data/remote/session` | `sign-out` (el `refresh` ya lo maneja `TokenRefresher`) |
| `DeviceTokenRegistrar` | `core/notifications` | Registrar el token FCM tras el login y borrarlo al salir |
| `AppDatabase` + 18 Entities | `core/data/local`, `features/*/infrastructure/local` | Cada feature agrega **su DAO** a `AppDatabase` (las Entities ya existen) |
| `PendingOperationQueue`, `OperationHandler` | `core/data/sync` | Encolar capturas sin red y registrar su handler (`@IntoMap @StringKey`) — ver [06](06-offline-sync.md) §3 |
| `CacheMaintenanceDao` | `core/data/sync` | Borrar cachés al cerrar sesión |
| `SupabaseUploader`, `StoragePaths` | `core/media` | Comprimir y subir fotos; URL pública determinista |
| `PushRouter`, `PushInbox` | `core/notifications` | Abrir la pantalla correcta al tocar un push |
| `ConnectivityObserver` | `core/data/remote` | Deshabilitar acciones "en línea" sin red |
| `AppNavHost` | `navigation/` | Grafos `AuthNavGraphRoute` y `MainNavGraphRoute` con destinos vacíos para reemplazar |

Pendiente de este paso: probarlo en un emulador (la app debe abrir sin cerrarse).

---

## Paso 2 — `auth` ✅

- **Historias:** US01–US05 ([01](01-requirements/user-stories.md), EP01) · **API:** [api/iam.md](api/iam.md) · **Trazabilidad:** [03](03-traceability.md) EP01
- **Incluye:** login con correo y contraseña, Google Sign-In (`POST /auth/google-sign-in`), verificación OTP y reenvío, aceptar invitación (`validate` → `accept`), olvidé/restablecer contraseña, cierre de sesión. El alta de taller (`POST /auth/sign-up`) queda fuera: la hace el dueño en la web.
- **Tras el login:** `GET /hr/employees/membership/{membershipId}` con el token recibido → `SessionManager.save(dto)` + `updateAssignment(branchId, shiftId)` → `DeviceTokenRegistrar.registerCurrentToken()` → pedir `POST_NOTIFICATIONS` (API 33+). La sede se pide **antes** de guardar porque al guardar `AppNavHost` cambia de grafo; sin perfil laboral (404) se entra sin sede.
- **Cierre de sesión:** avisar si hay operaciones pendientes ([06](06-offline-sync.md) §3, "Cambio de usuario"; la cola se conserva) → `DeviceTokenRegistrar.clear()` → `POST /auth/sign-out` → `CacheMaintenanceDao.clearCaches()` → `SessionManager.clear()`.
- **Errores clave:** 401 credenciales · `code = EMAIL_NOT_VERIFIED` (403) → pantalla OTP · invitación: 404 inexistente/revocada, 409 ya aceptada o membresía activa, 410 vencida.
- **Sin red:** todo es en línea.
- **Token de invitación y de restablecimiento:** se pega a mano (hueco H1 en [03](03-traceability.md#huecos-abiertos)).

**Alcance entregado:** `domain`, `application` e `infrastructure` completos; `presentation` con **A5 Iniciar sesión**, **A3 Registro por invitación** (pegar enlace → formulario | vencida) y **A4 Verificar correo · código**, sobre el tema Atelier de `core/designsystem` (tokens de la galería `front-chi-ivory.vercel.app`, Plus Jakarta Sans, modo oscuro y claro). Archivos en `features/auth/presentation/{login,register,verifyemail,navigation}`; `AuthStartRoute` ya no existe: `AppNavHost` llama a `authNavGraph(navController)`.

Decisiones de flujo (contrato real, no el de la galería):

- **Invitación → app:** `POST /invitations/accept` devuelve la sesión completa, así que A3 **no** pasa por A4; al aceptar, `AppNavHost` entra solo.
- **A4 se abre desde el login:** cuando `sign-in` responde `EMAIL_NOT_VERIFIED`. Al verificar vuelve al login con el correo precargado y un aviso. "Vence en 15:00" solo aparece tras un reenvío hecho en la pantalla (la app no sabe cuándo se envió el código anterior); el reenvío se bloquea 60 s.
- **A3 necesita el enlace:** no hay deep link (H1), así que A3 empieza pidiendo pegar el enlace o el token (paso añadido a la galería). `RegisterRoute(token)` queda listo para el deep link.
- **A3 muestra el rol** de la invitación en lugar de la fecha de vencimiento: `GET /invitations/validate` no devuelve `expiresAt`.
- **Contraseña:** además de 8–64 caracteres (backend), la UI exige una mayúscula y confirmación, como la galería.
- **"¿Olvidaste tu contraseña?"** solo se dibuja si se pasa `onForgotPassword` (pendiente A6).

| Pieza lista para la UI | Dónde | Para qué |
| --- | --- | --- |
| Un caso de uso por acción (`SignInUseCase`, `GoogleSignInUseCase`, `VerifyEmailUseCase`, `ResendVerificationUseCase`, `ValidateInvitationUseCase`, `AcceptInvitationUseCase`, `ForgotPasswordUseCase`, `ResetPasswordUseCase`, `GetPendingOperationCountUseCase`, `SignOutUseCase`) | `features/auth/application` | Lo que llama cada ViewModel. Fallan con `ApiException` (mostrar `message`; `code = EMAIL_NOT_VERIFIED` → pantalla OTP) o `IOException` (sin red) |
| `AuthValidation` | `features/auth/domain` | Reglas de los campos (contraseña 8–64, teléfono, OTP de 6 dígitos) |
| `GoogleIdTokenProvider` | `features/auth/infrastructure/google` | Obtiene el `idToken` con Credential Manager (necesita contexto de Activity) para `GoogleSignInUseCase` |

Con éxito, sign-in, Google y aceptar invitación ya dejan la sesión guardada: `AppNavHost` cambia de grafo solo.

Pendiente de este paso: pantallas A6 (recuperar contraseña), A5b (elegir sede), cierre de sesión y pedido de `POST_NOTIFICATIONS`; probarlo contra el backend en un emulador; Google Sign-In cuando exista `GOOGLE_WEB_CLIENT_ID`.

## Paso 3 — `main` ✅

- **Incluye:** `MainScreen` con `NavigationBar` armada con `NavigationItem.tabsFor(session)` ([05](05-android-architecture.md) §5.5), pestaña "Más", aviso si `plan.accessGranted == false`, ícono con el contador de pendientes (`PendingOperationDao.countPending`) y pantalla "Pendientes de sincronizar" con *Reintentar* / *Descartar* ([06](06-offline-sync.md) §3, "Reglas de producto").
- **Consume** `PushInbox` para navegar al destino del push cuando la feature correspondiente exista.
- **Reemplaza** `MainStartRoute` en `AppNavHost`.

**Alcance entregado: solo `domain`, `application` e `infrastructure`.** La capa `presentation` (`MainScreen`, `NavigationBar`, `NavigationItem.tabsFor`, pestaña "Más", rutas, pantalla de pendientes, textos) la hace la otra parte del equipo, incluida la regla de pestañas por permiso. `MainStartRoute` sigue vacío en `AppNavHost`. No hay endpoints: todo es local.

| Pieza lista para la UI | Dónde | Para qué |
| --- | --- | --- |
| `ObserveSessionUseCase` → `Flow<Session?>` | `features/main/application` | Pestañas por permiso (`session.has(Permission.X)`) y aviso de plan (`session.hasSubscriptionAccess()`, false solo si `accessGranted == false`) |
| `ObservePendingOperationCountUseCase` → `Flow<Int>` | `features/main/application` | Ícono con contador; solo cuenta la cola del usuario de la sesión |
| `ObservePendingOperationsUseCase` → `Flow<List<PendingOperation>>` | `features/main/application` | Pantalla de pendientes: `type`, `status` (`PENDING`/`SYNCING`/`FAILED`), `lastErrorMessage` (el `message` del backend), `createdAt` |
| `RetryPendingOperationUseCase(id)` → `Boolean` | `features/main/application` | *Reintentar*: solo si está `FAILED`; reactiva también su cadena `FAILED` y dispara el sync |
| `DiscardPendingOperationUseCase(id)` → `Boolean` | `features/main/application` | *Descartar*: se lleva sus dependientes; un `ATTACH_*` se lleva su `UPLOAD_FILE` y la foto local; no actúa si algo de la cadena está `SYNCING` |
| `ObservePushDestinationUseCase` / `ConsumePushDestinationUseCase` | `features/main/application` | Ver el push tocado y consumirlo una sola vez para navegar |

Núcleo extendido: `PendingOperationDao.resetFailed`, `allDependentIds` y `deleteAll`; `Session.hasSubscriptionAccess()`.

Pendiente de este paso: la capa `presentation` (otra parte del equipo).

## Paso 4 — `attendance` ✅

- **Historias:** US06, US08, US09, US10 · **API:** [api/hr.md](api/hr.md) · [04](04-mobile-integration.md) §7 (geocerca)
- **Primera feature con cola offline:** `CLOCK_IN`, `CLOCK_OUT`, `SUBMIT_JUSTIFICATION` (handlers + `dedupe_key` de [06](06-offline-sync.md) §3 y §6).
- **Cachés:** `branches`, `shifts`, `attendances`.
- **Ubicación:** `LocationProvider` (interfaz en dominio, implementación con `FusedLocationProviderClient`), ubicación fresca, precisión ≤ 50 m, detectar ubicación simulada.

**Alcance entregado:** `domain`, `application` e `infrastructure` existentes, más `presentation` con la pantalla de asistencia, mi turno, historial y descargos. Archivos en `features/attendance/presentation/{AttendanceUiState.kt,AttendanceViewModel.kt,AttendanceScreen.kt,navigation/AttendanceNavGraph.kt}`. `AppNavHost` conecta temporalmente `MainStartRoute` con `AttendanceRoute` hasta integrar Attendance en el contenedor definitivo con pestañas de `main`.

Decisiones de flujo:

- **Ingreso:** la pantalla verifica `ATTENDANCE_CLOCK_IN`, la sede y el turno asignados, y solicita `ACCESS_FINE_LOCATION` al intentar marcar. Informa si la operación se registró o quedó pendiente de sincronización.
- **Salida:** verifica `ATTENDANCE_CLOCK_OUT` y utiliza `ClockOutUseCase`, respetando las operaciones pendientes.
- **Mi turno:** muestra el turno, la sede, la tolerancia y los días laborables o de descanso de lunes a domingo. Si no hay turno asignado, muestra el estado vacío.
- **Historial y descargos:** muestra los registros de asistencia y permite enviar un motivo de hasta 500 caracteres mediante `SubmitJustificationUseCase`, con el permiso `JUSTIFICATIONS_CREATE`.
- **Actualización:** al abrir la pantalla ejecuta `RefreshAttendanceUseCase`; también permite actualizar manualmente y muestra los errores correspondientes.
- **Navegación:** la conexión desde `MainStartRoute` es provisional; al integrar el contenedor de `main`, Attendance debe incorporarse como uno de sus destinos.

| Pieza lista para la UI | Dónde | Para qué |
| --- | --- | --- |
| `AttendanceUiState` | `features/attendance/presentation` | Estado de la jornada, turno, historial, carga, errores, avisos y formulario de descargo |
| `AttendanceViewModel` | `features/attendance/presentation` | Combina los casos de uso de jornada, turno e historial y ejecuta la actualización, ingreso, salida y envío de descargos |
| `AttendanceScreen` | `features/attendance/presentation` | Pantalla de asistencia, botones de ingreso/salida, turno semanal, historial y formulario de descargo; solicita el permiso de ubicación al intentar marcar ingreso |
| `attendanceNavGraph` / `AttendanceRoute` / `AttendanceHomeRoute` | `features/attendance/presentation/navigation` | Define el grafo y el destino de la pantalla de Attendance |
| `AppNavHost` | `navigation/` | Registra el grafo de Attendance y conecta temporalmente `MainStartRoute` con `AttendanceRoute` |
| `RefreshAttendanceUseCase` → `Result<Unit>` | `features/attendance/application` | Al abrir Asistencia. Recarga perfil laboral (sede y turno de la sesión), sedes y turno (24 h) y los últimos 30 días. Falla solo si no se pudo leer la asistencia: seguir con la caché y mostrar "Actualizado hace N min" |
| `ObserveTodayAttendanceUseCase` → `Flow<TodayAttendance>` | `features/attendance/application` | Botón de marcar: `NotClockedIn`, `ClockInQueued`, `ClockedIn`, `ClockOutQueued` o `Completed`. Incluye lo que espera señal, así no se marca dos veces |
| `ObserveAttendanceHistoryUseCase` → `Flow<List<Attendance>>` | `features/attendance/application` | Historial con `status`, minutos trabajados, regulares y sobretiempo, y estado del descargo |
| `ObserveWeeklyScheduleUseCase` → `Flow<WeeklySchedule?>` | `features/attendance/application` | US09: turno, sede, tolerancia y `days` de lunes a domingo; trae `branchId`/`shiftId` para marcar. `null` sin turno asignado |
| `ClockInUseCase(branchId, shiftId)` → `ClockInResult` | `features/attendance/application` | US06. `outcome` (`Recorded`, `Queued`, `AlreadyQueued`) y `geofence` (distancia, si está dentro y si la ubicación es simulada: solo avisos, decide el backend) |
| `ClockOutUseCase` → `SubmitOutcome` | `features/attendance/application` | US08. Sin red se encola detrás del ingreso pendiente |
| `SubmitJustificationUseCase(id, reason)` → `SubmitOutcome` | `features/attendance/application` | US10 E1. El dictamen llega por push `JUSTIFICATION_REVIEWED` (`PushDestination.MyAttendance`) |
| Excepciones | `features/attendance/domain` | `LocationPermissionDeniedException` (pedir permiso), `LocationUnavailableException` (activar ubicación), `LocationTooInaccurateException` (salir a cielo abierto), `MissingAssignmentException` (sin sede o turno), `InvalidJustificationException`. El resto es `ApiException` (mostrar `message`) |

`AlreadyQueued` significa que ese hecho ya está en la cola, pendiente o fallido: llevar a "Pendientes de sincronizar" (paso 3) para *Reintentar* o *Descartar*.

Núcleo extendido: `BusinessTime` (día de Lima), `ClockModule` (`Clock` inyectable), `PendingOperationDao.findByDedupeKey` / `observeByDedupeKey`, y `EmployeeService` movido de `auth` a `core/data/remote/employee/` (lo usan `auth` y `attendance`).

**Pendiente de este paso:** corregir y verificar cualquier error de compilación; ejecutar `./gradlew assembleDebug testDebugUnitTest`; probar contra el backend en un emulador el ingreso/salida, los permisos y la ubicación, el turno, el historial, los descargos y la sincronización offline. `status-today` sin jornada responde 404 (hueco H2, cerrado en [03](03-traceability.md#huecos-cerrados)).
## Paso 5 — `tasks` ✅

- **Historias:** US26–US30, US23 E1–E3, US33 E1, US35 E1/E3 · **API:** [api/operations.md](api/operations.md), [api/crm.md](api/crm.md) (placa), [api/inventory.md](api/inventory.md) (buscar repuesto)
- **En línea:** iniciar, pausar, suspender, reanudar, cerrar e imputar repuestos.
- **Offline:** fotos de desmontaje y montaje (`UPLOAD_FILE` + `ATTACH_TASK_EVIDENCE`).
- **Caché:** `tasks` (+ `vehicles` y `bays` para placa y bahía). **Push:** `TASK_ASSIGNED`, `TASK_PAUSED`, `TASK_RESUMED`, `TASK_SUSPENSION_REQUESTED`, `TASK_PART_CHARGED` (ya ruteados por `PushRouter`).
- **Fuera de este paso:** auditoría de reintegros `part-movements` (US33 E2) y rentabilidad `profitability` (US35 E2) son de la orden: pasan al paso 6.

**Alcance entregado: solo `domain`, `application` e `infrastructure`.** La capa `presentation` (Mis tareas, detalle, cronómetro, cámara, buscador de repuestos, rutas, textos y el pedido de `CAMERA`) la hace la otra parte del equipo.

| Pieza lista para la UI | Dónde | Para qué |
| --- | --- | --- |
| `ObserveMyTasksUseCase` → `Flow<List<Task>>` | `features/tasks/application` | US26 E1: en curso → en pausa → asignadas, con `plate`, `bayName`, `estimatedHours` y `serviceName` (este solo tras abrir el detalle) |
| `RefreshMyTasksUseCase` → `Result<Unit>` | `features/tasks/application` | Al abrir Mis tareas y al tocar `TASK_ASSIGNED`. Recarga `GET /tasks/my` y luego bahías por sede y vehículos que faltan (7 días), solo con `operations:bays:read` / `crm:vehicles:read`. Falla solo si no se pudo leer la lista: seguir con la caché |
| `ObserveTaskUseCase(id)` / `GetTaskDetailUseCase(id)` | `features/tasks/application` | Detalle sin red desde Room / detalle en línea con productos y evidencias. El segundo sirve también al supervisor que llega por push (no la guarda en Mis tareas) |
| `StartTaskUseCase`, `PauseTaskUseCase(id, PauseReason)` | `features/tasks/application` | US27. `Task.isPausedByMechanic` → botón *Reanudar* (`/start`). La pausa anticipada por fin de turno vuelve como `ApiException` con el `message` |
| `RequestSuspensionUseCase(id, missingItem, inventoryItemId?)` | `features/tasks/application` | US29 E1 (`operations:tasks:hold_request`). `Task.hasPendingSuspension` mientras espera al supervisor |
| `ApproveSuspensionUseCase`, `RejectSuspensionUseCase`, `ResumeSuspendedTaskUseCase` | `features/tasks/application` | US29 E1–E3 (`operations:tasks:hold_validate`). `Task.isSuspended` → solo un supervisor la levanta |
| `SuggestLaborHoursUseCase(task)` + `CompleteTaskUseCase(id, horas, notas?)` | `features/tasks/application` | US30. Precargar las horas sugeridas y que el mecánico las confirme; al cerrar la tarea sale de Mis tareas |
| `AttachTaskEvidenceUseCase(id, workOrderId, foto, EvidenceKind, nota?)` | `features/tasks/application` | US28. Comprime y encola siempre (con o sin red); `EvidenceKind` va al inicio de la `description` |
| `ObservePendingEvidenceUseCase(id)` → `Flow<List<PendingEvidence>>` | `features/tasks/application` | Fotos en cola con ícono de "pendiente": `localFilePath` hasta que sube, luego `imageUrl`; `FAILED` se maneja en "Pendientes de sincronizar" |
| `SearchPartsUseCase(texto)` → `PartOption` | `features/tasks/application` | US23 E1, US31 E2: en línea, `isAvailable = false` con saldo 0 |
| `AddPartToTaskUseCase`, `UpdateTaskPartQuantityUseCase`, `RemovePartFromTaskUseCase` | `features/tasks/application` | US23 E1–E3, US33 E1, US35 E1. Imputar con `charge_parts` (mecánico, tarea propia) o `work_orders:update`; cambiar o retirar solo con `work_orders:update`. 409 de stock con `message` |
| Excepciones | `features/tasks/domain` | `InvalidTaskInputException(field)` (dato inválido antes de llamar). El resto es `ApiException` (mostrar `message`) o `IOException` (sin red: deshabilitar con `ConnectivityObserver`) |

Núcleo extendido: `VehicleDao` (`features/reception`, `ReceptionDaoModule`), `BayDao` (`features/bays`, `BayDaoModule`), `PendingOperationDao.observeByTypeAndDedupePrefix`, `PendingUploadDao.observeByTarget`. Servicios de solo lectura `VehicleLookupService`, `WorkBayLookupService` y `PartSearchService` vivían en `features/tasks/infrastructure/remote`; los pasos 6, 7 y 9 los movieron a su feature (`VehicleService` en `reception`, `InventoryService` en `inventory`; `WorkBayLookupService` pasó a ser `WorkBayService` en `bays` y `tasks` ahora lo inyecta).

Pendiente de este paso: la capa `presentation` (otra parte del equipo) y probarlo contra el backend en un emulador. Los huecos H3, H4, H7 y H8 ya están confirmados contra el código del backend ([03](03-traceability.md#huecos-cerrados)). Queda H9: tras cambiar la cantidad de un repuesto, recargar la tarea en vez de usar la respuesta del `PUT` ([03](03-traceability.md#huecos-abiertos)).

## Paso 6 — `reception` + `workorders` ✅

- **Historias:** US11, US44, US12–US15, US22, US24, US25, US36, US33 E2, US35 E2 · **API:** [api/crm.md](api/crm.md), [api/operations.md](api/operations.md)
- **Reutiliza** `VehicleDao` y `BayDao` del paso 5 (ampliarlos, no crear otros).
- **Offline:** fotos de recepción (`ATTACH_INTAKE_IMAGE`) y propuestas (`SUBMIT_PROPOSAL`). El resto es en línea.
- **Propuestas:** `severity` solo `LOW`, `MEDIUM` o `CRITICAL`; la petición rechaza `HIGH` con 400 aunque el enum lo tenga ([api/README.md](api/README.md) §8).
- **Suspensiones (H5):** mostrar en el detalle de la orden las tareas con `pendingSuspensionReason` para que el supervisor las apruebe o rechace ([03](03-traceability.md#huecos-abiertos)).
- **Cachés:** `customers`, `vehicles`, `appointments`, `work_orders`. **PDF:** proforma ([04](04-mobile-integration.md) §8).
- **Llegada de cita (H10):** `arrive` devuelve la cita; la orden en borrador la crea el backend después, con kilometraje 0.
- **Fuera de este paso:** iniciar, completar o cancelar la orden, bahía (US40) y entrega, pago y pase de salida (paso 9). Agendar, confirmar, reprogramar o cancelar citas y flotas no tienen historia móvil.

**Alcance entregado: solo `domain`, `application` e `infrastructure`.** La capa `presentation` la hace la otra parte del equipo: agenda, búsqueda por placa, altas, afiliación, lista y detalle de orden, galería, propuesta, proforma, rutas, textos, el pedido de `CAMERA` y abrir o compartir el PDF con FileProvider (`ACTION_VIEW` / `ACTION_SEND`). Las acciones en línea se deshabilitan sin red con `ConnectivityObserver`.

| Pieza lista para la UI | Dónde | Para qué |
| --- | --- | --- |
| `FindVehicleByPlateUseCase(placa)` → `VehicleWithOwner?` | `features/reception/application` | US11 E1. Normaliza la placa (mayúsculas, sin espacios). null = primer ingreso (US11 E2). Sin red responde con la caché si la placa ya se vio |
| `SearchCustomersUseCase(texto)`, `GetCustomerVehiclesUseCase(id)` | `features/reception/application` | US11 E1 por documento. Sin red la búsqueda usa los clientes ya vistos |
| `ObserveCustomerUseCase(id)` / `ObserveVehicleUseCase(id)` | `features/reception/application` | Fichas desde Room |
| `RegisterIndividualCustomerUseCase`, `RegisterCompanyCustomerUseCase`, `RegisterVehicleUseCase` | `features/reception/application` | US11 E2, US44 E2. `InvalidReceptionInputException(field)` antes de llamar |
| `LookupAffiliationUseCase(correo)` → `nextStep` + `AffiliateCustomerUseCase(correo, taxId, teléfono?)` | `features/reception/application` | US44 E1/E2: `ALREADY_CUSTOMER`, `AFFILIATE` (afiliar; `taxId` obligatorio) o `REGISTER_WALK_IN` (altas) |
| `RefreshAppointmentsUseCase(fecha)` / `ObserveAppointmentsUseCase(fecha)` | `features/reception/application` | Agenda de la sede (hoy y mañana) con placa y cliente. `MissingBranchException` sin sede |
| `MarkAppointmentArrivedUseCase(id)` → `ArrivalResult` | `features/reception/application` | US11 E1, US12 E1. `WorkOrderReady(workOrderId)` → completar con `UpdateWorkOrderUseCase`; `WorkOrderPending` → volver a buscar con `FindDraftWorkOrderForVehicleUseCase` (H10) |
| `RefreshBranchWorkOrdersUseCase` / `ObserveBranchWorkOrdersUseCase` / `ObserveWorkOrderUseCase(id)` | `features/workorders/application` | Órdenes no cerradas de la sede y cabecera con totales e IGV (US36), sin red desde Room |
| `GetWorkOrderDetailUseCase(id)` | `features/workorders/application` | Tareas (`TaskDetail` de `tasks`), propuestas y fotos de recepción (US13 E2). Destino de `WORK_ORDER_COMPLETED`. `tasksAwaitingSuspensionReview` para el supervisor (H5), que actúa con `ApproveSuspensionUseCase` / `RejectSuspensionUseCase` de `tasks`. Volver a llamarlo tras imputar o retirar repuestos |
| `OpenWorkOrderUseCase(NewWorkOrder)` / `UpdateWorkOrderUseCase(id, km?, diagnóstico?)` | `features/workorders/application` | US12 E1, en la sede de la sesión. 409 = el vehículo ya tiene una orden abierta (`message`) |
| `AddWorkOrderTaskUseCase` / `UpdateWorkOrderTaskUseCase` → detalle recargado | `features/workorders/application` | US12 E2, US22 E1/E2. Mecánico opcional (`mechanicId` = membershipId) |
| `GetServiceCatalogUseCase` / `GetBranchMechanicsUseCase` | `features/workorders/application` | Listas para elegir servicio y mecánico, y nombre del servicio por `serviceId` (H11). Vacías sin `services:read` / `iam:members:read` |
| `AttachIntakePhotoUseCase(id, foto, descripción?)` + `ObservePendingIntakePhotosUseCase(id)` | `features/workorders/application` | US13 E1. Siempre por la cola (con o sin red); la foto local aparece "pendiente" hasta subir |
| `SubmitProposalUseCase(id, descripción, severidad, foto?, servicio?)` + `ObservePendingProposalsUseCase(id)` | `features/workorders/application` | US14. Sin foto → `InvalidWorkOrderInputException("photo")` (E2); `HIGH` no se envía. Por la cola, con el mecánico de la sesión |
| `NotifyProposalUseCase`, `ApproveProposalUseCase` (→ detalle), `RejectProposalUseCase` | `features/workorders/application` | US15 E1/E2 |
| `DownloadQuotationPdfUseCase(id)` → `File` · `SendQuotationUseCase(id)` → correo | `features/workorders/application` | US24. El PDF queda en `cacheDir/pdfs`; 422 sin correo del cliente (`message`) |
| `ApproveQuotationUseCase` / `RejectQuotationUseCase(id, motivo)` | `features/workorders/application` | US25: `IN_PROGRESS` o `CANCELED` (sale de la lista) |
| `GetPartMovementsUseCase(id)` / `GetProfitabilityUseCase(id)` | `features/workorders/application` | US33 E2 / US35 E2 (permisos de lotes o flujo de caja). La ganancia puede ser negativa: `BigDecimal` + `currency` |

Núcleo extendido: `PhotoCapture` (`core/media`, ahora también la usa `tasks`), `PdfDownloader` (`core/data/remote`, para los PDFs de los pasos 8 y 9), `MissingBranchException`, `EmployeeService.getByBranch`, permisos `MEMBERS_READ` y `SERVICES_READ`, y en `AppDatabase` los DAOs `CustomerDao`, `AppointmentDao` y `WorkOrderDao`. `VehicleService` reemplaza a `VehicleLookupService` de `tasks` y `VehicleDao.saveFromServer` evita el choque de placa. Sin cambios en el esquema Room.

También en este paso: `attendance` trata solo el 404 de `status-today` como "sin jornada" (H2), `UpdateTaskPartQuantityUseCase` devuelve la tarea recargada (H9) y `auth` acepta el enlace completo del correo como token (H1).

Pendiente de este paso: la capa `presentation` (otra parte del equipo) y probarlo contra el backend en un emulador, en especial H10 (cuánto tarda en aparecer la orden de una cita) y H11.

## Paso 7 — `inventory` ✅

- **Historias:** US31, US32, US34 · **API:** [api/inventory.md](api/inventory.md)
- **Cachés:** `parts`, `batches` (solo con `inventory:batches:read`). **Push:** `LOW_STOCK`.
- **Backend:** el catálogo se pagina en memoria, los lotes llegan del más nuevo al más viejo y con agotados, las alertas se calculan al consultar y `receive` responde 400 a todo (H12–H15 de [03](03-traceability.md#huecos-abiertos)).
- **Fuera de este paso:** crear o editar repuestos, proveedores y órdenes de compra; despacho FIFO directo, ingreso directo de lotes, `restore` y `resolve`/`evaluate` de alertas. El descargo y el reintegro de US32/US33 ocurren al imputar o retirar en `tasks`.

**Alcance entregado: solo `domain`, `application` e `infrastructure`.** La capa `presentation` la hace la otra parte del equipo:
- Pantallas: catálogo, búsqueda, lector de código de barras, detalle con lotes, bandeja de alertas, órdenes de compra y recepción.
- Rutas y textos.
- El pedido de `CAMERA` y abrir la cámara o el Photo Picker para el comprobante.

Recibir se deshabilita sin red con `ConnectivityObserver`. El push `LOW_STOCK` puede repetirse (H14): lleva a la bandeja, no muestra una alerta por push.

| Pieza lista para la UI | Dónde | Para qué |
| --- | --- | --- |
| `RefreshPartsCatalogUseCase(force?)` / `ObservePartsCatalogUseCase(categoría?)` | `features/inventory/application` | US31: catálogo completo en Room (1 h, página por página). `Part.isOutOfStock` resalta los agotados y `isSelectable` bloquea su selección (E2) |
| `SearchPartsCatalogUseCase(texto, categoría?)` | `features/inventory/application` | US31 E1: por nombre o SKU. Sin red busca en la caché |
| `FindPartBySkuUseCase(sku)` → `Part?` | `features/inventory/application` | US31 E1 desde el lector de código. null = no existe; formato inválido → `InvalidInventoryInputException("sku")` sin llamar |
| `ObservePartUseCase(id)` / `GetPartDetailUseCase(id)` | `features/inventory/application` | Ficha y detalle con lotes en orden FIFO (US32, solo con `inventory:batches:read`). Destino del push `LOW_STOCK` (`itemId`) |
| `RefreshPartBatchesUseCase(id)` / `ObservePartBatchesUseCase(id)` | `features/inventory/application` | US32: lotes del más antiguo al más nuevo; `Batch.isActive` separa los agotados. Desde la caché, `initialQty`, proveedor y comprobante van en null |
| `GetStockAlertsUseCase` / `ObserveStockAlertsUseCase` | `features/inventory/application` | US34 E1: bandeja en línea y `Flow` desde la caché, que se vacía sola al recibir la compra (E2) |
| `GetInventoryValuationUseCase` | `features/inventory/application` | Valorización en PEN (`inventory:batches:read`) |
| `GetPurchaseOrdersUseCase(estado = ISSUED)` / `GetPurchaseOrderUseCase(id)` | `features/inventory/application` | US34 E2: órdenes emitidas y sus líneas (`inventory:purchase_orders:read`) |
| `ReceivePurchaseOrderUseCase(id, número, foto?, receivedAt?)` | `features/inventory/application` | US34 E2 (`inventory:batches:receive`). Sin foto → `InvalidInventoryInputException("photo")`. Sube el comprobante, recibe y recarga catálogo y lotes. Si la orden ya estaba recibida responde éxito sin subir la foto (H15) |

Núcleo extendido: `StoragePaths.purchaseReceipt`, permisos `BATCHES_RECEIVE` y `PURCHASE_ORDERS_READ`, y en `AppDatabase` los DAOs `PartDao` y `BatchDao`. `PartSearchService` de `tasks` pasa a ser `InventoryService` en `inventory` (`tasks` sigue buscando en línea para imputar). Sin cambios en el esquema Room.

Pendiente de este paso: la capa `presentation` (otra parte del equipo) y probarlo contra el backend en un emulador, en especial la recepción con comprobante (H15).

## Paso 8 — `diagnostics` ✅

- **Historias:** US16–US21, US43 E1 · **API:** [api/iot.md](api/iot.md) · [04](04-mobile-integration.md) §4 (ELM327)
- **Offline:** `telemetry_readings` → `TELEMETRY_BATCH`, `REGISTER_FAULT`, `RESOLVE_FAULT` ([06](06-offline-sync.md) §6).
- **En línea:** instalar o retirar el escáner, informe IA, alertas predictivas.
- **Visible solo si** `session.canUseTelemetry()` / `canUseAiDiagnostics()`. Foreground service `connectedDevice` mientras dura la lectura en vivo.
- **Fuera de este paso:** PDF de flota (US43 E2, paso 9), `generate-async`, registrar dispositivos, telemetría histórica, alertas del taller y convertir alertas en citas.

**Alcance entregado: solo `domain`, `application` e `infrastructure`.** La capa `presentation` la hace la otra parte del equipo:
- Pantallas: elegir adaptador y escáner, conexión, lectura en vivo con avisos, códigos de falla, borrado, informe IA y alertas.
- Rutas y textos.
- El pedido de `BLUETOOTH_CONNECT` (API 31+) y abrir o compartir el PDF con FileProvider.

El `vehicleId` sale de `Task.vehicleId` (paso 5) o de `FindVehicleByPlateUseCase` (paso 6). Instalar y el informe IA se deshabilitan sin red con `ConnectivityObserver`; conectar, leer, escanear y borrar funcionan sin red.

| Pieza lista para la UI | Dónde | Para qué |
| --- | --- | --- |
| `GetPairedObdAdaptersUseCase` → `ObdAdapter` | `features/diagnostics/application` | US16: adaptadores emparejados en Android (sin escaneo). `BluetoothPermissionDeniedException` → pedir permiso; `BluetoothDisabledException` → activar Bluetooth |
| `GetObdDevicesUseCase` / `GetActiveInstallationUseCase(vehicleId)` / `InstallObdDeviceUseCase` / `UninstallObdDeviceUseCase` | `features/diagnostics/application` | US16 en línea. Sin instalación activa el lote de telemetría responde 404 (H16): conviene instalar antes de capturar. `ObdDevice.matches(address)` solo sugiere el escáner (H17) |
| `ConnectObdUseCase(address)` → `ObdConnectionInfo` / `DisconnectObdUseCase` | `features/diagnostics/application` | US16 E1: protocolo y VIN. E2: `ObdNoResponseException` (contacto apagado o sin alimentación) u `ObdAdapterUnreachableException` |
| `StartLiveTelemetryUseCase(vehicleId)` / `StopLiveTelemetryUseCase` / `ObserveLiveTelemetryUseCase` → `StateFlow<LiveTelemetry>` | `features/diagnostics/application` | US18 E1, US19: muestra cada 2 s, `warnings` por umbral, estado `STREAMING` / `RECONNECTING` / `LOST`. Lotes de 200 por la cola |
| `FreezeTelemetryUseCase` → `TelemetrySnapshot` | `features/diagnostics/application` | US18 E2: congela la última muestra (ya viaja en el lote; no se puede adjuntar a la orden, H19) |
| `ScanFaultsUseCase(vehicleId)` → `FaultScanResult` | `features/diagnostics/application` | US17: modo 03 + cuadro congelado (modo 02). `isClean` = sin códigos (E2). Los nuevos quedan `isPending` hasta sincronizar |
| `ClearFaultsUseCase(vehicleId)` → `ClearFaultsResult` | `features/diagnostics/application` | US20 (`iot:faults:resolve`): `cleared` (E1) y `persistent` (E2, revisar el circuito) |
| `RefreshVehicleFaultsUseCase` / `ObserveVehicleFaultsUseCase` / `ObserveDiagnosticsSyncStateUseCase` | `features/diagnostics/application` | Fallas activas desde Room y lecturas o fallas que esperan señal (US19 E1) |
| `GenerateHealthReportUseCase(vehicleId, días 7–90)` → `HealthReport` | `features/diagnostics/application` | US21 E1. `InsufficientTelemetryException` (E3); 403 cuota/plan y 502 IA como `ApiException` con `message` |
| `GetLatestHealthReportUseCase` / `GetHealthReportDetailUseCase(reportId)` / `DownloadHealthReportPdfUseCase` | `features/diagnostics/application` | US21 E1/E2, US43 E1: detalle (subsistemas, riesgos, servicios sugeridos, correlaciones DTC) y PDF en `cacheDir/pdfs` |
| `RefreshPredictiveAlertsUseCase` / `ObservePredictiveAlertsUseCase` / `AcknowledgePredictiveAlertUseCase` | `features/diagnostics/application` | Alertas predictivas del vehículo (`iot:alerts:read` / `acknowledge`) |
| Excepciones | `features/diagnostics/domain` | `ObdNotConnectedException`, `ObdConnectionLostException`, `InvalidDiagnosticsInputException(field)`. El resto es `ApiException` (mostrar `message`) o `IOException` |

Núcleo extendido: en `AppDatabase` los DAOs `TelemetryReadingDao`, `VehicleFaultDao` y `PredictiveAlertDao` (sin cambios en el esquema Room). La ubicación del lote reutiliza `LocationProvider` de `attendance` (opcional). `ObdSessionService` crea su propio canal "Diagnóstico"; `core/notifications` no cambia.

Pendiente de este paso: la capa `presentation` (otra parte del equipo) y probarlo con un ELM327 real y contra el backend, en especial H16 (lote sin instalación) y H17 (qué es `deviceIdentifier`).

## Paso 9 — `bays`, `cashier`, `reports`, `team` 🔄

- **`bays`:** US40 · tablero con refresco cada 15–30 s mientras la pantalla está visible.
- **`cashier`:** US37–US39 · todo en línea (dinero y documentos fiscales); pase de salida en PDF. `voucherType` va y vuelve como código SUNAT (`"01"` factura, `"03"` boleta, `"07"` nota de crédito), no como nombre del enum ([api/README.md](api/README.md) §8).
- **`reports`:** US41, US43 E2 · PDFs de flujo de caja y de flota.
- **`team`:** US10 (revisar descargos) y asistencia diaria de la sede.

### Parte `bays` ✅ (sin `presentation`)

Solo `domain`, `application` e `infrastructure`; la pantalla del tablero, las rutas y los textos los hace la otra parte del equipo. Caché `bays:{branchId}` compartida con `tasks`; sin cambios en el esquema Room. Huecos H24 y H25 en [03](03-traceability.md#huecos-abiertos).

| Pieza lista para la UI | Dónde | Para qué |
| --- | --- | --- |
| `RefreshBayBoardUseCase` → `Result<Unit>`; `ObserveBayBoardUseCase` → `Flow<List<BayBoardItem>>` | `features/bays/application` | US40 E3: tablero desde Room. Emite enseguida sin mecánicos ni tiempo y los completa cuando llegan los detalles de las órdenes. `laborElapsed` es null si ninguna tarea está `IN_PROGRESS`. El tiempo transcurrido solo avanza con cada refresco (30 s); la UI puede contarlo localmente. Sin sede, Refresh falla con `MissingBranchException` y Observe emite una lista vacía |
| `PollBayBoardUseCase` → `Flow<Result<Unit>>` | `features/bays/application` | US40 E3: refresca ya y cada 30 s mientras se recoge; recogerlo solo con la pantalla visible. Sin `operations:bays:read` termina sin emitir ni llamar |
| `ReassignBayUseCase(workOrderId, targetBay)` | `features/bays/application` | US40 E1/E2 (`operations:bays:reassign`). Si `WorkBay.canReceive` es falso falla con `BayUnavailableException` sin llamar (deshabilitar esas bahías); si otro la ocupó antes, 409 con el `message`. `bayType` solo se muestra (H25) |
| `ReassignTaskMechanicUseCase(taskId, mechanicId)` | `features/bays/application` | US40 E1. `mechanicId` = membershipId (H11); la lista sale de `GetBranchMechanicsUseCase`. Tras reasignar, el tablero muestra un momento los ocupantes sin mecánico hasta que se recargan los detalles |
| Excepciones | `features/bays/domain` | `BayUnavailableException`. El resto es `ApiException` (mostrar `message`) o `IOException` (deshabilitar con `ConnectivityObserver`) |

---

## Pendientes y riesgos conocidos

| Tema | Detalle | Afecta |
| --- | --- | --- |
| Prueba en emulador del paso 1 | No se pudo ejecutar (no había AVD) | Paso 1 |
| Configuración local | Faltan `API_BASE_URL`, `GOOGLE_WEB_CLIENT_ID`, `SUPABASE_*` en `local.properties` y `app/google-services.json` | Pasos 2, 5, 6 |
| Tokens en DataStore sin cifrar | `SessionManager` guarda ambos tokens en DataStore normal ([api/README.md](api/README.md) §2). Cifrarlos (Tink / Android Keystore) es decisión de la app, no del backend | Decidir antes de producción |
| Pantallas sin diseño | Las pantallas se entregan funcionales; el estilo lo aplica la otra parte del equipo | Todos |
