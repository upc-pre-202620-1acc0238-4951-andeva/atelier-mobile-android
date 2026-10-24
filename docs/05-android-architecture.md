# Guía de arquitectura y estándares — Atelier Mobile (Android)

Adaptación de la guía del curso (*EasySneaker Mobile*) a la app de staff de Atelier. Se mantienen **la misma arquitectura, estructura de carpetas, nombres y patrones**; solo se agregan las piezas que esta app necesita (sesión con refresh, permisos RBAC, push, Bluetooth, cámara, ubicación y cola offline).

---

## 1. Visión general

**Clean Architecture** + organización **Feature-First** + conceptos tácticos de **DDD**, con UI en **Jetpack Compose** y **Material 3**.

```text
┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                         │
│   (Screens Compose, ViewModels Hilt, UiState, NavGraphs)        │
└───────────────────────────────┬─────────────────────────────────┘
                                │ usa
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                          │
│                   (UseCases con operator invoke)                │
└───────────────────────────────┬─────────────────────────────────┘
                                │ orquesta
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER                             │
│   (Entities, Value Objects, Repository Interfaces, Permission)  │
└───────────────────────────────▲─────────────────────────────────┘
                                │ implementa (DIP)
┌───────────────────────────────┴─────────────────────────────────┐
│                     INFRASTRUCTURE LAYER                        │
│  (Retrofit Services, DTOs, Room, DAOs, DataStore, Workers,      │
│   Bluetooth OBD, Supabase uploader, FCM, Hilt Modules)          │
└─────────────────────────────────────────────────────────────────┘
```

Regla de dependencia: `domain` no importa nada de Android, Retrofit, Room ni Hilt. Cada **feature** corresponde a un *bounded context* del backend (o a una parte de él), para que el lenguaje de la app y el del backend sean el mismo.

---

## 2. Dependencias (Gradle Kotlin DSL + Version Catalog)

Las versiones base son las de la guía del curso. Las librerías nuevas se marcan con ➕; antes de crear el proyecto, confirma en Android Studio (*Project Structure → Suggestions*) que no haya una versión estable más nueva.

### `gradle/libs.versions.toml`

```toml
[versions]
agp = "9.4.0"
kotlin = "2.4.20"
ksp = "2.3.4"
coreKtx = "1.19.0"
lifecycleRuntimeKtx = "2.11.0"
activityCompose = "1.13.0"
composeBom = "2026.09.00"
material3 = "1.4.0"
ui = "1.12.1"
navigationCompose = "2.9.5"            # ➕ navegación type-safe
kotlinSerialization = "2.4.20"
kotlinxSerializationJson = "1.9.0"      # ➕ rutas @Serializable
retrofit = "3.0.0"
okhttp = "4.12.0"                       # ➕ interceptor JWT + logging
coilCompose = "3.6.3"
hilt = "2.60.1"
hiltNavigationCompose = "1.4.0"
hiltWork = "1.4.0"                      # ➕ Workers con @HiltWorker
room = "3.0.3"
datastorePreferences = "1.2.1"
datastorePreferencesCore = "1.2.1"
workManager = "2.10.5"                  # ➕ cola offline
firebaseBom = "34.4.0"                  # ➕ FCM
googleServices = "4.4.4"                # ➕ plugin google-services
playServicesLocation = "21.3.0"         # ➕ geocerca
camerax = "1.5.1"                       # ➕ fotos de evidencia
credentials = "1.5.0"                   # ➕ Google Sign-In (Credential Manager)
googleId = "1.1.1"                      # ➕ Google ID token
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
coroutinesTest = "1.10.2"               # ➕ tests de ViewModel
mockk = "1.14.6"                        # ➕ mocks en tests

[libraries]
# AndroidX & Compose Base
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }
androidx-ui = { group = "androidx.compose.ui", name = "ui", version.ref = "ui" }

# Navigation (type-safe)
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }

# Networking (Retrofit + Gson + OkHttp)
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
converter-gson = { module = "com.squareup.retrofit2:converter-gson", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

# Image Loading (Coil 3)
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coilCompose" }
coil-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coilCompose" }

# Dependency Injection (Hilt & KSP)
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-android-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hiltWork" }
androidx-hilt-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "hiltWork" }

# Local Database (Room 3)
room-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room" }
room-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room" }

# Key-Value Storage (DataStore Preferences)
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastorePreferences" }
androidx-datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "datastorePreferencesCore" }

# Background work (offline queue)
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "workManager" }

# Firebase Cloud Messaging
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-messaging = { module = "com.google.firebase:firebase-messaging" }

# Location (geofence attendance)
play-services-location = { module = "com.google.android.gms:play-services-location", version.ref = "playServicesLocation" }

# Camera (evidence photos)
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
androidx-camera-compose = { group = "androidx.camera", name = "camera-compose", version.ref = "camerax" }

# Google Sign-In
androidx-credentials = { group = "androidx.credentials", name = "credentials", version.ref = "credentials" }
androidx-credentials-play-services = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "credentials" }
googleid = { module = "com.google.android.libraries.identity.googleid:googleid", version.ref = "googleId" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlinSerialization" }
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

No se usa el cliente oficial de Supabase (arrastra Ktor): la subida de fotos es un `POST` con OkHttp. Bluetooth (ELM327) usa las APIs nativas de Android, sin librería.

### `build.gradle.kts` (raíz)

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
}
```

