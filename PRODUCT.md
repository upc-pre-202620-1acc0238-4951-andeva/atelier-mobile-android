# Product

<!-- impeccable:product-schema 1 -->

Verdad de producto de **Atelier Workshop Mobile**, la app Android de staff. Reemplaza a `docs/product-marketing.md`. Las decisiones visuales viven en [DESIGN.md](DESIGN.md); el detalle funcional, en [docs/](docs/README.md).

## Platform

android

## Users

La app la usa el personal de un taller mecánico automotriz MYPE (1 a 5 bahías o elevadores, multimarca, independiente) en Lima Metropolitana y otras ciudades de Latinoamérica, con facturación mensual entre S/ 5,000 y S/ 35,000. Hay ocho roles de fábrica; la app decide por **permiso**, nunca por nombre de rol ([docs/02-roles-and-permissions.md](docs/02-roles-and-permissions.md)).

### Segmento 1 — Dueño y administrador de taller (decide y paga)

Referencia: **Pedro Suárez**, dueño que gestiona solo presupuestos, compras y caja.

- **Situación:** de pie en el patio o en el mostrador, entre un cliente y otro, con el teléfono en una mano; también en casa al cierre del día. Luz de fluorescente o de portón abierto.
- **Trabajo en el móvil (confirmado 2026-11-16):** primero **supervisar** — cómo va el taller hoy y qué espera su decisión — y, a un toque, **operar** recepción y órdenes, porque en un taller chico el dueño también atiende.
- **Le importa:** rentabilidad, que no se fuguen repuestos, formalidad sin multas de SUNAT, trato honesto con el cliente.
- **Le pesa:** la sobrecarga de cuadrar todo solo, clientes que desconfían del presupuesto, no poder atender flotas por falta de reportes formales.
- El dueño tiene además permisos que el administrador no: flujo de caja, configuración fiscal SUNAT, remuneraciones y suscripción.

### Segmento 2 — Personal de piso y mostrador

| Persona | Le importa | Le pesa | Qué hace en la app |
| --- | --- | --- | --- |
| **Andrés Vílchez**, técnico mecatrónico (usuario y promotor interno) | Resolver averías difíciles, que sus horas se reconozcan, evitar reclamos injustos | Órdenes en papel ilegibles, fosa sin señal, tiempos muertos esperando repuestos, usar su celular personal para las fotos | Marca asistencia, ejecuta y cronometra tareas, registra fotos, reporta hallazgos, escanea por OBD-II |
| **Asesor de servicio / recepcionista** | Atender rápido en mostrador, agenda ordenada, entrega sin fricciones de cobro | Transcribir datos a mano, preguntar a gritos por bahías libres, demoras en la autorización | Busca por placa o documento, registra y afilia clientes, abre órdenes, gestiona propuestas y proformas |
| Mecánico jefe, encargado de inventario, cajero | — | — | Bahías y suspensiones; catálogo, lotes y alertas; cobro, comprobante y pase de salida |

**Anti-persona:** talleres estrictamente informales que rechazan emitir comprobantes o bancarizar cobros, y concesionarias corporativas (más de 50 sucursales) que exigen ERPs como SAP con consultoría dedicada.

## Product Purpose

Atelier Workshop une el piso de trabajo con la administración del taller. El mecánico diagnostica con cualquier escáner OBD-II Bluetooth comercial, documenta la reparación con fotos y cronometra su labor desde esta app, que funciona sin red en la fosa; el dueño o administrador gestiona órdenes, controla repuestos por lote FIFO y emite comprobantes SUNAT.

**Problema que resuelve:** el taller tradicional opera a ciegas. La administración vive desconectada de lo que ocurre en la bahía y depende de libretas manchadas y audios de WhatsApp. Eso produce mermas de inventario, horas productivas perdidas y fricción con clientes que desconfían del cobro.

**Trabajos que el dueño le encarga al producto:**

