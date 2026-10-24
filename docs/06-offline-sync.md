# Offline y sincronización — modelo local y cola de operaciones

Diseño completo de la persistencia local (Room) y de la sincronización de la app de staff. Parte del modelo propuesto en el informe (`atelier-report/docs/backend-documentation/atelier-database-schema.md`, sección 9, *Modelo de Datos Local Móvil*) y lo ajusta al backend real después de la versión **1.0.3**.

> Regla de oro: **la app nunca decide reglas de negocio sin red.** Lo que el backend valida con su propio reloj o con stock en vivo se hace solo en línea. Sin red se puede **leer** (caché) y **capturar** hechos que ocurrieron en un momento conocido (marcaciones, lecturas OBD-II, fotos), que se envían después.

---

## 1. Resultado de la revisión del modelo del informe

El informe define 26 tablas locales. Contrastadas con el código actual:

| Tabla del informe | Decisión | Motivo |
| --- | --- | --- |
| `pending_sync_events` | ✅ **Se mantiene** como `pending_operations` (única cola) | Se agregan dueño (`user_id`, `tenant_id`), clave de deduplicación y próximo intento |
| `local_cache_metadata` | ✅ Se mantiene como `cache_metadata` | Sin `version_hash`: el backend no emite ETag ni acepta `since`; la sincronización es **recarga completa por alcance** |
| `auth_session` | 🔁 **Pasa a DataStore** (`SessionManager`) | Es un registro único con tokens; DataStore es el patrón del curso. Campos ajustados a la respuesta real |
| `local_permissions_cache` | ❌ Se elimina | Los permisos del usuario llegan en la sesión; `GET /permissions` es solo para dueño y administrador |
| `local_customers_cache` | ✅ Se mantiene (`customers`) | Campos ajustados a `CustomerResource` |
| `local_vehicles_cache` | ✅ Se mantiene (`vehicles`) | `brand` y `model` separados; se agregan `year` y `current_mileage` |
| `local_appointments_cache` | ✅ Se mantiene (`appointments`) | El backend devuelve `customerId`/`vehicleId`, no nombres ni placa: se resuelven con las cachés de clientes y vehículos |
| `offline_reception_mutations` | ❌ Se elimina (llegada **en línea**) | `POST /appointments/{id}/arrive` no tiene body y crea la orden de trabajo; repetirlo no es idempotente |
| `local_bays_cache` | ✅ Se mantiene (`bays`) | Enum en mayúsculas (`LIFT`, `AVAILABLE`…) |
| `local_work_orders_cache` | ✅ Se mantiene (`work_orders`) | Sin placa ni cliente embebidos (se resuelven por id); se agregan `subtotal` y `tax_amount` |
| `local_tasks_cache` | ✅ Se mantiene (`tasks`) | Campos de `AssignedTaskResource` + detalle; sin `actual_hours` ni `paused_at` (el backend no los expone) |
| `offline_pit_mutations` | ❌ Se elimina (tareas **en línea**) | Iniciar, pausar y cerrar usan la hora del servidor; la pausa por fin de turno se valida contra el turno en ese instante |
| `offline_pending_evidences` | 🔁 Se mantiene como `pending_uploads` | Subida directa a **Supabase**, sin URL prefirmada; tres destinos (recepción, tarea, propuesta) |
| `local_inventory_cache` | ✅ Se mantiene (`parts`) | Se agrega `unit_of_measure` |
| `local_batches_cache` | ✅ Se mantiene (`batches`) | Se llena solo si el usuario tiene `inventory:batches:read` (dueño, admin, almacén); para el resto queda vacía |
| `local_suppliers_cache` | ❌ Se elimina | Requiere `inventory:suppliers:manage`; ninguna historia móvil lo necesita sin red |
| `offline_inventory_reservations` | ❌ Se elimina (imputación **en línea**) | El backend valida stock (409), que la tarea sea propia y descarga FIFO en ese momento |
| `local_attendance_cache` | ✅ Se mantiene (`attendances`) | Se agregan `worked_minutes`, `regular_minutes`, `overtime_minutes` y el estado del descargo |
| `local_shift_cache` | ✅ Se mantiene (`shifts`) | Se agregan `work_days`, `rest_days`, `is_active` |
| `offline_attendance_mutations` | 🔁 Se integra en `pending_operations` (`CLOCK_IN`, `CLOCK_OUT`) | El backend acepta `clockInTime` (≤ 12 h) y `clockOutTime` desde 1.0.3 |
| `local_voucher_status_cache` | ✅ Se mantiene (`vouchers`) | Ahora existen `paidAmount`, `pendingBalance` y `paymentStatus` |
| `offline_payment_collections` | ❌ Se elimina (cobro **en línea**) | Exige comprobante emitido y referencia de operación, rechaza montos mayores al saldo y marca la orden como pagada; sin idempotencia un reintento duplicaría dinero |
| `local_subscription_cache` | 🔁 **Pasa a la sesión** (`plan` en DataStore) | `GET /billing/subscriptions/me` es solo para dueño y administrador; desde ahora la respuesta de sesión trae `plan` (tier, estado y flags de IoT e IA) para todos los roles |
| `local_plan_features_cache` | ❌ Se elimina | Se cubre con `plan.iotTelemetryEnabled` y `plan.aiDiagnosticsEnabled` de la sesión |
| `local_telemetry_buffer` | ✅ Se mantiene (`telemetry_readings`) | Nombres de `TelemetryReadingItemDto`; `engine_temp_celsius` pasa a **obligatorio**; ruta `POST /iot/telemetry/batch` |
| `local_vehicle_faults_cache` | ✅ Se mantiene (`vehicle_faults`) | Se agregan `is_resolved` y `resolved_at`; ruta `/iot/faults/vehicle/{id}/active` |
| `local_predictive_alerts_cache` | ✅ Se mantiene (`predictive_alerts`) | Sin push de alertas al staff: se refresca por consulta; estados incluyen `DISMISSED` |

