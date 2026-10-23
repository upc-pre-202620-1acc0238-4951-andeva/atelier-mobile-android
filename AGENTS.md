# AGENTS.md — Guía para asistentes de IA

Este repositorio es la app Android de staff de Atelier. Antes de escribir código, lee:

0. `docs/07-roadmap.md` — en qué paso va la construcción, qué hace cada paso y qué reutilizar del núcleo `core/`. Al terminar un paso, márcalo ahí.
1. `docs/05-android-architecture.md` — arquitectura, librerías, carpetas y nombres. **Síguela al pie de la letra.**
2. `docs/03-traceability.md` — qué endpoint y permiso cubre cada escenario de la historia que estás implementando.
3. `docs/api/<contexto>.md` — campos exactos de request y response (los DTOs salen de la sección *Modelos*).
4. `docs/api/README.md` — sesión, refresh token, formato de error, fechas (UTC → America/Lima), dinero e IGV.
5. `docs/06-offline-sync.md` — Entities Room, cola `pending_operations` y qué acciones funcionan sin red. No encoles acciones marcadas como "en línea".

## Reglas

- Clean Architecture + Feature-First + DDD. `domain` no importa Android, Retrofit, Room ni Hilt.
- Un `UseCase` por acción con `operator fun invoke`. ViewModel con `MutableStateFlow` privado y `StateFlow` público, mutado con `update { it.copy(...) }`.
- Hilt: `@Provides` en `object` (`*ApiModule`, `*DaoModule`), `@Binds` en `interface` (`*RepositoryModule`). KSP, nunca kapt.
- Navegación type-safe con rutas `@Serializable` y `NavGraphBuilder.<feature>NavGraph(...)`.
- Repositorios offline-first: red → Room → UI. Escrituras que deben sobrevivir sin red → cola `pending_operations` + `SyncWorker`.
- La UI se habilita por **permiso** (`Session.has(Permission.X)`), nunca por nombre de rol.
- Mostrar al usuario el `message` del error del backend; decidir el flujo por el código HTTP.
- No inventar endpoints ni campos: si no está en `docs/api/`, no existe. Si una historia lo necesita, márcalo como hueco en `docs/03-traceability.md`.
- No versionar secretos: `local.properties`, `google-services.json`, llaves de firma y cuentas de prueba quedan fuera de git.
- Textos visibles en español, en `strings.xml`.

## Comandos

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```