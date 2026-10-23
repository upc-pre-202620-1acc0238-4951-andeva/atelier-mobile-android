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
| 1 | Núcleo `core/` | ✅ Hecho (sin merge a `develop`) | `feature/core` |
| 2 | `auth` — sesión y acceso | ⬜ Pendiente | `feature/auth` |
| 3 | `main` — contenedor con pestañas por permiso | ⬜ Pendiente | `feature/main` |
| 4 | `attendance` — asistencia y turnos | ⬜ Pendiente | `feature/attendance` |
| 5 | `tasks` — tareas del mecánico | ⬜ Pendiente | `feature/tasks` |
| 6 | `reception` + `workorders` — recepción y órdenes | ⬜ Pendiente | `feature/reception-workorders` |
| 7 | `inventory` — catálogo, lotes y alertas | ⬜ Pendiente | `feature/inventory` |
| 8 | `diagnostics` — OBD-II, DTC e informe IA | ⬜ Pendiente | `feature/diagnostics` |
| 9 | `bays`, `cashier`, `reports`, `team` | ⬜ Pendiente | una rama por feature |

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

## Paso 2 — `auth` ⬜

- **Historias:** US01–US05 ([01](01-requirements/user-stories.md), EP01) · **API:** [api/iam.md](api/iam.md) · **Trazabilidad:** [03](03-traceability.md) EP01
- **Incluye:** login con correo y contraseña, Google Sign-In (`POST /auth/google-sign-in`), verificación OTP y reenvío, aceptar invitación (`validate` → `accept`), olvidé/restablecer contraseña, cierre de sesión.
- **Tras el login:** `SessionManager.save(dto)` → `GET /hr/employees/membership/{membershipId}` → `updateAssignment(branchId, shiftId)` → `DeviceTokenRegistrar.registerCurrentToken()` → pedir `POST_NOTIFICATIONS` (API 33+).
- **Cierre de sesión:** avisar si hay operaciones pendientes ([06](06-offline-sync.md) §3, "Cambio de usuario") → `DeviceTokenRegistrar.clear()` → `POST /auth/sign-out` → `CacheMaintenanceDao.clearCaches()` → `SessionManager.clear()`.
- **Errores clave:** 401 credenciales · 403 `EMAIL_NOT_VERIFIED` → pantalla OTP · 409 invitación con membresía activa · 400 invitación vencida.
- **Sin red:** todo es en línea.
- **Reemplaza** `AuthStartRoute` en `AppNavHost` por el grafo real de `auth`.

## Paso 3 — `main` ⬜

- **Incluye:** `MainScreen` con `NavigationBar` armada con `NavigationItem.tabsFor(session)` ([05](05-android-architecture.md) §5.5), pestaña "Más", aviso si `plan.accessGranted == false`, ícono con el contador de pendientes (`PendingOperationDao.countPending`) y pantalla "Pendientes de sincronizar" con *Reintentar* / *Descartar* ([06](06-offline-sync.md) §3, "Reglas de producto").
- **Consume** `PushInbox` para navegar al destino del push cuando la feature correspondiente exista.
- **Reemplaza** `MainStartRoute` en `AppNavHost`.

## Paso 4 — `attendance` ⬜

- **Historias:** US06, US08, US09, US10 · **API:** [api/hr.md](api/hr.md) · [04](04-mobile-integration.md) §7 (geocerca)
- **Primera feature con cola offline:** `CLOCK_IN`, `CLOCK_OUT`, `SUBMIT_JUSTIFICATION` (handlers + `dedupe_key` de [06](06-offline-sync.md) §3 y §6).
- **Cachés:** `branches`, `shifts`, `attendances`.
- **Ubicación:** `LocationProvider` (interfaz en dominio, implementación con `FusedLocationProviderClient`), ubicación fresca, precisión ≤ 50 m, detectar ubicación simulada.

## Paso 5 — `tasks` ⬜

- **Historias:** US26–US30, US23 E1, US33, US35 · **API:** [api/operations.md](api/operations.md)
- **En línea:** iniciar, pausar, suspender, reanudar, cerrar e imputar repuestos.
- **Offline:** fotos de desmontaje y montaje (`UPLOAD_FILE` + `ATTACH_TASK_EVIDENCE`).
- **Caché:** `tasks`. **Push:** `TASK_ASSIGNED`, `TASK_PAUSED`, `TASK_RESUMED`, `TASK_SUSPENSION_REQUESTED`, `TASK_PART_CHARGED`.

## Paso 6 — `reception` + `workorders` ⬜

- **Historias:** US11, US44, US12–US15, US22, US24, US25, US36 · **API:** [api/crm.md](api/crm.md), [api/operations.md](api/operations.md)
- **Offline:** fotos de recepción (`ATTACH_INTAKE_IMAGE`) y propuestas (`SUBMIT_PROPOSAL`). El resto es en línea.
- **Cachés:** `customers`, `vehicles`, `appointments`, `work_orders`. **PDF:** proforma ([04](04-mobile-integration.md) §8).

## Paso 7 — `inventory` ⬜

- **Historias:** US31, US32, US34 · **API:** [api/inventory.md](api/inventory.md)
- **Cachés:** `parts`, `batches` (solo con `inventory:batches:read`). **Push:** `LOW_STOCK`.

## Paso 8 — `diagnostics` ⬜

- **Historias:** US16–US21, US43 · **API:** [api/iot.md](api/iot.md) · [04](04-mobile-integration.md) §4 (ELM327)
- **Offline:** `telemetry_readings` → `TELEMETRY_BATCH`, `REGISTER_FAULT`, `RESOLVE_FAULT` ([06](06-offline-sync.md) §6).
- **Visible solo si** `session.canUseTelemetry()` / `canUseAiDiagnostics()`. Foreground service `connectedDevice` en sesiones largas.

## Paso 9 — `bays`, `cashier`, `reports`, `team` ⬜

- **`bays`:** US40 · tablero con refresco cada 15–30 s mientras la pantalla está visible.
- **`cashier`:** US37–US39 · todo en línea (dinero y documentos fiscales); pase de salida en PDF.
- **`reports`:** US41, US43 E2 · PDFs de flujo de caja y de flota.
- **`team`:** US10 (revisar descargos) y asistencia diaria de la sede.

---

## Pendientes y riesgos conocidos

| Tema | Detalle | Afecta |
| --- | --- | --- |
| Prueba en emulador del paso 1 | No se pudo ejecutar (no había AVD) | Paso 1 |
| Configuración local | Faltan `API_BASE_URL`, `GOOGLE_WEB_CLIENT_ID`, `SUPABASE_*` en `local.properties` y `app/google-services.json` | Pasos 2, 5, 6 |
| Idempotencia R3/R4 del backend | Está en la rama `feature/offline-sync-idempotency` de `atelier-platform`, sin publicar ([06](06-offline-sync.md) §7). Hasta entonces, reintentar `ATTACH_*`, `SUBMIT_PROPOSAL` o `REGISTER_FAULT` puede duplicar registros | Pasos 5, 6, 8 |
| Tokens en DataStore sin cifrar | [api/README.md](api/README.md) §2 menciona "DataStore cifrado"; la guía usa DataStore normal | Decidir antes de producción |
| Pantallas sin diseño | Las pantallas se entregan funcionales; el estilo lo aplica la otra parte del equipo | Todos |