### Rutas del informe que cambiaron

| En el informe | En el backend actual |
| --- | --- |
| `POST /api/v1/iam/auth/refresh-token` | `POST /api/v1/auth/refresh` |
| `GET /api/v1/iam/permissions` | Permisos dentro de la respuesta de sesión |
| `GET /api/v1/crm/customers?since=…` | `GET /api/v1/customers?search=&type=&status=` (sin `since`) |
| `GET /api/v1/crm/vehicles/by-plate/{plate}` | `GET /api/v1/vehicles/by-plate/{plate}` |
| `GET /api/v1/crm/appointments?branchId&date` | `GET /api/v1/appointments?branchId=&date=` |
| `GET /api/v1/mro/work-bays?branchId` | `GET /api/v1/work-bays?branchId=` |
| `GET /api/v1/mro/work-orders?status=in_progress` | `GET /api/v1/work-orders?branchId=` (filtrar estado en la app) |
| `GET /api/v1/mro/tasks/assigned` | `GET /api/v1/tasks/my` |
| `POST /api/v1/mro/tasks/{id}/actions` | `POST /tasks/{id}/start`, `/pause`, `/hold`, `/complete` (en línea) |
| `POST …/evidences/presigned-url` + `POST …/evidences` | Subida a Supabase + `POST /tasks/{id}/evidence-images` |
| `POST /api/v1/inventory/items/{id}/allocate` | `POST /api/v1/tasks/{taskId}/products` (en línea) |
| `GET /api/v1/hr/attendance/my-record` | `GET /api/v1/hr/attendances/employee/{membershipId}/history?startDate=&endDate=` y `/status-today` |
| `POST /api/v1/hr/attendance/clock-in` / `clock-out` | `POST /api/v1/hr/attendances/clock-in` / `clock-out` |
| `POST /api/v1/invoicing/payments/offline-sync` | No existe (cobro en línea: `POST /api/v1/invoicing/payments`) |
| `GET /api/v1/billing/subscriptions/tenant-quota` | `plan` en la respuesta de `sign-in` / `refresh` (y `GET /api/v1/billing/subscriptions/me` para dueño y admin) |
| `POST /api/v1/iot/telemetry/ingest/batch` | `POST /api/v1/iot/telemetry/batch` |
| `GET /api/v1/iot/faults/vehicle/{vehicleId}` | `GET /api/v1/iot/faults/vehicle/{vehicleId}/active` |

---

## 2. Qué funciona sin red

| Acción | Sin red | Cómo |
| --- | --- | --- |
| Ver mis tareas, detalle de orden, bahías, catálogo de repuestos, turno, sede, citas del día, clientes y vehículos ya vistos | ✅ Lectura | Cachés Room (§4). Del detalle de orden, sin red solo la cabecera (`work_orders`) y las fotos y propuestas en cola: tareas, propuestas e imágenes ya enviadas no tienen tabla y requieren red |
| Marcar ingreso / salida (US06–US08) | ✅ Captura y envío diferido | `CLOCK_IN` / `CLOCK_OUT` en la cola, con hora y GPS del dispositivo |
| Leer DTC y telemetría OBD-II (US16–US19) | ✅ Captura y envío diferido | `telemetry_readings` + `REGISTER_FAULT` |
| Fotos de recepción, desmontaje/montaje y propuestas (US13, US14, US28) | ✅ Captura y envío diferido | `pending_uploads` + `ATTACH_*` |
| Enviar descargo de tardanza (US10) | ✅ Envío diferido | `SUBMIT_JUSTIFICATION` |
| Iniciar, pausar, suspender o cerrar tarea (US27, US29, US30) | ❌ En línea | El backend usa su hora y valida el turno |
| Imputar o retirar repuestos (US23, US33) | ❌ En línea | Stock FIFO y reglas de la tarea en vivo |
| Recibir una orden de compra con su comprobante (US34 E2) | ❌ En línea | Crea lotes y cambia el stock; el backend no es idempotente (H15) |
| Abrir orden, llegada de cita, registrar cliente/vehículo (US11, US12, US44) | ❌ En línea | Generan ids y correlativos del servidor |
| Proforma, aprobar propuestas, emitir comprobante, cobrar, pase de salida (US15, US24, US37–US39) | ❌ En línea | Documentos fiscales y dinero |
| Informe de salud con IA (US21) | ❌ En línea | Inferencia en el servidor y cuota mensual |

En las acciones en línea la UI deshabilita el botón sin conexión y muestra "Requiere conexión". No se encolan.

