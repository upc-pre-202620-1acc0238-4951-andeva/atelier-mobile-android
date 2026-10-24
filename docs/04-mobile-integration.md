# Guía de integración móvil

Todo lo que la app Android necesita de servicios externos además de la API REST: configuración, notificaciones push, fotos, OBD-II, trabajo sin conexión, permisos de Android y cuentas de prueba.

---

## 1. Configuración del proyecto

Ningún secreto va al repositorio. Los valores se leen de `local.properties` (ignorado por git) y se exponen como `BuildConfig`:

```properties
# local.properties (NO versionar)
API_BASE_URL=https://<servicio>.onrender.com/
GOOGLE_WEB_CLIENT_ID=<id>.apps.googleusercontent.com
SUPABASE_URL=https://<proyecto>.supabase.co
SUPABASE_ANON_KEY=<anon key pública del proyecto>
SUPABASE_BUCKET=atelier
```

| Valor | De dónde sale | Para qué |
| --- | --- | --- |
| `API_BASE_URL` | URL del servicio web en Render (termina en `/`) | Retrofit `baseUrl` |
| `GOOGLE_WEB_CLIENT_ID` | Google Cloud Console → Credenciales → cliente OAuth **Web** (el mismo que el backend acepta en `GOOGLE_CLIENT_ID`) | `serverClientId` de Credential Manager |
| Cliente OAuth **Android** | Google Cloud Console → cliente Android con el package `com.andeva.atelier.mobile` y el SHA-1 de la llave debug/release | Necesario para que Google entregue el `idToken` a esta app. No se configura en el backend: el `aud` del `idToken` es el **Web client ID**, que es el que debe estar en `GOOGLE_CLIENT_ID` |
| `google-services.json` | Firebase Console → proyecto `atelier-platform-ee4f5` → app Android `com.andeva.atelier.mobile` | FCM. Va en `app/` y **no se versiona** |
| `SUPABASE_URL` / `SUPABASE_ANON_KEY` | Supabase → Project Settings → API | Subida directa de fotos |
| `SUPABASE_BUCKET` | Bucket `atelier` (mismo que `SUPABASE_STORAGE_BUCKET` del backend) | Ruta de las fotos |

```kotlin
// app/build.gradle.kts (fragmento)
import java.util.Properties   // con AGP 9, `java.util` sin import choca con la extensión `java`

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
android {
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"${localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8080/")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProps.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "SUPABASE_BUCKET", "\"${localProps.getProperty("SUPABASE_BUCKET", "atelier")}\"")
    }
    buildFeatures { buildConfig = true }
}
```

---

## 2. Notificaciones push (FCM)

1. Al iniciar sesión: obtener el token con `FirebaseMessaging.getInstance().token` y enviarlo con `PUT /api/v1/users/me/device-token { "fcmToken": "…" }`.
2. En `onNewToken` del `FirebaseMessagingService`: reenviarlo si hay sesión.
3. Al cerrar sesión: `PUT /api/v1/users/me/device-token { "fcmToken": null }` y luego `POST /api/v1/auth/sign-out`.

El backend envía mensajes con `notification` (título y cuerpo en español) y `data`. La app enruta según `data.type`:

| `data.type` | Quién lo recibe | `data` extra | Pantalla a abrir |
| --- | --- | --- | --- |
| `TASK_ASSIGNED` | El mecánico asignado | `workOrderId`, `taskId` | Detalle de la tarea |
| `TASK_PAUSED` | Quienes tienen `operations:tasks:hold_validate` | `workOrderId`, `taskId` | Detalle de la tarea (supervisión) |
| `TASK_SUSPENSION_REQUESTED` | Quienes tienen `operations:tasks:hold_validate` | `workOrderId`, `taskId` | Aprobar / rechazar suspensión |
| `TASK_PART_CHARGED` | Quienes tienen `inventory:batches:restore` | `workOrderId`, `taskId` | Productos de la tarea (revisar imputación) |
| `TASK_RESUMED` | Supervisores (`operations:tasks:hold_validate`) tras una pausa; el mecánico cuando se levanta su suspensión | `workOrderId`, `taskId` | Detalle de la tarea |
| `WORK_ORDER_COMPLETED` | Quienes tienen `operations:vehicle_handover:execute` (asesor) | `workOrderId` | Detalle de la orden (control de calidad y cobro) |
| `LOW_STOCK` | Quienes tienen `inventory:alerts:read` | `itemId`, `currentStock`, `minimumStock` | Alertas de stock (se repite en cada descargo bajo el mínimo, H14 de [03](03-traceability.md#huecos-abiertos)) |
| `OVERTIME_RECORDED` | Quienes tienen `hr:payroll:manage` | `attendanceId`, `membershipId`, `overtimeMinutes` | Asistencia del colaborador |
| `JUSTIFICATION_REVIEWED` | El colaborador que envió el descargo | `attendanceId`, `verdict` (`APPROVED`/`REJECTED`) | Mi asistencia |
| `SHIFT_REMINDER` | Colaboradores con turno que empieza en 15 min | `shiftId`, `startTime` | Marcar ingreso |

Reglas:

- Crear un canal de notificación (`NotificationChannel`) por grupo: **Tareas**, **Asistencia**, **Inventario**.
- Android 13+ requiere pedir `POST_NOTIFICATIONS` en tiempo de ejecución (tras el login, con explicación previa).
- El push es un aviso: la pantalla siempre recarga los datos desde la API (no confiar en el contenido del push).
- Un `data.type` desconocido se ignora sin fallar (el backend puede agregar tipos nuevos).

---

## 3. Fotos y evidencias (Supabase Storage)

El backend no recibe archivos: la app sube la foto a Supabase y envía la URL.

```text
CameraX / Photo Picker → comprimir (JPEG, lado mayor 1600 px, calidad 80)
  → POST {SUPABASE_URL}/storage/v1/object/{bucket}/{ruta}     (Authorization: Bearer {SUPABASE_ANON_KEY})
  → URL pública: {SUPABASE_URL}/storage/v1/object/public/{bucket}/{ruta}
  → POST /api/v1/work-orders/{id}/intake-images  |  /tasks/{id}/evidence-images  |  /work-orders/{id}/proposals
```

| Uso | Ruta sugerida en el bucket |
| --- | --- |
| Recepción (US13) | `{tenantId}/work-orders/{workOrderId}/intake/{uuid}.jpg` |
| Desmontaje / montaje (US28) | `{tenantId}/work-orders/{workOrderId}/tasks/{taskId}/{uuid}.jpg` |
| Propuesta adicional (US14) | `{tenantId}/work-orders/{workOrderId}/proposals/{uuid}.jpg` |
| Comprobante de compra recibida (US34 E2) | `{tenantId}/purchase-orders/{purchaseOrderId}/receipt/{uuid}.jpg`. Se sube **en línea** justo antes de `PUT /inventory/purchase-orders/{id}/receive`, no por la cola |

- El backend valida que la URL sea `https`, del host de Supabase configurado y que contenga `/storage/v1/object/` y `/atelier/`. Cualquier otra URL → 400.
- El bucket debe permitir `INSERT` con la anon key (política de Storage) y lectura pública.
- Si no hay red, la foto se guarda en el almacenamiento interno y un `Worker` la sube después (ver sección 5).
- Las fotos se muestran con Coil 3 a partir de la URL pública.

---

## 4. OBD-II (ELM327 por Bluetooth)

La lectura del vehículo ocurre **en la app**; el backend solo recibe los resultados.

| Paso | Comandos ELM327 / OBD-II | Endpoint |
| --- | --- | --- |
| Emparejar y abrir socket SPP (UUID `00001101-0000-1000-8000-00805F9B34FB`) | `ATZ`, `ATE0`, `ATL0`, `ATS0`, `ATH0`, `ATSP0`; luego `0100` (si no contesta, la ECU no responde: US16 E2), `ATDPN` (protocolo) y `0902` (VIN, opcional) | `GET /iot/installations/vehicle/{vehicleId}/active` (o `POST /iot/installations/install`) |
| Leer DTC (US17) | Modo `03` → decodificar a `P0xxx` / `Cxxxx` / `Bxxxx` / `Uxxxx` (`NO DATA` = sin códigos); cuadro congelado con modo `02` (`0202`, `020500`, `020C00`, `020D00`), solo se muestra | `POST /iot/faults` por código (`severity`: `LOW`, `MEDIUM` o `CRITICAL`, de `DtcCatalog`: H20 de [03](03-traceability.md#huecos-abiertos)) |
| Telemetría en vivo (US18) | Modo `01`: `010C` RPM, `010D` velocidad, `0105` temperatura refrigerante, `012F` combustible, `010B` presión de admisión (solo se muestra: el lote no tiene ese campo), `ATRV` voltaje | Se encola localmente |
| Enviar lote (US19) | — | `POST /iot/telemetry/batch { vehicleId, readings[] }` |
| Borrar fallas (US20) | Modo `04` y luego `03` para comprobar | `POST /iot/faults/{id}/resolve` por cada DTC que ya no aparece |
| Informe IA (US21) | — | `POST /iot/health-reports/generate?vehicleId=` |

Cada lectura del lote:

```json
{ "timestamp": "2026-10-20T15:04:05Z", "engineRpm": 820, "speedKmh": 0,
  "engineTempCelsius": 88.5, "batteryVoltage": 13.9, "fuelPercentage": 61.0,
  "latitude": -12.0464, "longitude": -77.0428 }
```

- `engineTempCelsius` y `timestamp` son obligatorios; el resto es opcional.
- Lecturas repetidas (mismo vehículo y mismo `timestamp`) se ignoran, así que reintentar un lote es seguro.
- Agrupar en lotes de hasta ~200 lecturas; muestreo sugerido 1 lectura cada 2 s en vivo.
- El informe IA necesita telemetría suficiente en el período (`daysToAnalyze` 7–90); si no, 422 `ERR_INSUFFICIENT_TELEMETRY`.
- Solo el rol con `iot:telemetry:ingest` (por defecto, el mecánico) puede subir telemetría y DTC.
- Durante una sesión de lectura larga, usar un **foreground service** de tipo `connectedDevice` para que Android no corte el socket.

---

## 5. Trabajo sin conexión (offline-first)

Patrón del curso: el repositorio intenta la red, guarda en Room y **la UI siempre lee de Room**. Las escrituras que no pueden fallar se encolan. El diseño completo (tablas, cola, idempotencia por endpoint y flujos) está en [06-offline-sync.md](06-offline-sync.md); esta sección es el resumen.

| Dato | Lectura offline (caché Room) | Escritura offline (cola + WorkManager) |
| --- | --- | --- |
| Mis tareas, detalle de orden, catálogo de repuestos, turno, sedes | Sí | — |
| Lecturas de telemetría y DTC | — | Sí (`POST /iot/telemetry/batch`, `POST /iot/faults`) |
| Fotos de evidencia | — | Sí (subida a Supabase + registro de la URL) |
| Marcación de salida | — | Sí, enviando `clockOutTime` capturado en el dispositivo |
| Marcación de ingreso | — | Sí, enviando `clockInTime` capturado en el dispositivo junto con las coordenadas. El backend lo acepta hasta 12 h después (más tarde → 400: avisar al colaborador para que envíe un descargo) |
| Iniciar / pausar / cerrar tarea, imputar repuestos, cobrar | — | No: requieren respuesta inmediata del backend (stock, reglas de turno) |

Reglas de la cola:

- Una tabla `pending_operations` (tipo, payload JSON, intentos, creado en) y un `CoroutineWorker` único con restricción `NetworkType.CONNECTED` y backoff exponencial.
- Cada operación debe ser **idempotente** o tolerar duplicados (la telemetría ya lo es).
- Antes de enviar, renovar el token si expiró.

---

## 6. Permisos de Android

| Permiso | Para qué | Cuándo pedirlo |
| --- | --- | --- |
| `INTERNET`, `ACCESS_NETWORK_STATE` | API, Supabase, estado de red | Manifiesto (normales) |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Geocerca de asistencia (US06–US08) | Al tocar "Marcar ingreso" |
| `CAMERA` | Fotos de recepción y evidencias | Al abrir la cámara |
| `POST_NOTIFICATIONS` (API 33+) | Push | Tras el primer login |
| `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (API 31+, `neverForLocation`) | Escáner ELM327 | Al abrir el diagnóstico |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` (`maxSdkVersion="30"`) | Escáner en Android 11 o menor | Manifiesto |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Sesión OBD-II en vivo | Manifiesto |

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />
<uses-feature android:name="android.hardware.location.gps" android:required="false" />
```

Para galería se usa el **Photo Picker** (`PickVisualMedia`), que no necesita permiso de almacenamiento.

---

## 7. Geocerca y asistencia

- Pedir una ubicación **fresca** con `FusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, …)`; no usar la última conocida.
- Si la precisión (`accuracy`) es peor que 50 m, pedir al usuario que salga a cielo abierto antes de marcar.
- La sede y el turno salen de `GET /hr/employees/membership/{membershipId}` (`branchId`, `assignedShiftId`); la geocerca, de `GET /branches/{id}` (`latitude`, `longitude`, `geofenceRadiusMeters`). La app puede mostrar la distancia antes de enviar, pero **la validación real la hace el backend** (422 si está fuera).
- Detectar ubicación simulada (`Location.isMock`, API 31+) y advertir; el backend no lo verifica.

---

## 8. Descarga de PDFs

```text
GET (con Authorization) → guardar en cacheDir/pdfs/{nombre}.pdf
  → FileProvider.getUriForFile(...) → Intent(ACTION_VIEW, "application/pdf") con FLAG_GRANT_READ_URI_PERMISSION
```

Para compartir (WhatsApp, correo) usar `ACTION_SEND` con el mismo URI. El nombre del archivo viene en `Content-Disposition`.

---

## 9. Cuentas de prueba

No se versionan credenciales. Para el entorno local:

1. Registrar un taller con `POST /auth/sign-up` usando un alias de correo propio (por ejemplo `tu.correo+dueno@gmail.com`) y verificar el OTP que llega al correo.
2. Como dueño, invitar a un colaborador por rol con `POST /invitations/tenant/{tenantId}` usando alias distintos (`+mecanico`, `+jefe`, `+asesor`, `+recepcion`, `+almacen`, `+caja`).
3. Aceptar cada invitación desde la app (`POST /invitations/accept`).
4. Guardar los correos y contraseñas en un archivo local ignorado por git (por ejemplo `test-accounts.local.md`), nunca en el repo.

Un taller recién registrado tiene prueba gratuita con las cuotas del plan elegido (si se elige un plan sin IA, los informes IA responden 403).