- *Monitoreo transparente:* saber qué vehículo está en cada bahía, qué repuestos se consumieron y cuánto se cobró, sin perseguir a los mecánicos ni cuadrar notas al final del día.
- *Cierre fiscal ágil:* emitir factura o boleta electrónica válida al cerrar la reparación, sin un software contable paralelo ni redigitar datos.
- *Respaldo probatorio:* sostener cada intervención con fotos y lecturas del vehículo para que el cliente no dude del presupuesto ni reclame por averías previas.

**Éxito para el negocio:** ser el estándar de gestión operativa y telemetría diagnóstica del taller MYPE peruano; meta de más de 100 talleres suscritos en el primer año comercial.

## Positioning

Un solo flujo continuo y verificable desde la bahía hasta el comprobante: diagnóstico computarizado, ejecución en fosa, almacén y cumplimiento fiscal, con dos interfaces hechas a medida (panel web para gestión, app móvil para el piso).

Lo que un producto vecino no puede copiar con verdad:

- **Funciona sin red en la fosa.** Arquitectura offline-first: lo hecho sin señal se guarda en el dispositivo y se sincroniza solo al recuperarla.
- **Evidencia en dos niveles.** Fotos de recepción y entrega por orden, y fotos de desmontaje y montaje por tarea.
- **OBD-II agnóstico al hardware.** Lee códigos de falla y parámetros en vivo con adaptadores comerciales de bajo costo.
- **FIFO estricto por lote.** El descargo usa el costo real de compra de cada lote y protege el margen.
- **SUNAT nativo.** Boleta y factura UBL 2.1 bajo Régimen MYPE Tributario al cierre de la orden, vía PSE.
- **Hecho para el taller peruano.** Tarifas en soles, tributación local, sin equipos sofisticados en la zona de trabajo.

**Por qué fallan las alternativas:** Mi Taller CRM (Perú) es web de oficina, sin app de bahía, sin OBD-II y con almacén sin costeo por lote. OK CAR (regional) no emite comprobantes SUNAT, cobra en dólares y su preventivo es un cálculo teórico de kilometraje. Los ERPs europeos (Taller GP) cuestan desproporcionado y no se alinean con la norma peruana. El papel, Excel y WhatsApp pierden información y no dejan rastro auditable.

**Fuerzas del cambio:** empujan las fugas de dinero en repuestos, los reclamos sin evidencia, los fines de semana cuadrando libretas y el temor a SUNAT. Frenan el hábito del papelito y la pizarra, los audios de WhatsApp, la resistencia de mecánicos veteranos, el miedo a que sea difícil para "migrantes digitales" y a perder datos si falla la conexión.

## Operating Context

- **Dónde:** patio, bahía, fosa (sin cobertura), mostrador y almacén. Manos ocupadas o con grasa, luz artificial de taller, interrupciones constantes.
- **Dispositivo:** teléfonos Android del personal, a menudo el personal y de gama media.
- **Flujo que recorre la app:** acceso por invitación → asistencia con geocerca GPS → recepción por placa o documento → orden de trabajo y tareas → fotos de recepción → hallazgos y propuestas → proforma y resolución del cliente → ejecución cronometrada → repuestos FIFO → diagnóstico OBD-II → cobro, comprobante, pase de salida y entrega.
- **Costumbres que reemplaza:** la pizarra de órdenes, el talonario de órdenes de trabajo, la libreta, el audio de WhatsApp para cotizar y autorizar.
- **Ecosistema:** el panel web cubre la gestión de escritorio y el pago de la suscripción (Stripe); la app del conductor y el portal de flotas son otros productos.
- **Requisitos:** historias US01–US44 en [docs/01-requirements/user-stories.md](docs/01-requirements/user-stories.md); orden de construcción en [docs/07-roadmap.md](docs/07-roadmap.md).

## Capabilities and Constraints

**Reglas duras** (detalle en `docs/`):