### `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.andeva.atelier.mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.andeva.atelier.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // BuildConfig desde local.properties: ver docs/04-mobile-integration.md §1
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Jetpack Compose y Core
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)

    // Navegación type-safe
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Carga de imágenes remotas (Coil 3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Red y consumo de APIs (Retrofit + Gson + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Inyección de dependencias (Hilt + KSP)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Base de datos local (Room 3 + KSP)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Almacenamiento clave-valor reactivo (DataStore Preferences)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)

    // Trabajo en segundo plano (cola offline)
    implementation(libs.androidx.work.runtime.ktx)

    // Notificaciones push (FCM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Ubicación (asistencia con geocerca)
    implementation(libs.play.services.location)

    // Cámara (evidencias)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)

    // Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
```

> Como en el curso, los procesadores de anotaciones usan `ksp(...)`, nunca `kapt`.

---

## 3. Estructura de carpetas

```text
com.andeva.atelier.mobile/
│
├── AtelierApplication.kt              # @HiltAndroidApp + Configuration.Provider (HiltWorkerFactory)
├── MainActivity.kt                    # Edge-to-Edge, tema y AppNavHost
│
├── core/
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt         # Única base Room de la app (todas las Entities)
│   │   │   ├── JwtClaims.kt           # Lee `exp` del JWT para token_expires_at
│   │   │   └── SessionManager.kt      # token, refreshToken, permisos, tenant, membershipId (DataStore)
│   │   ├── remote/
│   │   │   ├── AuthInterceptor.kt     # Agrega "Authorization: Bearer" y "Accept-Language: es"
│   │   │   ├── PublicEndpoints.kt     # Rutas sin token (/auth/*, /invitations/validate|accept, GET /billing/plans)
│   │   │   ├── TokenRefresher.kt      # Un solo refresh aunque haya varios 401 a la vez (synchronized)
│   │   │   ├── TokenAuthenticator.kt  # 401 → TokenRefresher → reintenta una vez
│   │   │   ├── ErrorResponseDto.kt    # { code, message, details, timestamp }
│   │   │   ├── ApiResult.kt           # apiCall / apiCallUnit: Response → Result con ApiException(code, message, details)
│   │   │   ├── ConnectivityObserver.kt # StateFlow<Boolean> para deshabilitar acciones en línea
│   │   │   ├── employee/              # EmployeeService: perfil laboral (sede y turno), lo usan auth y attendance
│   │   │   └── session/               # AuthRefreshService (@RefreshClient), DeviceTokenService y sus DTOs
│   │   └── sync/
│   │       ├── PendingOperationEntity.kt / PendingUploadEntity.kt / CacheMetadataEntity.kt
│   │       ├── PendingOperationDao.kt / PendingUploadDao.kt / CacheMetadataDao.kt
│   │       ├── CacheMaintenanceDao.kt # Borra las cachés al cerrar sesión (no la cola)
│   │       ├── PendingOperationQueue.kt # Punto único para encolar (usa la sesión y dispara el sync)
│   │       ├── OperationHandler.kt    # Un handler por `type`, registrado por cada feature (@IntoMap)
│   │       ├── UploadFileHandler.kt   # UPLOAD_FILE (núcleo)
│   │       ├── SyncProcessor.kt       # Algoritmo de 06-offline-sync.md §3, sin Android (testeable)
│   │       ├── SyncScheduler.kt       # Trabajo único, reintento tras backoff y periódico de 15 min
│   │       └── SyncWorker.kt          # @HiltWorker: renueva el token y ejecuta SyncProcessor
│   ├── designsystem/
│   │   ├── icon/Icons.kt
│   │   ├── component/                 # AtelierTopBar, StatusChip, MoneyText, EmptyState, ErrorState
│   │   └── theme/                     # Color.kt, Theme.kt, Type.kt
│   ├── di/
│   │   ├── ClockModule.kt             # java.time.Clock inyectable (hora de captura de la cola, tests)
│   │   ├── DataStoreModule.kt
│   │   ├── DatabaseModule.kt
│   │   ├── NetworkModule.kt           # OkHttp (interceptor + authenticator + logging) y Retrofit
│   │   └── WorkModule.kt
│   ├── domain/
│   │   ├── ApiException.kt            # httpStatus + code + message + details (el caso de uso decide por HTTP)
│   │   ├── BusinessTime.kt            # Día de Lima (America/Lima): "hoy", dedupe_key, rangos de fechas
│   │   ├── Money.kt                   # amount (BigDecimal) + currency (PEN/USD)
│   │   ├── Permission.kt              # value class + constantes del catálogo usadas por la app
│   │   ├── Plan.kt
│   │   └── Session.kt                 # userId, tenant, membershipId, permissions; hasPermission()
│   ├── media/
│   │   ├── StoragePaths.kt            # Rutas del bucket con UUID por foto
│   │   └── SupabaseUploader.kt        # Comprime, sube con x-upsert y arma la URL pública (determinista)
│   └── notifications/
│       ├── AtelierMessagingService.kt # FirebaseMessagingService: onNewToken / onMessageReceived
│       ├── DeviceTokenRegistrar.kt    # PUT /users/me/device-token tras login y con null al salir
│       ├── NotificationChannels.kt
│       ├── PushInbox.kt               # Push tocado pendiente de abrir (lo consume cada feature)
│       └── PushRouter.kt              # data.type → PushDestination
│
├── navigation/
│   └── AppNavHost.kt
│
└── features/
    ├── auth/            # IAM: login, Google, registro de taller, OTP, invitación, recuperar contraseña
    ├── main/            # Scaffold + NavigationBar con pestañas según permisos
    ├── attendance/      # HR: marcar ingreso/salida, mi día, historial, descargos, mi turno
    ├── team/            # HR: asistencia de la sede, revisar descargos (supervisores)
    ├── reception/       # CRM: buscar placa/cliente, registrar, afiliar, citas, llegada
    ├── workorders/      # Operations: abrir orden, tareas, propuestas, proforma, fotos de recepción
    ├── tasks/           # Operations: mis tareas, cronómetro, pausas, suspensión, evidencias, repuestos, cierre
    ├── bays/            # Operations: tablero de bahías y reasignación
    ├── inventory/       # Inventory: catálogo, lotes, alertas de stock
    ├── diagnostics/     # IoT: escáner ELM327, DTC, telemetría en vivo, informes IA, alertas
    ├── cashier/         # Invoicing: comprobante SUNAT, pagos, pase de salida, entrega
    └── reports/         # Invoicing/IoT: flujo de caja y flota en PDF
```

Cada feature repite la estructura del curso:

```text
features/tasks/
├── application/
│   ├── GetMyTasksUseCase.kt
│   ├── StartTaskUseCase.kt
│   ├── PauseTaskUseCase.kt
│   ├── RequestSuspensionUseCase.kt
│   ├── AddPartToTaskUseCase.kt
│   └── CompleteTaskUseCase.kt
├── domain/
│   ├── Task.kt
│   ├── TaskId.kt
│   ├── TaskStatus.kt
│   ├── PauseReason.kt
│   └── TaskRepository.kt
├── infrastructure/
│   ├── di/
│   │   ├── TaskApiModule.kt
│   │   ├── TaskDaoModule.kt
│   │   └── TaskRepositoryModule.kt
│   ├── local/
│   │   ├── TaskDao.kt
│   │   └── TaskEntity.kt
│   ├── remote/
│   │   ├── TaskService.kt
│   │   ├── AssignedTaskDto.kt
│   │   ├── WorkOrderTaskDto.kt
│   │   └── PauseTaskRequestDto.kt
│   └── repository/
│       └── TaskRepositoryImpl.kt
└── presentation/
    ├── mytasks/
    │   ├── MyTasksScreen.kt
    │   ├── MyTasksUiState.kt
    │   └── MyTasksViewModel.kt
    ├── taskdetail/
    │   ├── TaskDetailScreen.kt
    │   ├── TaskDetailUiState.kt
    │   ├── TaskDetailViewModel.kt
    │   └── components/
    │       ├── TaskTimer.kt
    │       └── TaskProductList.kt
    └── navigation/
        └── TaskNavGraph.kt
```

### Mapa feature → historias → API

| Feature | Historias | Archivo de API |
| --- | --- | --- |
| `auth` | US01–US05 | [api/iam.md](api/iam.md) |
| `attendance` | US06–US10 | [api/hr.md](api/hr.md) |
| `team` | US10 (supervisión de descargos), asistencia diaria de la sede | [api/hr.md](api/hr.md) |
| `reception` | US11, US44 | [api/crm.md](api/crm.md) |
| `workorders` | US12–US15, US22, US24, US25, US36, US33 E2, US35 E2 | [api/operations.md](api/operations.md) |
| `tasks` | US23, US26–US30, US33 E1, US35 E1/E3 | [api/operations.md](api/operations.md) |
| `bays` | US40 | [api/operations.md](api/operations.md) |
| `inventory` | US31, US32, US34 | [api/inventory.md](api/inventory.md) |
| `diagnostics` | US16–US21, US43 | [api/iot.md](api/iot.md) |
| `cashier` | US37–US39 | [api/invoicing.md](api/invoicing.md), [api/operations.md](api/operations.md) |
| `reports` | US41, US43 E2 | [api/invoicing.md](api/invoicing.md), [api/iot.md](api/iot.md) |

---

## 4. Convenciones de nomenclatura

Idénticas a la guía del curso:

| Elemento | Convención | Ejemplo en Atelier |
| :--- | :--- | :--- |
| Paquetes | minúsculas separadas por punto | `com.andeva.atelier.mobile.features.tasks.domain` |
| Entidades de dominio | Sustantivo PascalCase | `Task.kt`, `WorkOrder.kt`, `Attendance.kt` |
| Value Objects | Sustantivo PascalCase | `Money.kt`, `TaskId.kt`, `Plate.kt` |
| Interfaces de repositorio | `[Modelo]Repository` | `TaskRepository.kt` |
| Implementaciones | `[Modelo]RepositoryImpl` / `InMemory[Modelo]Repository` | `TaskRepositoryImpl.kt` |
| Casos de uso | Verbo + Modelo + `UseCase` | `StartTaskUseCase.kt`, `ClockInUseCase.kt` |
| Servicios Retrofit | `[Modelo]Service` | `TaskService.kt`, `AttendanceService.kt` |
| DTOs | `[Modelo]Dto`, `[Acción]RequestDto`, `[Acción]ResponseDto` | `AssignedTaskDto.kt`, `ClockInRequestDto.kt` |
| Entidades Room | `[Modelo]Entity` | `TaskEntity.kt` |
| DAOs | `[Modelo]Dao` | `TaskDao.kt` |
| Módulos Hilt de API | `[Feature]ApiModule` (`object`) | `TaskApiModule.kt` |
| Módulos Hilt de repositorio | `[Feature]RepositoryModule` (`interface`, `@Binds`) | `TaskRepositoryModule.kt` |
| Módulos Hilt de DAO | `[Feature]DaoModule` (`object`) | `TaskDaoModule.kt` |
| Estado de UI | `[Pantalla]UiState` | `TaskDetailUiState.kt` |
| ViewModels | `[Pantalla]ViewModel` | `TaskDetailViewModel.kt` |
| Pantallas | `[Pantalla]Screen` | `TaskDetailScreen.kt` |
| Rutas | `[Feature]NavGraphRoute`, `[Pantalla]Route` | `TaskNavGraphRoute`, `TaskDetailRoute(taskId)` |
| Grafo de feature | `fun NavGraphBuilder.[feature]NavGraph(navController: NavController)` | `taskNavGraph()` |

Reglas extra de este proyecto:

- Los enums del backend se reflejan como `enum class` de dominio con los mismos nombres (`TaskStatus.IN_PROGRESS`); el DTO los recibe como `String` y el repositorio los convierte.
- Los ids son `String` UUID envueltos en `@JvmInline value class` (`TaskId`, `WorkOrderId`).
- Los textos visibles van en `strings.xml` (español).

---

## 5. Patrones por capa

### 5.1. Dominio

```kotlin
// core/domain/Money.kt — BigDecimal (no Double) para no perder céntimos; Room lo guarda como String (06 §5)
data class Money(
    val amount: BigDecimal,
    val currency: String
) {
    init {
        require(amount >= BigDecimal.ZERO) { "Amount must be greater than or equal to 0" }
        require(currency == PEN || currency == USD) { "Currency must be PEN or USD" }
    }

    companion object {
        const val PEN = "PEN"
        const val USD = "USD"
    }
}

// features/tasks/domain/TaskId.kt
@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId must not be blank" }
    }
}

