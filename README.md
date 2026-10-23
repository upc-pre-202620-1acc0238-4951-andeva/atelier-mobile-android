# Atelier Mobile

App Android para el **personal del taller** (dueño, administrador, mecánico jefe, asesor, recepcionista, mecánico, almacén y caja) del ecosistema Atelier. Consume el backend `atelier-platform` (Spring Boot, Render).

- **Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Retrofit, Room 3, DataStore, WorkManager, Firebase Messaging, CameraX, Play Services Location y Bluetooth (ELM327).
- **Arquitectura:** Clean Architecture + Feature-First + DDD, con la guía del curso como estándar ([docs/05-android-architecture.md](docs/05-android-architecture.md)).

## Documentación

Empieza por [docs/README.md](docs/README.md). Ahí está el orden de lectura y para qué sirve cada archivo.

## Configuración local

1. Copiar los valores de [docs/04-mobile-integration.md §1](docs/04-mobile-integration.md#1-configuración-del-proyecto) a `local.properties`.
2. Descargar `google-services.json` de Firebase y dejarlo en `app/`.
3. Ninguno de esos archivos se versiona (ver `.gitignore`).