- La UI se habilita por permiso (`Session.has(Permission.X)`). El backend vuelve a validar; ocultar un botón es solo UX.
- Offline-first: red → Room → UI. Solo las acciones que [docs/06-offline-sync.md](docs/06-offline-sync.md) marca como encolables sobreviven sin red; las marcadas "en línea" no se encolan. El usuario siempre debe poder saber si algo quedó pendiente de sincronizar.
- Se muestra el `message` del error del backend; el flujo se decide por el código HTTP.
- Si un endpoint o campo no está en `docs/api/`, no existe. Nada de datos inventados en pantalla.
- Fechas del backend en UTC, mostradas en `America/Lima`. Dinero en soles (S/), con IGV.
- Textos visibles en español, en `strings.xml`.
- Funciones por plan: telemetría OBD-II (`iotTelemetryEnabled`) e informe de salud con IA (`aiDiagnosticsEnabled`) solo aparecen si el plan las incluye.

**Modelo de negocio:** suscripción mensual con acceso al ecosistema completo desde el plan inicial. Bahías ilimitadas en todos los planes.

| Plan | Precio | Límites y qué desbloquea |
| --- | --- | --- |
| Go | S/ 139/mes (S/ 109 anual) | 5 usuarios, 1 sede. Plataforma completa con app móvil; sin telemetría; solo clientes particulares |
| Pro | S/ 269/mes (S/ 219 anual) | 10 usuarios, 1 sede. Fotos y SUNAT ilimitados; telemetría en vivo hasta 5 vehículos; sin informes PDF predictivos |
| Max | S/ 489/mes (S/ 399 anual) | 25 usuarios, 2 sedes, 15 OBD-II. Empresas y flotas, marketplace Atelier Bussiness, hasta 60 informes de salud con IA |
| Enterprise | A medida | Por sedes y consumo; integración con ERPs corporativos |

**Términos del dominio:**

| Término | Significado |
| --- | --- |
| Bahía de servicio | Puesto de trabajo o elevador asignado a un vehículo |
| Fosa | Estructura bajo el nivel del piso para trabajar el chasis sin elevador; suele no tener señal |
| Orden de trabajo | Unidad de servicio de un vehículo, con sus tareas, repuestos y evidencias |
| Proforma | Presupuesto formal que se envía al cliente en PDF |
| Propuesta | Tarea adicional sugerida por un hallazgo durante el trabajo |
| Descargo | Justificación que un colaborador envía por una tardanza o ausencia |
| OBD-II / DTC / PID | Puerto de diagnóstico a bordo / código de falla / parámetro leído en vivo (RPM, temperatura) |
| FIFO por lote | El repuesto más antiguo sale primero, a su costo exacto de compra |
| RMT | Régimen MYPE Tributario de SUNAT |

**Sin decidir o inconsistente en las fuentes:**

- El precio de entrada aparece como S/ 139 en el modelo de negocio y como S/ 149 en una respuesta a objeciones. Se toma S/ 139 hasta que negocio lo confirme.
- El nombre del portal de flotas figura como "Atelier Bussiness" (así, con doble s) en las fuentes.

## Brand Commitments

- **Nombre:** Atelier Workshop. La app móvil es Atelier Workshop Mobile.
- **Símbolo:** el perfil de un mecánico con gorra, lentes de protección y cuello de overol. Activos en [docs/branding/](docs/branding/): isotipo, imagotipo (isotipo en squircle + "Atelier" / "Workshop" a dos líneas) e ícono, en azul y negro.
- **Paleta fija** ([docs/branding/pallette.txt](docs/branding/pallette.txt)): azul `#0071EB`, azul profundo `#031A6B`, azul suave `#69B1FF`, ámbar `#F68B01`, ámbar oscuro `#D87900`, ámbar suave `#FFCD69`, éxito `#3AC530`, error `#FB2C36`, advertencia `#F0B100`, y los neutros de superficie, tarjeta, texto y borde.
- **Tipografía fija (confirmado 2026-11-16):** Satoshi para toda la interfaz; Albert Sans ExtraBold solo para el texto del imagotipo.
- **Tema claro y oscuro (confirmado 2026-11-16):** dos esquemas completos que siguen el ajuste del sistema.
- **Voz:** profesional, honesta, empática; respeta la experiencia del mecánico y el esfuerzo del microempresario. Práctica y directa, sin burocracia ni tecnicismos vacíos. Personalidad: robusta, confiable, transparente, pragmática, cercana al taller peruano.
- **Palabras que sí:** taller automotriz, bahía de servicio, fosa, orden de trabajo, proforma, comprobante electrónico SUNAT, escáner OBD-II, códigos de falla, repuestos por lote, horas hombre efectivas, evidencia fotográfica.
- **Palabras que no:** "aplicativo" (siempre *aplicación* o *software*), carrocería cosmética, taller artesanal, comitear, buildear, jerga informática ajena al lenguaje mecánico.

