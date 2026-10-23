# Documentación de Atelier Mobile

Documentación para construir la app Android de staff. Está escrita en Markdown simple (sin LaTeX ni HTML) para que la lean igual una persona y un asistente de IA.

## Orden de lectura

| # | Archivo | Qué responde |
| --- | --- | --- |
| 1 | [01-requirements/user-stories.md](01-requirements/user-stories.md) | **Qué** hay que construir: épicas EP01–EP09 e historias US01–US44 con sus criterios Gherkin |
| 2 | [02-roles-and-permissions.md](02-roles-and-permissions.md) | **Quién** puede hacer cada cosa: 8 roles, catálogo de permisos, matriz y qué pantalla habilita cada permiso |
| 3 | [03-traceability.md](03-traceability.md) | **Con qué endpoint** se cumple cada escenario, con qué permiso, y qué falta en el backend |
| 4 | [api/README.md](api/README.md) | **Cómo** hablar con el backend: sesión, errores, cabeceras, fechas, dinero, archivos y enums |
| 5 | `api/<contexto>.md` | Referencia de cada endpoint: permiso, parámetros, body, respuesta y modelos |
| 6 | [04-mobile-integration.md](04-mobile-integration.md) | Configuración, push FCM, fotos en Supabase, OBD-II, offline, permisos de Android, cuentas de prueba |
| 7 | [05-android-architecture.md](05-android-architecture.md) | **Cómo** se programa: librerías, carpetas, nombres y patrones (estilo del curso) |
| 8 | [06-offline-sync.md](06-offline-sync.md) | **Sin red**: tablas Room, cola de operaciones, idempotencia por endpoint y flujos offline (revisión del modelo local del informe) |
| — | [01-requirements/technical-stories.md](01-requirements/technical-stories.md) | Historias técnicas TS01–TS24 del backend (contexto) |

## Fuentes y cómo se generaron

| Documento | Fuente |
| --- | --- |
| Historias de usuario y técnicas | `atelier-report/.../24-requirements-specification.md` (convertido de tablas LaTeX a Markdown) |
| `api/*.md` | Código de los controladores y records de `atelier-platform` + OpenAPI exportado (`/v3/api-docs`) |
| Roles y permisos | `PermissionCatalog.java` y `RoleTemplate.java` del backend |
| Trazabilidad | Revisión de cada escenario contra el código del backend |

Si el backend cambia, **el código del backend manda**. Regenerar `api/*.md` cuando cambien controladores o recursos.

## Estado del backend

Referencia: rama `hotfix/1.0.3-unified-error-body` de `atelier-platform` (formato de error único, `membershipId` en la sesión, desglose `subtotal`/`taxAmount` en la orden y los huecos G1–G10 cerrados). Todos los escenarios de US01–US44 tienen respaldo en el backend o se resuelven en la app; ver [03-traceability.md → Huecos cerrados](03-traceability.md#huecos-cerrados).