---

## 3. Cola de operaciones (`pending_operations`)

Una sola tabla para todo lo que se captura sin red. Reemplaza `pending_sync_events` y las colas por contexto del informe.

| Columna | Tipo SQLite | Nulo | Descripción |
| --- | --- | --- | --- |
| `id` | TEXT (UUID) PK | no | Identificador local de la operación |
| `type` | TEXT | no | Tipo (tabla de abajo) |
| `payload` | TEXT (JSON) | no | Body exacto a enviar, ya con la hora del dispositivo |
| `dedupe_key` | TEXT | no | Evita encolar dos veces el mismo hecho (p. ej. `CLOCK_IN:{membershipId}:{fecha}`) |
| `user_id` | TEXT | no | Usuario que la capturó |
| `tenant_id` | TEXT | no | Taller de la sesión |
| `status` | TEXT | no | `PENDING`, `SYNCING`, `FAILED` (las exitosas se borran) |
| `attempts` | INTEGER | no | Reintentos realizados |
| `next_attempt_at` | INTEGER (epoch ms) | no | Cuándo puede volver a intentarse (backoff) |
| `last_http_status` | INTEGER | sí | Último código HTTP |
| `last_error_code` | TEXT | sí | `code` del error del backend |
| `last_error_message` | TEXT | sí | `message` del backend, para mostrarlo |
| `created_at` | INTEGER (epoch ms) | no | Momento de la captura en el dispositivo |
| `depends_on` | TEXT | sí | `id` de otra operación que debe completarse antes (p. ej. la subida de la foto) |

Índices: `UNIQUE(dedupe_key)`, `(status, next_attempt_at)`, `(user_id)`.

### Tipos de operación

| `type` | Endpoint | Payload | Idempotencia al reintentar |
| --- | --- | --- | --- |
| `CLOCK_IN` | `POST /hr/attendances/clock-in` | `{ branchId, shiftId, latitude, longitude, clockInTime }` | 409 = ya existe una jornada abierta → **se considera aplicada** |
| `CLOCK_OUT` | `POST /hr/attendances/clock-out` | `{ attendanceId?, clockOutTime }` | 400 ("salida ya registrada" o reloj desajustado) y 404 ("sin jornada abierta" o salida sin ingreso) son ambiguos: se verifican con `history` del día. Si la jornada ya tiene salida → aplicada; si no → FAILED con el `message`. Sin red para verificar → se reintenta |
| `SUBMIT_JUSTIFICATION` | `POST /hr/attendances/{attendanceId}/justification` | `{ attendanceId, reason }` (el id va en la ruta; el body es `{ reason }`) | 422 "ya justificada" → aplicada |
| `UPLOAD_FILE` | `POST {SUPABASE_URL}/storage/v1/object/atelier/{ruta}` | archivo de `pending_uploads` | Misma ruta = mismo objeto (usar `x-upsert: true`) |
| `ATTACH_INTAKE_IMAGE` | `POST /work-orders/{id}/intake-images` | `{ imageUrl, description }` | ✅ Misma `imageUrl` en la orden → devuelve la imagen ya guardada (R4) |
| `ATTACH_TASK_EVIDENCE` | `POST /tasks/{id}/evidence-images` | `{ imageUrl, description }` | ✅ Misma `imageUrl` en la tarea → devuelve la evidencia ya guardada (R4) |
| `SUBMIT_PROPOSAL` | `POST /work-orders/{id}/proposals` | `{ mechanicId, description, severity, imageUrl, suggestedServiceId? }` | ✅ Misma `imageUrl` en la orden → devuelve la propuesta ya registrada (R4) |
| `TELEMETRY_BATCH` | `POST /iot/telemetry/batch` | `{ vehicleId, readings[] }` (hasta 200) | ✅ El backend ignora lecturas repetidas (mismo vehículo y `timestamp`) |
| `REGISTER_FAULT` | `POST /iot/faults` | `{ vehicleId, dtcCode, severity, description }` | ✅ Si el vehículo ya tiene ese DTC activo, devuelve esa falla (R3) |
| `RESOLVE_FAULT` | `POST /iot/faults/{id}/resolve` | `{}` | ✅ Resolver dos veces devuelve 200 |

`ATTACH_*` y `SUBMIT_PROPOSAL` dependen (`depends_on`) de su `UPLOAD_FILE`: el worker no los envía hasta que la subida termine. La URL pública es determinista (`{SUPABASE_URL}/storage/v1/object/public/{bucket}/{storage_path}`, `SupabaseUploader.publicUrlFor`), así que el payload de `ATTACH_*` se arma con la URL final **al capturar** y no hay que reescribirlo después. Si `UPLOAD_FILE` queda `FAILED`, sus dependientes pasan a `FAILED` con el mismo mensaje.

### Cómo registra cada feature su tipo

El núcleo no conoce endpoints de features. Cada feature implementa `OperationHandler` (`core/data/sync/OperationHandler.kt`) y lo registra en su módulo de Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface AttendanceSyncModule {
    @Binds @IntoMap @StringKey("CLOCK_IN")
    fun bindClockIn(handler: ClockInHandler): OperationHandler
}