// features/tasks/domain/Task.kt
data class Task(
    val id: TaskId,
    val workOrderId: String,
    val workOrderNumber: Int?,
    val vehicleId: String,
    val description: String,
    val status: TaskStatus,
    val startedAt: Instant?,
    val totalPausedSeconds: Long
)

// features/tasks/domain/TaskRepository.kt
interface TaskRepository {
    suspend fun getMyTasks(): List<Task>
    suspend fun start(id: TaskId): Result<Task>
    suspend fun pause(id: TaskId, reason: PauseReason): Result<Task>
}
```

Permisos como concepto de dominio (la app decide la UI con ellos, nunca con el nombre del rol):

```kotlin
// core/domain/Permission.kt
@JvmInline
value class Permission(val value: String) {
    companion object {
        val TASKS_READ = Permission("operations:tasks:read")
        val TASKS_TRACK_TIME = Permission("operations:tasks:track_time")
        val TASKS_HOLD_REQUEST = Permission("operations:tasks:hold_request")
        val TASKS_HOLD_VALIDATE = Permission("operations:tasks:hold_validate")
        val ATTENDANCE_CLOCK_IN = Permission("hr:attendance:clock_in")
        val TELEMETRY_INGEST = Permission("iot:telemetry:ingest")
        // … resto según docs/02-roles-and-permissions.md
    }
}

