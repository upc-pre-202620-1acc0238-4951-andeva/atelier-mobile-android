# AGENTS.md — Guía para asistentes de IA

Este repositorio es la app Android de staff de Atelier. Antes de escribir código, lee:

0. `docs/07-roadmap.md` — en qué paso va la construcción, qué hace cada paso y qué reutilizar del núcleo `core/`. Al terminar un paso, márcalo ahí.
1. `docs/05-android-architecture.md` — arquitectura, librerías, carpetas y nombres. **Síguela al pie de la letra.**
2. `docs/03-traceability.md` — qué endpoint y permiso cubre cada escenario de la historia que estás implementando.
3. `docs/api/<contexto>.md` — campos exactos de request y response (los DTOs salen de la sección *Modelos*).
4. `docs/api/README.md` — sesión, refresh token, formato de error, fechas (UTC → America/Lima), dinero e IGV.
5. `docs/06-offline-sync.md` — Entities Room, cola `pending_operations` y qué acciones funcionan sin red. No encoles acciones marcadas como "en línea".

Antes de tocar una pantalla, un ViewModel de UI, `core/designsystem/` o `strings.xml`, lee además:

6. `PRODUCT.md` — para quién es la app, en qué situación la usa, la voz y qué no se puede inventar.
7. `DESIGN.md` — el sistema visual: tokens, tipografía, componentes, estados, carga y movimiento. **Manda sobre lo visual**; `docs/` manda sobre lo funcional.
8. `.impeccable/surfaces/` — el brief de la superficie que estás construyendo (alcance, dirección acordada y decisiones abiertas), si existe.

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

## Interfaz

El rediseño va por segmentos. Primero: **dueño y administrador de taller**, con todo lo que sus permisos habilitan en todos los módulos (docs/02-roles-and-permissions.md): operación de la sede, órdenes, equipo, repuestos, caja y reportes. No rediseñes pantallas de otro segmento sin que se pida.

- Colores, tipografía y formas salen del tema (`MaterialTheme.colorScheme`, `MaterialTheme.typography`, `AtelierTheme.colors`). Nada de `Color(0x…)`, `.sp` ni `RoundedCornerShape(…)` sueltos en una pantalla.
- Los componentes reutilizables viven en `core/designsystem/component/`. Una pantalla no redefine botones, campos, tarjetas ni barras: si falta una variante, se agrega ahí.
- Material 3 gobierna estructura y controles: objetivos táctiles de 48 dp, gesto Atrás del sistema, edge-to-edge con insets, barra inferior flotante de 3 a 5 destinos con un botón de acción circular, un solo FAB por pantalla (en raíces de pestaña, la acción va en la barra).
- Cada pantalla resuelve sus estados: cargando, vacío, error, pendiente de sincronizar y sin permiso. El estado nunca depende solo del color.
- **Carga:** Room se pinta primero. El esqueleto aparece solo si no hay caché, y se deriva del layout real de la pantalla (ver DESIGN.md → *Carga*); nunca una pantalla en blanco ni un spinner a pantalla completa.
- Movimiento: respuesta al presionar, no al soltar; entradas con ease-out; respetar "quitar animaciones" del sistema. Lo que se ve decenas de veces al día no se anima.
- Cada pantalla trae `@Preview` en claro y oscuro con datos realistas del taller.
- La capa `presentation` sigue la misma arquitectura: estado en `UiState`, eventos al ViewModel, composables sin lógica de negocio.

Skills de diseño (en `.claude/skills/`, por desarrollador; no se versionan):

| Skill | Úsala para |
| --- | --- |
| `impeccable` | Flujo principal. `shape` antes de construir una pantalla, `critique` y `audit` para revisarla, `polish` y `harden` antes de entregar, `document` para regenerar DESIGN.md desde el código |
| `ui-ux-pro-max` | Consultar reglas de UX y de Jetpack Compose (`--stack jetpack-compose`) y la lista de verificación previa a la entrega |
| `emil-design-eng` | Decidir si algo se anima, con qué curva y duración; detalles de presión y transición |
| `apple-design` | Gestos, hojas inferiores, continuidad espacial y movimiento reducido |

Si DESIGN.md y el código se contradicen, corrige el código; si la decisión cambió, actualiza DESIGN.md en el mismo cambio.

## Comandos

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```