class ClockInHandler @Inject constructor(private val service: AttendanceService, private val gson: Gson) : OperationHandler {
    override suspend fun send(operation: PendingOperationEntity): OperationResult {
        val body = gson.fromJson(operation.payload, ClockInRequestDto::class.java)
        val result = apiCall(gson, { service.clockIn(body) }) { it }
        return OperationResult.from(result) { it.httpStatus == 409 }    // 409 = ya existe la jornada → aplicada
    }
}
```

`OperationResult.from` aplica la tabla de respuestas del algoritmo (2xx → `Applied`, 401 → `Unauthorized`, 5xx/408/429/sin red → `Retry`, otro 4xx → `Failed`). Cada handler solo indica qué errores cuentan como "ya aplicada". Para encolar se usa `PendingOperationQueue.enqueue(type, payload, dedupeKey, dependsOn)`, que completa `user_id`/`tenant_id` desde la sesión y dispara el sync. Un `type` sin handler registrado queda `PENDING` (no se pierde).

### Algoritmo del `SyncWorker`

```text
1. Restricción: NetworkType.CONNECTED. Trabajo único "atelier-sync" (ExistingWorkPolicy.KEEP).
2. Si no hay sesión o el token no se puede renovar → detener (las operaciones quedan PENDING).
3. Tomar operaciones PENDING del usuario actual con next_attempt_at <= ahora, ordenadas por created_at.
   Saltar las que tienen depends_on sin completar.
4. Por cada una: marcar SYNCING → llamar al endpoint → según la respuesta:
     2xx                         → borrar la operación (y su archivo local si era la última que lo usa)
     409/400/404/422 "ya aplicada" (tabla de tipos) → borrar
     401                         → el TokenAuthenticator renueva; si falla, PENDING y detener
     otro 4xx (regla de negocio) → FAILED con code y message (no se reintenta solo)
     5xx, timeout, sin red       → PENDING, attempts+1, next_attempt_at = ahora + min(2^attempts, 60) min
5. Si attempts llega a 8 → FAILED ("No se pudo sincronizar").
6. Al terminar, refrescar las cachés afectadas (asistencia de hoy, tarea, fallas del vehículo).
```

Disparadores: al encolar algo, al recuperar red, al volver la app a primer plano, y uno periódico cada 15 min (`PeriodicWorkRequest`). Tras el backoff, `SyncScheduler.requestSyncAt` programa otro trabajo único (`atelier-sync-retry`) para el próximo `next_attempt_at`, así un `KEEP` no bloquea el envío inmediato. Al empezar, el worker devuelve a `PENDING` lo que quedó en `SYNCING` si el proceso murió a mitad de un envío.

### Reglas de producto

- **Mostrar siempre lo pendiente.** Un ícono en la barra superior con el número de operaciones `PENDING`/`FAILED` y una pantalla "Pendientes de sincronizar" con el `message` del backend y las acciones *Reintentar* y *Descartar*.
- **Cambio de usuario.** La cola es por `user_id`: al cerrar sesión con operaciones pendientes, avisar ("Tienes 3 marcaciones sin enviar"). Si se confirma, se conservan y se envían cuando ese mismo usuario vuelva a entrar. Nunca se envían con el token de otro usuario.
- **Sesión vencida sin red.** Si el refresh token (30 días) vence con operaciones pendientes, el login vuelve a mostrar el aviso; tras entrar, se envían.
- **Hora del dispositivo.** Se guarda en UTC (`Instant`) al capturar. Si el reloj del teléfono está desajustado más de 2 min hacia el futuro, el backend rechaza la marcación (400): mostrar "Revisa la hora de tu teléfono".
- **Marcaciones viejas.** Una marcación de ingreso con más de 12 h no se acepta (400): el colaborador debe enviar un descargo.

---

## 4. Cachés de lectura

Estrategia única: **red → Room → UI** (patrón offline-first del curso). Cada repositorio intenta la red, reemplaza las filas de su **alcance** y la UI observa Room con `Flow`. Como el backend no soporta deltas, se recarga el alcance completo; `cache_metadata` guarda cuándo.

### `cache_metadata`

| Columna | Tipo | Descripción |
| --- | --- | --- |
| `scope` | TEXT PK | Alcance cacheado, p. ej. `tasks:my`, `bays:{branchId}`, `appointments:{branchId}:{fecha}` |
| `last_synced_at` | INTEGER (epoch ms) | Última recarga exitosa |
| `record_count` | INTEGER | Filas del alcance |

La UI muestra "Actualizado hace N min" cuando la caché tiene más de 5 min y no hay red.

### Tablas y vigencia

| Tabla (Entity) | Fuente | Alcance y vigencia | Quién la usa |
| --- | --- | --- | --- |
| `branches` | `GET /branches` | Todo el taller · 24 h | Todos (geocerca) |
| `shifts` | `GET /hr/work-shifts/{id}` (mi turno) o `GET /hr/work-shifts` | Mi turno · 24 h | Todos |
| `attendances` | `GET /hr/attendances/employee/{membershipId}/history?startDate=&endDate=` + `/status-today` | Últimos 30 días · al abrir Asistencia | Todos |
| `tasks` | `GET /tasks/my` (+ `GET /tasks/{id}` al abrir) | Mis tareas abiertas · al abrir y tras cada acción | Mecánico, jefe |
| `work_orders` | `GET /work-orders?branchId=` y `GET /work-orders/{id}` | Órdenes no cerradas de la sede · 5 min | Asesor, jefe, admin |
| `bays` | `GET /work-bays?branchId=` | Sede · 30 s con pantalla visible | Jefe, admin |
| `parts` | `GET /inventory/items?page=&size=100`, página por página hasta recibir menos de 100 (el backend pagina en memoria y no da el total) | Catálogo · 1 h | Mecánico, almacén |
| `batches` | `GET /inventory/items/{id}/batches` (todos los lotes, también agotados; la app los ordena FIFO por `arrival_date`) | Por repuesto consultado · 1 h | Almacén, admin |
| `customers` | `GET /customers?search=` y `GET /customers/{id}` | Clientes vistos o buscados · 7 días | Recepción, asesor |
| `vehicles` | `GET /vehicles/by-plate/{plate}`, `GET /vehicles/{id}` | Vehículos vistos · 7 días | Recepción, mecánico |
| `appointments` | `GET /appointments?branchId=&date=` | Hoy y mañana · 5 min | Recepción, asesor |
| `vouchers` | `GET /invoicing/vouchers/work-order/{workOrderId}` | Por orden consultada · al abrir | Caja, asesor |
| `vehicle_faults` | `GET /iot/faults/vehicle/{id}/active` | Por vehículo en diagnóstico · al abrir | Mecánico |
| `predictive_alerts` | `GET /iot/alerts/vehicle/{id}` | Por vehículo · al abrir | Mecánico, jefe |

Al cerrar sesión se borran **todas** las cachés (son datos del taller) y se conserva solo la cola del usuario si eligió guardarla.

---

## 5. Esquema Room (Entities)

Convención: tablas y columnas en `snake_case`, ids como `String` (UUID), instantes como `Long` (epoch ms UTC), fechas `LocalDate` como `String` ISO, dinero como `String` decimal (evita errores de `Double`; se convierte a `BigDecimal` en el dominio). Todos los enums se guardan como el `String` del backend.

```kotlin
// core/data/sync/PendingOperationEntity.kt
@Entity(
    tableName = "pending_operations",
    indices = [Index(value = ["dedupe_key"], unique = true), Index(value = ["status", "next_attempt_at"]), Index("user_id")]
)
data class PendingOperationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payload: String,
    @ColumnInfo(name = "dedupe_key") val dedupeKey: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "tenant_id") val tenantId: String,
    val status: String = "PENDING",
    val attempts: Int = 0,
    @ColumnInfo(name = "next_attempt_at") val nextAttemptAt: Long,
    @ColumnInfo(name = "last_http_status") val lastHttpStatus: Int? = null,
    @ColumnInfo(name = "last_error_code") val lastErrorCode: String? = null,
    @ColumnInfo(name = "last_error_message") val lastErrorMessage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "depends_on") val dependsOn: String? = null
)