// core/domain/Session.kt
data class Session(
    val userId: String,
    val fullName: String,
    val tenantId: String,
    val tenantName: String,
    val membershipId: String?,
    val permissions: Set<String>,
    val plan: Plan?                                   // null: sin plan conocido → no ocultar, el backend decide
) {
    fun has(permission: Permission) = permission.value in permissions

    /** Diagnóstico OBD-II: permiso del rol y módulo incluido en el plan. */
    fun canUseTelemetry() = has(Permission.TELEMETRY_INGEST) && (plan?.iotTelemetryEnabled ?: true)

    /** Informe de salud con IA: permiso del rol y módulo incluido en el plan. */
    fun canUseAiDiagnostics() = has(Permission("iot:health_reports:generate")) && (plan?.aiDiagnosticsEnabled ?: true)
}

// core/domain/Plan.kt
data class Plan(
    val tier: String,                                 // GO | PRO | MAX | ENTERPRISE | NONE
    val status: String,
    val accessGranted: Boolean,
    val iotTelemetryEnabled: Boolean,
    val aiDiagnosticsEnabled: Boolean
)
```

La UI combina **permiso del rol** y **módulo del plan**: un mecánico con `iot:telemetry:ingest` no ve el escáner si el plan del taller no incluye IoT.

### 5.2. Aplicación

```kotlin
// features/tasks/application/StartTaskUseCase.kt
class StartTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId) = taskRepository.start(id)
}