## Evidence on Hand

- **Activos de marca:** logos SVG/PNG y fuentes (Satoshi en WOFF2, Albert Sans en TTF) en `docs/branding/`. Android no carga WOFF2: Satoshi debe entrar al proyecto como TTF u OTF.
- **Lenguaje real de los usuarios** (entrevistas de campo):
  - "Los clientes desconfían de entrada; piensan que uno les inventa fallas mecánicas o les cambia repuestos buenos por viejos."
  - "En el foso no entra la señal del teléfono y tengo que subir a cada rato para coordinar o buscar información."
  - "Tengo que andar persiguiendo al mecánico para saber cuánto tiempo real le metió a la reparación y poder cobrar lo justo."
  - "Mi dolor de cabeza es cuando … se demoran con los repuestos y se me acumulan los carros una semana."
- **Testimonios de validación:** Marcelo Silva Ramos, propietario de taller (Chorrillos): "Si la tecnología me ayuda a que el cliente entienda la falla con claridad y no se me tranquen los trabajos en el patio, el negocio avanza seguro." César Montero Vega, técnico mecatrónico: "…que el escáner enlace al instante y que las fotos de la reparación queden registradas para que nadie dude de la chamba realizada."
- **Estado real:** fase de validación académica y MVP, con validación de campo en talleres de Lima (Chorrillos, Surquillo, San Juan de Miraflores, Los Olivos).
- **No fabricar:** las cifras de mejora (−50 % de tiempo administrativo, −40 % en recepción, −75 % en liquidación, 85 % de ocupación de bahías) son **proyecciones**, no resultados medidos. No hay clientes de pago, logos de clientes ni benchmarks que citar.

## Product Principles

1. **El taller manda.** La app se adapta al patio, la fosa y el mostrador: una mano, pocos toques, sin depender de la señal.
2. **Primero lo que espera una decisión.** Antes que cualquier indicador, el dueño ve qué está detenido esperándolo a él o al cliente.
3. **Todo con respaldo.** Cada cobro, hora y repuesto se puede rastrear hasta una foto, una lectura o un registro. La app nunca muestra un dato que no venga del backend.
4. **Honesta con el estado.** Guardado, pendiente de sincronizar, fallido o bloqueado por plan o permiso: siempre se dice cuál, con las palabras del taller.
5. **Respeto por el oficio.** Ni infantiliza ni vigila: registra el trabajo para reconocerlo y defenderlo.

## Accessibility & Inclusion

- Parte del personal es "migrante digital": etiquetas visibles siempre, vocabulario del taller, flujos cortos y sin gestos ocultos como único camino.
- Uso con una mano, de pie y con manos sucias: objetivos táctiles de 48 dp o más y acciones principales al alcance del pulgar.
- Legible bajo fluorescente y a contraluz del portón: contraste WCAG 2.2 AA como mínimo en claro y oscuro (el estándar formal es una decisión de diseño adoptada, no un requisito del cliente).
- El estado nunca depende solo del color: lleva ícono o texto.
- Respeta el tamaño de fuente del sistema y el ajuste de "quitar animaciones".