// core/data/sync/PendingUploadEntity.kt
@Entity(tableName = "pending_uploads")
data class PendingUploadEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "local_file_path") val localFilePath: String,     // filesDir/pending/{id}.jpg
    @ColumnInfo(name = "storage_path") val storagePath: String,          // {tenantId}/work-orders/{woId}/tasks/{taskId}/{id}.jpg
    val target: String,                                                  // INTAKE | TASK_EVIDENCE | PROPOSAL
    @ColumnInfo(name = "public_url") val publicUrl: String? = null,      // se llena al subir
    @ColumnInfo(name = "created_at") val createdAt: Long
)

// core/data/sync/CacheMetadataEntity.kt
@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(
    @PrimaryKey val scope: String,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long,
    @ColumnInfo(name = "record_count") val recordCount: Int
)
```

```kotlin
// features/attendance/infrastructure/local/AttendanceEntity.kt
@Entity(tableName = "attendances", indices = [Index(value = ["membership_id", "clock_in"])])
data class AttendanceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "membership_id") val membershipId: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    @ColumnInfo(name = "clock_in") val clockIn: Long,
    @ColumnInfo(name = "clock_out") val clockOut: Long?,
    val status: String,                                                  // ON_TIME | LATE | EXCUSED | ABSENT
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "distance_to_branch_m") val distanceToBranchMeters: Double,
    @ColumnInfo(name = "worked_minutes") val workedMinutes: Long?,
    @ColumnInfo(name = "regular_minutes") val regularMinutes: Long?,
    @ColumnInfo(name = "overtime_minutes") val overtimeMinutes: Long?,
    @ColumnInfo(name = "justification_reason") val justificationReason: String?,
    @ColumnInfo(name = "justification_status") val justificationStatus: String?,   // PENDING | APPROVED | REJECTED
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

// features/attendance/infrastructure/local/ShiftEntity.kt
@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "start_time") val startTime: String,              // "08:00:00" hora de Lima
    @ColumnInfo(name = "end_time") val endTime: String,
    @ColumnInfo(name = "grace_period_minutes") val gracePeriodMinutes: Int,
    @ColumnInfo(name = "work_days") val workDays: String,                // "MONDAY,TUESDAY,…"
    @ColumnInfo(name = "rest_days") val restDays: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