// features/attendance/application/ClockInUseCase.kt
class ClockInUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val locationProvider: LocationProvider
) {
    suspend operator fun invoke(branchId: String, shiftId: String): Result<Attendance> {
        val location = locationProvider.currentLocation()
            ?: return Result.failure(LocationUnavailableException())
        return attendanceRepository.clockIn(branchId, shiftId, location.latitude, location.longitude)
    }
}
```

`LocationProvider` es una interfaz de dominio; su implementación con `FusedLocationProviderClient` vive en `infrastructure`.

### 5.3. Infraestructura

#### Retrofit y DTOs

```kotlin
// features/tasks/infrastructure/remote/TaskService.kt
interface TaskService {
    @GET("api/v1/tasks/my")
    suspend fun getMyTasks(): Response<List<AssignedTaskDto>>

    @POST("api/v1/tasks/{taskId}/start")
    suspend fun start(@Path("taskId") taskId: String): Response<WorkOrderTaskDto>

    @POST("api/v1/tasks/{taskId}/pause")
    suspend fun pause(@Path("taskId") taskId: String, @Body body: PauseTaskRequestDto): Response<WorkOrderTaskDto>
}

// features/tasks/infrastructure/remote/AssignedTaskDto.kt
data class AssignedTaskDto(
    @SerializedName("taskId") val taskId: String,
    @SerializedName("workOrderId") val workOrderId: String,
    @SerializedName("workOrderNumber") val workOrderNumber: Int?,
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("status") val status: String,
    @SerializedName("description") val description: String,
    @SerializedName("startedAt") val startedAt: String?,
    @SerializedName("totalPausedSeconds") val totalPausedSeconds: Long?
)
```

Los campos de cada DTO salen de la sección *Modelos* del archivo de API del contexto (por ejemplo `AssignedTaskResource` en [api/operations.md](api/operations.md)).

#### Errores del backend

```kotlin
// core/data/remote/ErrorResponseDto.kt
data class ErrorResponseDto(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("details") val details: List<String> = emptyList()
)