// features/attendance/infrastructure/local/BranchEntity.kt
@Entity(tableName = "branches")
data class BranchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "geofence_radius_m") val geofenceRadiusMeters: Double,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
```

```kotlin
// features/tasks/infrastructure/local/TaskEntity.kt
@Entity(tableName = "tasks", indices = [Index("work_order_id")])
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "work_order_id") val workOrderId: String,
    @ColumnInfo(name = "work_order_number") val workOrderNumber: Int?,
    @ColumnInfo(name = "work_order_status") val workOrderStatus: String?,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "branch_id") val branchId: String?,
    @ColumnInfo(name = "bay_id") val bayId: String?,
    @ColumnInfo(name = "service_name") val serviceName: String?,          // del detalle
    @ColumnInfo(name = "mechanic_id") val mechanicId: String?,
    val status: String,                                                  // ASSIGNED | IN_PROGRESS | ON_HOLD | COMPLETED | …
    val description: String,
    @ColumnInfo(name = "estimated_hours") val estimatedHours: String?,
    @ColumnInfo(name = "hold_reason") val holdReason: String?,           // BREAK | END_OF_SHIFT | DEFECTIVE_PART | …
    @ColumnInfo(name = "missing_item_description") val missingItemDescription: String?,
    @ColumnInfo(name = "pending_suspension_reason") val pendingSuspensionReason: String?,
    @ColumnInfo(name = "started_at") val startedAt: Long?,
    @ColumnInfo(name = "total_paused_seconds") val totalPausedSeconds: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

// features/workorders/infrastructure/local/WorkOrderEntity.kt
@Entity(tableName = "work_orders", indices = [Index(value = ["branch_id", "status"])])
data class WorkOrderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "internal_number") val internalNumber: Int?,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,              // placa y modelo: tabla vehicles
    @ColumnInfo(name = "customer_id") val customerId: String,            // nombre: tabla customers
    @ColumnInfo(name = "current_bay_id") val currentBayId: String?,
    @ColumnInfo(name = "bay_name") val bayName: String?,
    val status: String,                                                  // DRAFT | IN_PROGRESS | COMPLETED | PAID | CANCELED
    @ColumnInfo(name = "diagnostic_summary") val diagnosticSummary: String?,
    @ColumnInfo(name = "mileage_in") val mileageIn: Int?,
    val subtotal: String?,
    @ColumnInfo(name = "tax_amount") val taxAmount: String?,
    @ColumnInfo(name = "total_amount") val totalAmount: String,
    val currency: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

// features/bays/infrastructure/local/BayEntity.kt
@Entity(tableName = "bays", indices = [Index("branch_id")])
data class BayEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    val name: String,
    @ColumnInfo(name = "bay_type") val bayType: String,                  // LIFT | PAINT_BOOTH | WASHING | ALIGNMENT
    val status: String,                                                  // AVAILABLE | OCCUPIED | MAINTENANCE
    @ColumnInfo(name = "current_work_order_id") val currentWorkOrderId: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
```

```kotlin
// features/reception/infrastructure/local/CustomerEntity.kt
@Entity(tableName = "customers", indices = [Index("tax_id")])
data class CustomerEntity(
    @PrimaryKey val id: String,
    val type: String,                                                    // INDIVIDUAL | COMPANY
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "tax_id") val taxId: String?,
    val email: String?,
    val phone: String?,
    val status: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

// features/reception/infrastructure/local/VehicleEntity.kt
@Entity(tableName = "vehicles", indices = [Index(value = ["plate"], unique = true)])
data class VehicleEntity(
    @PrimaryKey val id: String,
    val plate: String,
    val vin: String?,
    val brand: String,
    val model: String,
    val year: Int?,
    @ColumnInfo(name = "engine_type") val engineType: String?,
    @ColumnInfo(name = "current_owner_id") val currentOwnerId: String?,
    @ColumnInfo(name = "current_mileage") val currentMileage: Int?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

// features/reception/infrastructure/local/AppointmentEntity.kt
@Entity(tableName = "appointments", indices = [Index(value = ["branch_id", "scheduled_at"])])
data class AppointmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "customer_id") val customerId: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "scheduled_at") val scheduledAt: Long,
    @ColumnInfo(name = "estimated_duration_minutes") val estimatedDurationMinutes: Int?,
    val reason: String?,
    val status: String,                                                  // PENDING | CONFIRMED | ARRIVED | CANCELED
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
```

```kotlin
// features/inventory/infrastructure/local/PartEntity.kt
@Entity(tableName = "parts", indices = [Index("sku"), Index("category")])
data class PartEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val category: String,
    @ColumnInfo(name = "base_price") val basePrice: String,
    @ColumnInfo(name = "total_stock") val totalStock: String,
    @ColumnInfo(name = "minimum_stock") val minimumStock: String,
    @ColumnInfo(name = "unit_of_measure") val unitOfMeasure: String,
    val status: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

// features/inventory/infrastructure/local/BatchEntity.kt  (se llena solo con inventory:batches:read)
@Entity(tableName = "batches", indices = [Index("item_id")])
data class BatchEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "batch_number") val batchNumber: String,
    @ColumnInfo(name = "remaining_qty") val remainingQty: String,
    @ColumnInfo(name = "unit_cost") val unitCost: String,
    @ColumnInfo(name = "arrival_date") val arrivalDate: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