// core/domain/ApiException.kt — en el dominio para que los casos de uso decidan por HTTP sin importar Retrofit
class ApiException(val httpStatus: Int, val code: String, override val message: String, val details: List<String>) : Exception(message)

// core/data/remote/ApiResult.kt
suspend fun <T, R> apiCall(gson: Gson, call: suspend () -> Response<T>, map: (T) -> R): Result<R> = try {
    val response = call()
    val body = response.body()
    if (response.isSuccessful && body != null) Result.success(map(body))
    else if (response.isSuccessful) Result.failure(ApiException(response.code(), "EMPTY_BODY", "Respuesta vacía del servidor"))
    else Result.failure(response.toApiException(gson))
} catch (e: IOException) {
    Result.failure(e)
}

// Para endpoints sin cuerpo (204), como PUT /users/me/device-token: apiCall fallaría porque body() es null.
suspend fun apiCallUnit(gson: Gson, call: suspend () -> Response<*>): Result<Unit> = try {
    val response = call()
    if (response.isSuccessful) Result.success(Unit) else Result.failure(response.toApiException(gson))
} catch (e: IOException) {
    Result.failure(e)
}
```

#### Sesión, interceptor y renovación del token

```kotlin
// core/data/local/SessionManager.kt
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TOKEN = stringPreferencesKey("token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val PERMISSIONS = stringSetPreferencesKey("permissions")
        val TENANT_ID = stringPreferencesKey("tenant_id")
        val MEMBERSHIP_ID = stringPreferencesKey("membership_id")
    }

    suspend fun getAuthToken(): String? = dataStore.data.map { it[TOKEN] }.first()
    suspend fun getRefreshToken(): String? = dataStore.data.map { it[REFRESH_TOKEN] }.first()

    suspend fun save(token: String, refreshToken: String, permissions: List<String>, tenantId: String?, membershipId: String?) {
        dataStore.edit {
            it[TOKEN] = token
            it[REFRESH_TOKEN] = refreshToken
            it[PERMISSIONS] = permissions.toSet()
            tenantId?.let { id -> it[TENANT_ID] = id }
            membershipId?.let { id -> it[MEMBERSHIP_ID] = id }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

// core/data/remote/AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = runBlocking { sessionManager.getAuthToken() }
        val request = chain.request().newBuilder()
            .header("Accept-Language", "es")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        return chain.proceed(request)
    }
}

// core/data/remote/TokenRefresher.kt — el refresh ROTA el refreshToken (api/README.md §2):
// dos 401 simultáneos no pueden refrescar dos veces o el segundo cerraría la sesión.
@Singleton
class TokenRefresher @Inject constructor(
    private val sessionManager: SessionManager,
    @RefreshClient private val refreshService: AuthRefreshService
) {
    private val lock = Any()

    fun refreshIfStale(staleToken: String?): String? = synchronized(lock) {
        val current = runBlocking { sessionManager.getAuthToken() } ?: return null
        if (staleToken != null && current != staleToken) return current        // otro hilo ya renovó
        val refreshToken = runBlocking { sessionManager.getRefreshToken() } ?: return null
        val response = try { refreshService.refresh(RefreshTokenRequestDto(refreshToken)).execute() }
                       catch (_: IOException) { return null }                   // sin red: se conserva la sesión
        val body = response.body()
        if (response.isSuccessful && body != null) { runBlocking { sessionManager.save(body) }; return body.token }
        if (response.code() == 401) runBlocking { sessionManager.clear() }
        null
    }
}