// features/cashier/infrastructure/local/VoucherEntity.kt
@Entity(tableName = "vouchers", indices = [Index("work_order_id")])
data class VoucherEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "work_order_id") val workOrderId: String?,
    @ColumnInfo(name = "voucher_type") val voucherType: String,          // 01 FACTURA | 03 BOLETA | 07 NOTA_CREDITO
    val serie: String,
    val number: Int,
    @ColumnInfo(name = "total_amount") val totalAmount: String,
    @ColumnInfo(name = "paid_amount") val paidAmount: String,
    @ColumnInfo(name = "pending_balance") val pendingBalance: String,
    @ColumnInfo(name = "payment_status") val paymentStatus: String,      // PENDING | PARTIAL | PAID
    val status: String,                                                  // DRAFT | ISSUED | ACCEPTED_SUNAT | REJECTED_SUNAT | VOIDED
    @ColumnInfo(name = "pdf_url") val pdfUrl: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
```

```kotlin
// features/diagnostics/infrastructure/local/TelemetryReadingEntity.kt
@Entity(tableName = "telemetry_readings", indices = [Index(value = ["vehicle_id", "timestamp"], unique = true)])
data class TelemetryReadingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    val timestamp: Long,                                                 // momento de la lectura (UTC)
    @ColumnInfo(name = "engine_temp_celsius") val engineTempCelsius: Double,   // obligatorio en el backend
    @ColumnInfo(name = "engine_rpm") val engineRpm: Int?,
    @ColumnInfo(name = "speed_kmh") val speedKmh: Int?,
    @ColumnInfo(name = "battery_voltage") val batteryVoltage: Double?,
    @ColumnInfo(name = "fuel_percentage") val fuelPercentage: Double?,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "batch_operation_id") val batchOperationId: String? = null  // id del TELEMETRY_BATCH que la lleva
)

// features/diagnostics/infrastructure/local/VehicleFaultEntity.kt
@Entity(tableName = "vehicle_faults", indices = [Index(value = ["vehicle_id", "dtc_code"])])
data class VehicleFaultEntity(
    @PrimaryKey val id: String,                                          // id del backend, o "local-{uuid}" si aún no se envió
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "dtc_code") val dtcCode: String,
    val severity: String,                                                // LOW | MEDIUM | CRITICAL
    val description: String,
    @ColumnInfo(name = "detected_at") val detectedAt: Long,
    @ColumnInfo(name = "is_resolved") val isResolved: Boolean,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?
)

// features/diagnostics/infrastructure/local/PredictiveAlertEntity.kt
@Entity(tableName = "predictive_alerts", indices = [Index(value = ["vehicle_id", "created_at"])])
data class PredictiveAlertEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "alert_type") val alertType: String,              // ENGINE_OVERHEATING_RISK | BATTERY_FAILURE_RISK | …
    @ColumnInfo(name = "confidence_score") val confidenceScore: Double,
    val message: String,
    val status: String,                                                  // DISPATCHED | ACKNOWLEDGED | RESOLVED | DISMISSED
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)