// core/data/remote/TokenAuthenticator.kt — se ejecuta solo ante un 401
class TokenAuthenticator @Inject constructor(private val tokenRefresher: TokenRefresher) : Authenticator {
    override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
        val request = response.request
        if (request.header("X-Retry-After-Refresh") != null) return null       // ya se intentó
        if (PublicEndpoints.isPublic(request)) return null                      // 401 de credenciales, no de sesión
        val token = tokenRefresher.refreshIfStale(request.header("Authorization")?.removePrefix("Bearer ")) ?: return null
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("X-Retry-After-Refresh", "1")
            .build()
    }
}
```

`refresh` es síncrono (`Call<…>`) y usa `synchronized`, no un `Mutex`: el authenticator corre en un hilo de OkHttp y un `Mutex` con `runBlocking` ahí arriesga bloqueos. El `AuthRefreshService` se crea con un `Retrofit` **sin** el `TokenAuthenticator` (calificador `@RefreshClient`) para no entrar en bucle. Cuando el refresh falla con 401, la sesión se borra y `AppNavHost` vuelve al login observando el `SessionManager`. `SyncWorker` usa el mismo `TokenRefresher.ensureFresh()` antes de vaciar la cola.

#### Hilt

```kotlin
// core/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, tokenAuthenticator: TokenAuthenticator): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(60, TimeUnit.SECONDS)     // Render despierta en frío
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}

// features/tasks/infrastructure/di/TaskApiModule.kt
@Module
@InstallIn(SingletonComponent::class)
object TaskApiModule {
    @Provides
    @Singleton
    fun provideTaskService(retrofit: Retrofit): TaskService = retrofit.create(TaskService::class.java)
}

// features/tasks/infrastructure/di/TaskRepositoryModule.kt
@Module
@InstallIn(ViewModelComponent::class)
interface TaskRepositoryModule {
    @Binds
    fun provideTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
```

Excepción a la guía del curso: los repositorios que usan los `Worker` (telemetría, fotos, salida) se enlazan en `SingletonComponent`, porque `ViewModelComponent` no existe dentro de un `Worker`.

#### Offline-first (Room como fuente única de la verdad)

```kotlin
// features/tasks/infrastructure/repository/TaskRepositoryImpl.kt
class TaskRepositoryImpl @Inject constructor(
    private val service: TaskService,
    private val dao: TaskDao
) : TaskRepository {

    override suspend fun getMyTasks(): List<Task> {
        try {
            val response = service.getMyTasks()
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    dao.replaceAll(dtos.map { it.toEntity() })   // 1. DTO → Entity, 2. guardar en Room
                }
            }
        } catch (_: Exception) {
            // Sin red: se continúa con el caché local
        }
        return dao.fetchAll().map { it.toDomain() }              // 3. siempre se lee de Room
    }
}
```

Las escrituras que deben sobrevivir sin red (marcaciones, telemetría, DTC, fotos, descargos) se guardan en `pending_operations` y las envía `SyncWorker`. Las Entities, la cola y el algoritmo del worker están en [06-offline-sync.md](06-offline-sync.md).

### 5.4. Presentación

Igual que en el curso: `@HiltViewModel`, `MutableStateFlow` privado, `StateFlow` público, `update { it.copy(...) }`, y pantallas con `collectAsStateWithLifecycle()`.

```kotlin
// features/tasks/presentation/taskdetail/TaskDetailUiState.kt
data class TaskDetailUiState(
    val task: Task? = null,
    val canTrackTime: Boolean = false,
    val canRequestSuspension: Boolean = false,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

// features/tasks/presentation/taskdetail/TaskDetailViewModel.kt
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTask: GetTaskUseCase,
    private val startTask: StartTaskUseCase,
    private val getSession: GetSessionUseCase
) : ViewModel() {

    private val taskId = TaskId(savedStateHandle.toRoute<TaskDetailRoute>().taskId)

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val session = getSession()
            getTask(taskId)
                .onSuccess { task ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            task = task,
                            canTrackTime = session.has(Permission.TASKS_TRACK_TIME),
                            canRequestSuspension = session.has(Permission.TASKS_HOLD_REQUEST)
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }

    fun onStart() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            startTask(taskId)
                .onSuccess { task -> _uiState.update { it.copy(isSubmitting = false, task = task) } }
                .onFailure { e -> _uiState.update { it.copy(isSubmitting = false, errorMessage = e.message) } }
        }
    }
}
```

`e.message` ya es el `message` traducido del backend (`ApiException`), así que se puede mostrar tal cual en un `Snackbar`.

### 5.5. Navegación type-safe

```kotlin
// features/tasks/presentation/navigation/TaskNavGraph.kt
@Serializable
data object TaskNavGraphRoute

@Serializable
data object MyTasksRoute

@Serializable
data class TaskDetailRoute(val taskId: String)

fun NavGraphBuilder.taskNavGraph(navController: NavController) {
    navigation<TaskNavGraphRoute>(startDestination = MyTasksRoute) {
        composable<MyTasksRoute> {
            MyTasksScreen { task -> navController.navigate(TaskDetailRoute(task.id.value)) }
        }
        composable<TaskDetailRoute> {
            TaskDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

Pestañas según permisos (la barra se arma con la sesión):

```kotlin
// features/main/presentation/NavigationItem.kt
sealed class NavigationItem(
    val route: Any,
    val icon: ImageVector,
    val title: String,
    val requires: Permission?
) {
    data object Tasks : NavigationItem(TaskNavGraphRoute, Icons.Default.Build, "Tareas", Permission.TASKS_READ)
    data object Attendance : NavigationItem(AttendanceNavGraphRoute, Icons.Default.Schedule, "Asistencia", Permission.ATTENDANCE_CLOCK_IN)
    data object Reception : NavigationItem(ReceptionNavGraphRoute, Icons.Default.DirectionsCar, "Recepción", Permission("crm:vehicles:create"))
    data object Bays : NavigationItem(BayNavGraphRoute, Icons.Default.Dashboard, "Bahías", Permission("operations:bays:read"))
    data object More : NavigationItem(MoreNavGraphRoute, Icons.Default.Menu, "Más", null)

    companion object {
        fun tabsFor(session: Session): List<NavigationItem> =
            listOf(Tasks, Attendance, Reception, Bays, More).filter { it.requires == null || session.has(it.requires) }.take(5)
    }
}
```

Los pushes abren la pantalla correcta con `PushRouter`, que convierte `data.type` + ids en una ruta (`TaskDetailRoute(taskId)`) y la entrega a `MainActivity` mediante el `Intent`.

### 5.6. Sistema de diseño

Se mantiene el patrón del curso (`core/designsystem/theme` y `icon/Icons.kt` con `ImageVector.Builder` y caché privada). Componentes propios recomendados: `StatusChip` (colores por `WorkOrderStatus` / `WorkOrderTaskStatus`), `MoneyText` (formato `S/ 1,234.50`), `ErrorState` (muestra `message` y botón Reintentar) y `PermissionGate { }` (no dibuja su contenido si falta el permiso).

---

## 6. Cómo construir un feature (paso a paso)

1. **Dominio:** entidad, value objects, enum de estados e interfaz de repositorio en `features/<feature>/domain`.
2. **Infraestructura:** `Service` Retrofit + DTOs a partir del archivo de API del contexto; `Entity` + `Dao` si hay caché (registrar la entidad en `AppDatabase`); `RepositoryImpl` con el patrón offline-first; módulos `ApiModule`, `DaoModule` y `RepositoryModule`.
3. **Aplicación:** un `UseCase` por acción, con `operator fun invoke`.
4. **Presentación:** `UiState`, `ViewModel` (`@HiltViewModel`), `Screen` y `components/`; los botones que dependen de permisos leen la `Session`.
5. **Navegación:** rutas `@Serializable` y `NavGraphBuilder.<feature>NavGraph(...)`; registrar el grafo en `MainScreen` o `AppNavHost` y, si es pestaña, en `NavigationItem`.
6. **Trazabilidad:** comprobar en [03-traceability.md](03-traceability.md) que cada escenario de la historia tiene su llamada.

---

## 7. Resumen

1. **DIP:** ViewModel → UseCase → interfaz de repositorio; la implementación vive en infraestructura.
2. **Hilt:** `@Binds` en interfaces para repositorios; `@Provides` en `object` para Retrofit, Room, DataStore, OkHttp y servicios de Android.
3. **Estado:** `StateFlow` inmutable expuesto, `MutableStateFlow` privado mutado con `.update { it.copy(...) }`.
4. **Offline-first:** red → Room → UI; las escrituras críticas pasan por la cola de `WorkManager`.
5. **Seguridad:** la UI se arma con permisos del JWT; el backend vuelve a validar todo.
6. **Sesión:** `AuthInterceptor` agrega el token; `TokenAuthenticator` renueva una sola vez ante 401 y, si falla, cierra la sesión.