```

```kotlin
// core/data/local/AppDatabase.kt
@Database(
    entities = [
        PendingOperationEntity::class, PendingUploadEntity::class, CacheMetadataEntity::class,
        BranchEntity::class, ShiftEntity::class, AttendanceEntity::class,
        TaskEntity::class, WorkOrderEntity::class, BayEntity::class,
        CustomerEntity::class, VehicleEntity::class, AppointmentEntity::class,
        PartEntity::class, BatchEntity::class, VoucherEntity::class,
        TelemetryReadingEntity::class, VehicleFaultEntity::class, PredictiveAlertEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun pendingUploadDao(): PendingUploadDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    // … un DAO por Entity
}
```

`exportSchema = true` (a diferencia del ejemplo del curso) para versionar el esquema y escribir migraciones cuando cambie el backend.

### Sesión en DataStore (`SessionManager`)

Reemplaza `auth_session` del informe. Claves:

| Clave | Origen | Nota |
| --- | --- | --- |
| `token`, `refresh_token` | `AuthenticatedUserResource` | El refresh rota en cada renovación |
| `token_expires_at` | claim `exp` del JWT | Para renovar antes de vencer |
| `user_id`, `email`, `full_name` | sesión | |
| `tenant_id`, `tenant_name` | `activeTenant` | |
| `membership_id` | sesión | Lo piden los endpoints propios de HR |
| `permissions` | sesión (`Set<String>`) | Incluye `ROLE_*` |
| `branch_id`, `shift_id` | `GET /hr/employees/membership/{membershipId}` | Sede y turno asignados |
| `plan_tier`, `plan_status`, `plan_access_granted`, `iot_telemetry_enabled`, `ai_diagnostics_enabled` | `plan` de la sesión | Ocultar diagnóstico OBD-II e informe IA si el plan no los incluye; aviso si `accessGranted` es falso. Se actualiza en cada `refresh` |

No se guarda avatar (el backend no lo tiene) ni el radio de geocerca (está en `branches`).

---

## 6. Flujos offline por historia

### US06–US08 — Marcación sin cobertura

```text
Tocar "Marcar ingreso"
 → Location fresca (alta precisión). Sin fix → bloquear y pedir activar la ubicación
 → Calcular distancia con branches; si está fuera, avisar (el backend decide igual)
 → Con red: POST /hr/attendances/clock-in directo
 → Sin red: encolar CLOCK_IN { branchId, shiftId, latitude, longitude, clockInTime = ahora }
            dedupe_key = CLOCK_IN:{membershipId}:{fecha Lima}
            UI: "Marcación guardada · se enviará al recuperar señal"
 → Al sincronizar: 201 → refrescar attendances; 422 fuera de geocerca → FAILED con la distancia; 409 → ya marcada
```

La salida igual, con `CLOCK_OUT { clockOutTime }`. Si el ingreso de ese día aún está en cola, la salida se encola con `depends_on` = id del ingreso.

### US13, US14, US28 — Fotos sin señal

```text
CameraX → comprimir (JPEG 1600 px, 80 %) → guardar en filesDir/pending/{uuid}.jpg
 → pending_uploads { storage_path = {tenantId}/work-orders/{woId}/{intake|tasks/{taskId}|proposals}/{uuid}.jpg }
 → pending_operations UPLOAD_FILE { uploadId } + ATTACH_… { imageUrl = publicUrlFor(storage_path) } (depends_on = UPLOAD_FILE)
 → La galería de la tarea muestra la foto local con un ícono de "pendiente"
 → Al subir: la foto ya está en Supabase → se borran el archivo local y la fila de pending_uploads; luego se envía ATTACH_…
```

La propuesta adicional (US14) puede **capturarse** sin red, pero su envío y la notificación al cliente ocurren al sincronizar.

### US16–US19 — OBD-II en foso

```text
Lecturas en vivo → telemetry_readings (una fila por muestra, unique vehicle_id + timestamp)
 → Cada 200 lecturas o al cerrar la sesión de diagnóstico: crear TELEMETRY_BATCH con esas filas
 → DTC leídos (modo 03) → vehicle_faults con id "local-{uuid}" + REGISTER_FAULT
      dedupe_key = FAULT:{vehicleId}:{dtcCode}:{sessionId}   (un mismo código no se encola dos veces por sesión)
 → Al confirmar REGISTER_FAULT: reemplazar la fila local por la del backend (id real)
 → Borrado de fallas (modo 04) sin red: RESOLVE_FAULT solo para fallas con id real; las locales se descartan
```

Las lecturas confirmadas se borran a los 7 días (la app no es el histórico; el backend sí).

---

## 7. Requisitos del backend para que la cola sea segura

| # | Cambio | Para qué | Estado |
| --- | --- | --- | --- |
| R1 | `clockInTime` en el ingreso | Marcación de ingreso sin red (US06) | ✅ 1.0.3 |
| R2 | Ignorar lecturas de telemetría repetidas | Reintentos de lotes | ✅ Ya existía |
| R3 | `POST /iot/faults`: si el vehículo ya tiene una falla **activa** con el mismo DTC, devolver esa en vez de crear otra | Reintentar `REGISTER_FAULT` o volver a escanear sin duplicar fallas | ✅ 1.1.0 |
| R4 | `intake-images`, `evidence-images` y `proposals`: si ya existe un registro con la misma `imageUrl` en esa orden o tarea, devolverlo | Reintentar `ATTACH_*` sin fotos ni propuestas duplicadas | ✅ 1.1.0 |

Con R3 y R4, todas las operaciones de la cola se pueden reintentar sin duplicar registros. La ruta de cada foto debe llevar un UUID generado al capturarla (§3): dos fotos distintas nunca comparten URL, y el reintento de la misma siempre la repite.

> R3 y R4 se publicaron en `atelier-platform` 1.1.0 (commit `cdcdc6f`) y siguen en 1.2.0. El reintento devuelve el registro ya guardado y no emite un segundo evento ni push.

---

## 8. Checklist para implementar

1. `AppDatabase` con las Entities del §5 y `exportSchema = true`.
2. `SessionManager` (DataStore) con las claves del §5.
3. `PendingOperationDao` con `enqueue` (respeta `dedupe_key`), `nextBatch(userId, now)`, `markFailed`, `delete`, y un `Flow<Int>` de pendientes para la UI.
4. `SyncWorker` (`@HiltWorker`) con el algoritmo del §3 y un `SyncScheduler` que lo dispare (al encolar, al haber red, en primer plano y cada 15 min).
5. `ConnectivityObserver` (`ConnectivityManager.NetworkCallback`) expuesto como `StateFlow<Boolean>` para deshabilitar las acciones en línea.
6. Repositorios offline-first por feature (§4) y casos de uso que encolan (§6).
7. Pantalla "Pendientes de sincronizar" e ícono con contador.
8. Limpieza al cerrar sesión (cachés) y aviso si hay operaciones pendientes.
9. Tests: DAO en memoria (`Room.inMemoryDatabaseBuilder`), `SyncWorker` con `TestListenableWorkerBuilder` y respuestas simuladas (2xx, 409 "ya aplicada", 422, 5xx, timeout).
