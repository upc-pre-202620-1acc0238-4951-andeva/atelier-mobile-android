# Historias de usuario — App móvil Atelier

Versión en Markdown limpio de la sección 2.4.1 del informe (`24-requirements-specification.md`), sin tablas LaTeX, para que la lean igual de bien una persona y una IA.

## Cómo leer este documento

- Cada **épica** (`EPxx`) agrupa historias de un área del negocio.
- Cada **historia** (`USxx`) sigue la forma *Como … quiero … para …* e incluye sus **criterios de aceptación** en Gherkin (`Dado que / Cuando / Entonces / Y`).
- **Sprint** y **story points** vienen del Product Backlog (sección 2.4.3 del informe).
- El endpoint y el permiso que cubren cada escenario están en [../03-traceability.md](../03-traceability.md).

## Alcance de la app móvil

| Incluido en la app de staff (este repo) | Fuera de alcance |
| --- | --- |
| EP01–EP09 · US01–US44 | EP11 · US45–US48 (landing web; ver apéndice) |
| | App del conductor/cliente final (por ejemplo US44 E3, conciliación cuando el cliente crea su cuenta móvil) |
| | Configuración SaaS de pago con Stripe (se hace en la web) |

## Usuarios (actores)

| Usuario | Rol de fábrica más cercano |
| --- | --- |
| Dueño de taller | `ROLE_WORKSHOP_OWNER` |
| Administrador de taller | `ROLE_WORKSHOP_ADMINISTRATOR` |
| Mecánico jefe | `ROLE_CHIEF_MECHANIC` |
| Asesor de servicio | `ROLE_SERVICE_ADVISOR` |
| Recepcionista | `ROLE_RECEPTIONIST` |
| Técnico mecánico | `ROLE_MECHANIC` |
| Encargado de inventario | `ROLE_INVENTORY_MANAGER` |
| Cajero | `ROLE_CASHIER` |

La app decide qué mostrar según los **permisos** del JWT, no según el nombre del rol (ver [../02-roles-and-permissions.md](../02-roles-and-permissions.md)).

## Índice

- **EP01 — Autenticación y Control de Acceso Móvil**
  - [US01 — Registro y vinculación de colaborador mediante invitación corporativa](#us01)
  - [US02 — Verificación de identidad y correo electrónico mediante código OTP](#us02)
  - [US03 — Inicio de sesión corporativo y contextualización de sede de trabajo](#us03)
  - [US04 — Cierre de sesión seguro y revocación de credenciales en el dispositivo móvil](#us04)
  - [US05 — Restablecimiento de credenciales de acceso mediante token de verificación](#us05)
- **EP02 — Control de Asistencia Satelital y Turnos**
  - [US06 — Marcación de ingreso laboral con validación de geocerca GPS satelital](#us06)
  - [US08 — Marcación de salida y liquidación de jornada laboral efectiva](#us08)
  - [US09 — Consulta de programación semanal de turnos y márgenes de tolerancia](#us09)
  - [US10 — Emisión y seguimiento de descargos justificatorios de tardanza](#us10)
- **EP03 — Recepción Pericial e Inspección en Patio**
  - [US11 — Búsqueda y vinculación de cliente y vehículo por placa o documento](#us11)
  - [US12 — Apertura de orden de trabajo y parametrización de tareas iniciales](#us12)
  - [US13 — Registro pericial de evidencias fotográficas de recepción vehicular](#us13)
  - [US14 — Generación de propuesta de tarea adicional por hallazgo en foso](#us14)
  - [US15 — Notificación y aprobación de propuesta técnica con el cliente](#us15)
  - [US44 — Alta y afiliación de clientes en taller según tenencia previa de cuenta en la plataforma](#us44)
- **EP04 — Diagnóstico Electrónico y Telemetría OBD-II**
  - [US16 — Enlace inalámbrico y sincronización de escáner OBD-II en bahía](#us16)
  - [US17 — Extracción y decodificación de códigos de falla computarizados](#us17)
  - [US18 — Monitoreo dinámico de parámetros telemétricos en tiempo real](#us18)
  - [US19 — Almacenamiento local resiliente y sincronización diferida en fosa](#us19)
  - [US20 — Borrado y verificación de restablecimiento de fallas en la computadora](#us20)
  - [US21 — Generación de informe pericial predictivo con inteligencia artificial y exportación en PDF](#us21)
- **EP05 — Presupuestos y Cotizaciones de Mantenimiento**
  - [US22 — Cómputo automático en cascada de costos de tareas e impuestos](#us22)
  - [US23 — Imputación de repuestos a tareas y supervisión administrativa](#us23)
  - [US24 — Generación y remisión digital de proforma comercial en PDF](#us24)
  - [US25 — Formalización de resolución de presupuesto tras comunicación externa](#us25)
- **EP06 — Ejecución de Tareas en Bahía de Servicio**
  - [US26 — Consulta y priorización de tareas mecánicas asignadas en bahía](#us26)
  - [US27 — Cronometraje de labor efectiva, pausas operativas y sincronización con turnos laborales](#us27)
  - [US28 — Registro fotográfico pericial de desmontaje y montaje de piezas en foso](#us28)
  - [US29 — Solicitud y validación administrativa de suspensión por pieza defectuosa indispensable](#us29)
  - [US30 — Cierre técnico de tarea con cómputo de horas hombre efectivas](#us30)
- **EP07 — Abastecimiento y Descargo de Repuestos FIFO**
  - [US31 — Búsqueda y consulta de disponibilidad de repuestos en catálogo](#us31)
  - [US32 — Descargo contable y físico automatizado mediante regla estricta FIFO por lote](#us32)
  - [US33 — Reincorporación lógica de materiales a inventario por remoción en tarea técnica](#us33)
  - [US34 — Disparo y desactivación de alertas automáticas de reposición por stock mínimo](#us34)
  - [US35 — Imputación automática de precios de venta de catálogo y conciliación de margen FIFO](#us35)
- **EP08 — Facturación Electrónica y Cierre de Servicio**
  - [US36 — Liquidación económica automática en cascada de la orden de trabajo](#us36)
  - [US37 — Emisión de comprobantes tributarios electrónicos UBL 2.1 ante SUNAT vía PSE](#us37)
  - [US38 — Recaudación y amortización de pagos multi-medio en mostrador y patio](#us38)
  - [US39 — Transición a orden pagada, emisión de pase de salida vehicular y entrega en patio](#us39)
- **EP09 — Monitoreo Operativo de Patio y Gestión Gerencial**
  - [US40 — Supervisión y reasignación de bahías de servicio en tiempo real](#us40)
  - [US41 — Exportación de reporte de flujo de caja en PDF para auditoría contable](#us41)
  - [US42 — Configuración de información fiscal y series SUNAT con PSE administrado por la plataforma](#us42)
  - [US43 — Descarga y remisión de informe pericial de salud vehicular en PDF](#us43)

## EP01 — Autenticación y Control de Acceso Móvil

- **Área:** Seguridad e Identidad (IAM)
- **Rol:** Colaborador del taller
- **Prioridad:** Alta
- **Alcance:** Gestión de credenciales corporativas, validación de tokens JWT y selección de sucursal de trabajo con aislamiento multi-inquilino.

> **Como** colaborador del taller, **quiero** registrarme mediante invitación corporativa, validar mi correo con código OTP, autenticarme con mis credenciales y gestionar mi sesión de forma segura, **para** acceder a las operaciones del taller bajo aislamiento multi-inquilino y proteger la información operativa

<a id="us01"></a>

### US01 — Registro y vinculación de colaborador mediante invitación corporativa

**Usuario:** Colaborador del taller · **Prioridad:** Alta · **Épica:** EP01 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** colaborador invitado a laborar en el taller, **quiero** registrar mis datos de identidad y crear mi contraseña a partir del enlace de invitación recibido por correo electrónico, **para** incorporarme a la sucursal de trabajo asignada con el rol operativo autorizado

**Criterios de aceptación**

*Escenario 1: Registro exitoso de colaborador invitado*

- **Dado que** el colaborador recibe un enlace con un token de invitación vigente emitido por la administración del taller
- **Cuando** ingresa sus datos de identidad, define su contraseña corporativa y confirma el registro
- **Entonces** el sistema valida la autenticidad del token, crea la cuenta del colaborador vinculándola al taller y sucursal emisora con el rol asignado
- **Y** marca la invitación como aceptada para impedir su reutilización

*Escenario 2: Bloqueo de registro por membresía activa previa en otro taller*

- **Dado que** el usuario mantiene una membresía laboral activa en otro taller automotriz dentro de la plataforma Atelier
- **Cuando** intenta aceptar la invitación para incorporarse a una nueva organización
- **Entonces** el sistema rechaza la solicitud de vinculación por colisión de exclusividad laboral
- **Y** notifica que debe gestionar su desvinculación formal de su taller actual antes de integrarse a una nueva entidad

*Escenario 3: Rechazo de invitación vencida o revocada*

- **Dado que** el token de invitación ha superado su período de vigencia de cuarenta y ocho horas o fue revocado por el administrador
- **Cuando** el destinatario intenta completar el formulario de registro
- **Entonces** el sistema invalida la solicitud
- **Y** notifica la caducidad del enlace requiriendo la emisión de una nueva invitación corporativa

<a id="us02"></a>

### US02 — Verificación de identidad y correo electrónico mediante código OTP

**Usuario:** Colaborador del taller · **Prioridad:** Alta · **Épica:** EP01 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** colaborador en proceso de registro institucional, **quiero** ingresar el código numérico OTP de seis dígitos despachado a mi correo electrónico institucional, **para** certificar la titularidad de mi cuenta y habilitar mi acceso operativo a la aplicación móvil

**Criterios de aceptación**

*Escenario 1: Validación exitosa y activación de cuenta*

- **Dado que** el sistema ha emitido un código numérico de seis dígitos con vigencia de diez minutos al correo del usuario
- **Cuando** el usuario suministra el código exacto recibido
- **Entonces** el sistema valida la correspondencia del código OTP, registra el correo electrónico como verificado
- **Y** activa la cuenta de usuario habilitándola para iniciar sesión en la aplicación móvil

*Escenario 2: Rechazo de código OTP incorrecto o caducado*

- **Dado que** el código suministrado no coincide con el emitido o ha superado su ventana temporal de validez
- **Cuando** se procesa la solicitud de verificación
- **Entonces** el sistema rechaza la activación manteniendo la cuenta en estado pendiente
- **Y** notifica la discrepancia permitiendo solicitar la reemisión de un nuevo código OTP

<a id="us03"></a>

### US03 — Inicio de sesión corporativo y contextualización de sede de trabajo

**Usuario:** Colaborador del taller · **Prioridad:** Alta · **Épica:** EP01 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** colaborador registrado del taller, **quiero** autenticarme con mi correo institucional y contraseña en la aplicación móvil, **para** obtener un token de sesión seguro y operar en el contexto de mi sede laboral asignada

**Criterios de aceptación**

*Escenario 1: Autenticación exitosa y carga de contexto operativo*

- **Dado que** el colaborador cuenta con credenciales válidas y membresía activa en una sucursal del taller
- **Cuando** envía su correo institucional y contraseña para iniciar sesión
- **Entonces** el sistema valida las credenciales, emite un token de sesión JWT con los permisos de su rol
- **Y** restringe las consultas de bahías, vehículos y órdenes al ámbito exclusivo de su sucursal de adscripción

*Escenario 2: Denegación de acceso por credenciales no válidas*

- **Dado que** se ingresa una contraseña incorrecta o un correo no registrado en el sistema
- **Cuando** se solicita la autenticación del usuario
- **Entonces** el sistema deniega el acceso sin emitir credenciales de sesión
- **Y** registra el intento fallido notificando la inconsistencia de los datos suministrados

<a id="us04"></a>

### US04 — Cierre de sesión seguro y revocación de credenciales en el dispositivo móvil

**Usuario:** Colaborador del taller · **Prioridad:** Alta · **Épica:** EP01 · **Sprint:** Sprint 3 · **Story points:** 1

**Como** colaborador que culmina su labor o entrega el terminal móvil, **quiero** finalizar voluntariamente mi sesión de trabajo en el dispositivo móvil, **para** revocar los tokens locales e impedir accesos no autorizados a la información operativa del taller

**Criterios de aceptación**

*Escenario 1: Cierre voluntario de turno de trabajo*

- **Dado que** el usuario mantiene una sesión autenticada activa en el dispositivo móvil
- **Cuando** solicita formalmente el término de su sesión
- **Entonces** el sistema invalida el token de acceso vigente en el almacén local del dispositivo
- **Y** restablece el estado de la aplicación requiriendo una nueva autenticación para cualquier interacción posterior

*Escenario 2: Expiración automática por caducidad de token*

- **Dado que** el token de sesión ha superado su período de validez temporal predefinido sin renovación
- **Cuando** la aplicación móvil intenta realizar una consulta operativa al backend
- **Entonces** el sistema detecta la caducidad del token, revoca los permisos en memoria
- **Y** exige la re-autenticación del usuario para garantizar la seguridad transaccional

<a id="us05"></a>

### US05 — Restablecimiento de credenciales de acceso mediante token de verificación

**Usuario:** Colaborador del taller · **Prioridad:** Media · **Épica:** EP01 · **Sprint:** Sprint 3 · **Story points:** 2

**Como** colaborador que ha olvidado su clave de acceso corporativa, **quiero** solicitar el restablecimiento de mi contraseña corporativa mediante un token temporal enviado a mi correo institucional, **para** recuperar el acceso a mis funciones operativas ante olvido de clave sin comprometer la seguridad

**Criterios de aceptación**

*Escenario 1: Generación exitosa de solicitud de recuperación*

- **Dado que** el colaborador ingresa un correo electrónico corporativo registrado y activo en el sistema
- **Cuando** solicita la recuperación de su contraseña de acceso
- **Entonces** el sistema genera un token de verificación criptográfico con vigencia temporal limitada de quince minutos
- **Y** despacha las instrucciones de restablecimiento al correo institucional del colaborador

*Escenario 2: Rechazo de token expirado o adulterado*

- **Dado que** se intenta ingresar una nueva contraseña utilizando un token cuya vigencia ha expirado o cuyos caracteres son inválidos
- **Cuando** el sistema procesa la confirmación de cambio de clave
- **Entonces** la operación es rechazada conservando inalterada la contraseña previa
- **Y** se notifica la invalidez del token requiriendo una nueva solicitud formal

## EP02 — Control de Asistencia Satelital y Turnos

- **Área:** Gestión de Personal (HR)
- **Rol:** Técnico mecánico
- **Prioridad:** Alta
- **Alcance:** Marcación laboral de ingreso y salida validada por geocerca GPS satelital con fórmula de Haversine y consulta de turnos programados.

> **Como** técnico mecánico, **quiero** registrar mi asistencia de ingreso y salida validando mi ubicación satelital dentro del perímetro del taller y consultar mi cronograma de trabajo, **para** acreditar mi presencia física en la sucursal asignada y dar seguimiento a mis turnos laborales

<a id="us06"></a>

### US06 — Marcación de ingreso laboral con validación de geocerca GPS satelital

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP02 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** técnico mecánico asignado a la jornada matutina, **quiero** registrar mi ingreso laboral validando mis coordenadas de posicionamiento satelital respecto a la sede asignada, **para** certificar mi puntualidad y presencia física dentro de las instalaciones del taller automotriz

**Criterios de aceptación**

*Escenario 1: Marcación puntual dentro del margen de tolerancia y perímetro de sede*

- **Dado que** el técnico automotriz se sitúa dentro del radio de tolerancia de cien metros respecto al centroide de la sucursal y arriba dentro del margen de gracia de quince minutos de su turno
- **Cuando** registra su entrada laboral en el sistema móvil
- **Entonces** el sistema valida las coordenadas mediante la fórmula de Haversine, registra la marca temporal con calificación de puntualidad
- **Y** vincula el asiento de asistencia al expediente laboral y al turno asignado

*Escenario 2: Marcación con tardanza fuera de la ventana de gracia*

- **Dado que** el colaborador se encuentra dentro de la geocerca pero su registro supera el margen de gracia establecido para su turno
- **Cuando** asienta su marcación de entrada en la jornada
- **Entonces** el sistema califica el registro bajo estado de tardanza, computa los minutos de demora acumulados
- **Y** requiere el ingreso de un descargo justificatorio para conocimiento de la administración

*Escenario 3: Bloqueo de marcación duplicada en la misma jornada*

- **Dado que** el colaborador ya cuenta con una marcación de ingreso activa y sin salida en la jornada en curso
- **Cuando** intenta registrar un nuevo ingreso sin haber liquidado el turno previo
- **Entonces** el sistema deniega el duplicado notificando la vigencia de una jornada abierta
- **Y** preserva intacto el registro temporal original

<a id="us08"></a>

### US08 — Marcación de salida y liquidación de jornada laboral efectiva

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP02 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** técnico mecánico que culmina su jornada operativa diaria, **quiero** registrar mi salida al término de mis actividades en el taller, **para** cerrar formalmente el turno diario y liquidar las horas hombre efectivamente laboradas

**Criterios de aceptación**

*Escenario 1: Cierre conforme de jornada laboral ordinaria*

- **Dado que** el técnico automotriz mantiene un ingreso activo en la jornada y se ubica dentro del perímetro de la sede
- **Cuando** registra su salida al culminar el horario del turno
- **Entonces** el sistema graba la marca temporal de salida, calcula el tiempo neto trabajado en horas y minutos
- **Y** asienta el registro de asistencia como completado en el expediente del colaborador

*Escenario 2: Detección y registro de tiempo extraordinario en faena de bahía*

- **Dado que** el técnico concluye la atención mecánica de un vehículo superando en más de treinta minutos la hora de salida de su turno
- **Cuando** efectúa la marcación de término de faena
- **Entonces** el sistema desglosa las horas de jornada regular y el excedente en calidad de tiempo extraordinario
- **Y** genera una notificación de sobretiempo para liquidación en la nómina periódica

*Escenario 3: Rechazo de salida sin marcación de ingreso previa*

- **Dado que** no existe una marcación de entrada registrada para la fecha en curso
- **Cuando** el colaborador intenta marcar la salida laboral
- **Entonces** el sistema rechaza la transacción indicando la inexistencia de una jornada abierta
- **Y** solicita coordinar la regularización de la omisión con la jefatura de taller

<a id="us09"></a>

### US09 — Consulta de programación semanal de turnos y márgenes de tolerancia

**Usuario:** Técnico mecánico · **Prioridad:** Media · **Épica:** EP02 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** técnico mecánico que planifica sus actividades laborales semanales, **quiero** consultar mi calendario de turnos asignados, sucursales de guardia y márgenes de tolerancia, **para** organizar con anticipación mis jornadas de trabajo y evitar penalizaciones por impuntualidad

**Criterios de aceptación**

*Escenario 1: Consulta de cronograma semanal de actividades y descansos*

- **Dado que** la jefatura de taller ha publicado la planificación operativa del ciclo vigente
- **Cuando** el técnico consulta su calendario de asignaciones
- **Entonces** el sistema presenta los turnos programados de lunes a domingo, detallando la sede correspondiente y las horas de entrada y salida
- **Y** resalta los días de descanso obligatorio concedidos

*Escenario 2: Advertencia preventiva de inicio de turno y ventana de gracia*

- **Dado que** restan menos de treinta minutos para el inicio del turno asignado al colaborador
- **Cuando** el sistema evalúa la proximidad de la jornada laboral
- **Entonces** despacha un recordatorio operativo indicando el horario de inicio y los quince minutos de gracia tolerados
- **Y** orienta al técnico a presentarse dentro del radio perimétrico del taller

<a id="us10"></a>

### US10 — Emisión y seguimiento de descargos justificatorios de tardanza

**Usuario:** Técnico mecánico · **Prioridad:** Media · **Épica:** EP02 · **Sprint:** Sprint 2 · **Story points:** 1

**Como** técnico mecánico que arriba a la sede con retraso involuntario, **quiero** sustentar digitalmente los motivos de demoras imprevistas y dar seguimiento a su evaluación por administración, **para** regularizar mi récord de puntualidad y mitigar deducciones salariales injustificadas

**Criterios de aceptación**

*Escenario 1: Registro formal de motivo de descargo en marcación tardía*

- **Dado que** la marcación de ingreso fue calificada bajo condición de tardanza
- **Cuando** el técnico ingresa la fundamentación de su demora en el registro correspondiente
- **Entonces** el sistema almacena el motivo justificatorio vinculado al identificador de la asistencia
- **Y** remite la solicitud de exoneración a la bandeja de revisión de la jefatura de taller

*Escenario 2: Notificación del dictamen emitido por la administración*

- **Dado que** la jefatura de taller ha revisado y dictaminado la justificación presentada
- **Cuando** se actualiza el expediente de asistencia en el servidor central
- **Entonces** el sistema actualiza el estado del registro a justificado o desestimado según corresponda
- **Y** notifica al colaborador la resolución con la marca temporal y el identificador del supervisor responsable

## EP03 — Recepción Pericial e Inspección en Patio

- **Área:** Operaciones de Taller y Clientes (CRM/MRO)
- **Rol:** Recepcionista y asesor de servicio
- **Prioridad:** Alta
- **Alcance:** Registro de cliente y unidad por placa o VIN, checklist de peritaje visual, captura de evidencias fotográficas y apertura de la orden de trabajo.

> **Como** recepcionista y asesor de servicio, **quiero** identificar al cliente y vehículo, aperturar la orden de trabajo con sus tareas técnicas, capturar fotos periciales de recepción y gestionar propuestas de reparación sobrevenidas, **para** formalizar el ingreso a la sucursal, respaldar el estado inicial del automóvil y autorizar trabajos adicionales con evidencia probatoria

<a id="us11"></a>

### US11 — Búsqueda y vinculación de cliente y vehículo por placa o documento

**Usuario:** Recepcionista · **Prioridad:** Alta · **Épica:** EP03 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** recepcionista en el counter de atención inicial del taller, **quiero** consultar o registrar los datos de identidad del cliente y la ficha técnica del vehículo mediante su placa o documento fiscal, **para** recuperar el historial de atenciones previas o incorporar una nueva unidad al catálogo del taller

**Criterios de aceptación**

*Escenario 1: Identificación y carga de unidad recurrente*

- **Dado que** la placa vehicular o el documento fiscal del propietario ya existen registrados en la plataforma del taller
- **Cuando** el personal de atención efectúa la búsqueda en el módulo de recepción
- **Entonces** el sistema recupera la ficha completa del cliente y las características técnicas del automóvil
- **Y** vincula automáticamente la información al nuevo registro de atención

*Escenario 2: Alta de nuevo cliente y unidad en primer ingreso*

- **Dado que** la placa consultada no cuenta con antecedentes registrados en la empresa
- **Cuando** el usuario registra los datos del cliente y los datos técnicos de la unidad vehicular
- **Entonces** el sistema valida la consistencia de los datos suministrados
- **Y** crea las fichas correspondientes en el espacio de trabajo del taller dejándolas disponibles para la orden de servicio

<a id="us12"></a>

### US12 — Apertura de orden de trabajo y parametrización de tareas iniciales

**Usuario:** Asesor de servicio · **Prioridad:** Alta · **Épica:** EP03 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** asesor de servicio responsable de la recepción e inspección técnica en patio, **quiero** crear la orden de trabajo asentando el kilometraje de odómetro, el resumen diagnóstico inicial y programar las tareas técnicas a realizar, **para** formalizar el ingreso del vehículo a la sucursal y delegar las faenas a los mecánicos responsables

**Criterios de aceptación**

*Escenario 1: Creación de orden de trabajo con kilometraje y diagnóstico*

- **Dado que** el vehículo y el cliente han sido seleccionados para el servicio
- **Cuando** el administrador asienta el kilometraje actual del odómetro y el resumen de síntomas manifestados por el propietario
- **Entonces** el sistema genera el correlativo secuencial único de la orden de trabajo vinculándola a la sucursal
- **Y** asocia la cita previa correspondiente en caso de haber sido reservada con anterioridad

*Escenario 2: Desglose de tareas técnicas y asignación a mecánico*

- **Dado que** la orden de trabajo ha sido inicializada en el sistema
- **Cuando** el administrador añade las tareas mecánicas requeridas especificando el servicio base, la bahía de trabajo y el mecánico asignado
- **Entonces** el sistema instancia cada tarea en estado pendiente con su precio de mano de obra y horas estimadas
- **Y** habilita la orden para el inicio de las labores en el taller

<a id="us13"></a>

### US13 — Registro pericial de evidencias fotográficas de recepción vehicular

**Usuario:** Asesor de servicio · **Prioridad:** Media · **Épica:** EP03 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** asesor de servicio que efectúa la inspección pericial 360° en patio, **quiero** registrar evidencias fotográficas del estado exterior de la carrocería del vehículo en el patio y vincularlas a la orden de trabajo, **para** blindar legalmente a la empresa constatando averías o raspones preexistentes antes de iniciar el servicio

**Criterios de aceptación**

*Escenario 1: Carga de evidencias fotográficas de recepción en patio*

- **Dado que** el vehículo se encuentra en el área de recepción y cuenta con una orden de trabajo aperturada
- **Cuando** el personal captura fotografías de los paneles exteriores del vehículo agregando una descripción del daño observado
- **Entonces** el sistema almacena las imágenes vinculándolas unívocamente a la cabecera de la orden de trabajo
- **Y** registra la marca temporal exacta de la captura pericial

*Escenario 2: Consulta del expediente fotográfico de recepción*

- **Dado que** una orden de trabajo posee registros fotográficos periciales asociados
- **Cuando** se consulta el detalle pericial de la unidad en servicio
- **Entonces** el sistema presenta la galería ordenada de imágenes con sus notas descriptivas
- **Y** certifica el estado físico en el que el vehículo fue recibido en el establecimiento

<a id="us14"></a>

### US14 — Generación de propuesta de tarea adicional por hallazgo en foso

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP03 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** técnico mecánico asignado a la ejecución de una tarea en foso, **quiero** registrar una propuesta de tarea adicional con nivel de severidad y fotografía probatoria al hallar un defecto imprevisto en bahía, **para** reportar al administrador la necesidad de una reparación adicional no identificada en el diagnóstico inicial

**Criterios de aceptación**

*Escenario 1: Creación conforme de propuesta con evidencia de daño*

- **Dado que** el técnico automotriz se encuentra ejecutando una labor en bahía y descubre una fuga o rotura no prevista en la orden
- **Cuando** registra la propuesta de tarea indicando el servicio sugerido, calificación de severidad y adjuntando la fotografía del desperfecto
- **Entonces** el sistema crea la propuesta técnica vinculada a la orden de trabajo en estado pendiente de revisión
- **Y** emite una notificación de advertencia en el panel de supervisión del administrador del taller

*Escenario 2: Denegación de propuesta ante omisión de evidencia fotográfica*

- **Dado que** el técnico intenta despachar una propuesta técnica calificada con severidad crítica
- **Cuando** envía el formulario sin adjuntar la captura fotográfica del componente dañado
- **Entonces** el sistema bloquea el registro exigiendo la evidencia probatoria obligatoria
- **Y** orienta al colaborador a capturar la imagen de la falla antes de proceder

<a id="us15"></a>

### US15 — Notificación y aprobación de propuesta técnica con el cliente

**Usuario:** Asesor de servicio · **Prioridad:** Alta · **Épica:** EP03 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** asesor de servicio que gestiona la comunicación comercial con el cliente, **quiero** remitir la propuesta técnica con su respaldo fotográfico al cliente y asentar su veredicto de aprobación o rechazo, **para** obtener la conformidad del propietario e incorporar la labor autorizada como tarea activa en la orden de trabajo

**Criterios de aceptación**

*Escenario 1: Aprobación de propuesta y adición automática a la orden de trabajo*

- **Dado que** el cliente ha revisado la notificación digital con la evidencia fotográfica de la avería y autoriza la reparación complementaria
- **Cuando** el administrador asienta la resolución favorable en el sistema
- **Entonces** el sistema actualiza la propuesta a estado aprobado
- **Y** genera automáticamente una nueva tarea técnica en estado pendiente dentro de la orden de trabajo recalculando el presupuesto proyectado

*Escenario 2: Desestimación de propuesta adicional por parte del cliente*

- **Dado que** el cliente opta por no autorizar la intervención correctiva sobrevenida
- **Cuando** el administrador registra la desestimación ingresando las observaciones del propietario
- **Entonces** el sistema actualiza la propuesta a estado rechazado conservando el historial técnico y la imagen
- **Y** mantiene inalterada la estructura de tareas y costos de la orden de trabajo

<a id="us44"></a>

### US44 — Alta y afiliación de clientes en taller según tenencia previa de cuenta en la plataforma

**Usuario:** Recepcionista · **Prioridad:** Alta · **Épica:** EP03 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** recepcionista en el área de bienvenida y atención al cliente, **quiero** que el sistema distinga entre clientes presenciales nuevos y conductores que ya cuentan con cuenta en la aplicación móvil al ingresar su correo electrónico, **para** registrar desde cero a los clientes presenciales (walk-in) junto con sus vehículos, o afiliar de forma inmediata a los usuarios preexistentes vinculando automáticamente sus autos ya registrados sin duplicar información

**Criterios de aceptación**

*Escenario 1: Afiliación rápida de cliente con cuenta previa en la aplicación móvil*

- **Dado que** un conductor ya cuenta con una cuenta activa en la plataforma y tiene registrados sus vehículos en la aplicación móvil
- **Cuando** acude al taller y el asesor de servicio ingresa su correo electrónico para darlo de alta en la cartera del taller
- **Entonces** el sistema reconoce que el usuario ya existe en la plataforma
- **Y** afilia al cliente al taller vinculando automáticamente todos los vehículos que ya tenía registrados en su cuenta móvil, dejándolos disponibles en el taller sin requerir que el asesor vuelva a digitar sus datos técnicos

*Escenario 2: Registro completo de cliente presencial sin cuenta previa (caso walk-in)*

- **Dado que** un conductor acude al taller y no cuenta con una cuenta registrada en la plataforma
- **Cuando** el asesor de servicio ingresa su correo electrónico en el módulo de recepción y el sistema confirma que no existe una cuenta previa
- **Entonces** el sistema habilita el formulario completo de registro presencial para capturar los datos del cliente y los datos de sus vehículos
- **Y** crea la ficha comercial del cliente en el taller con sus automóviles correspondientes

*Escenario 3: Conciliación automática cuando un cliente presencial crea posteriormente su cuenta móvil*

- **Dado que** un cliente fue registrado en el taller bajo la modalidad presencial con su correo electrónico y sus vehículos
- **Cuando** tiempo después descarga la aplicación móvil y se registra utilizando ese mismo correo electrónico
- **Entonces** el sistema detecta la coincidencia con la ficha registrada previamente en el taller
- **Y** vincula automáticamente su nueva cuenta móvil a los vehículos ya registrados, mostrándolos en su garaje digital con todo su historial de servicio acumulado

## EP04 — Diagnóstico Electrónico y Telemetría OBD-II

- **Área:** Telemetría Vehicular IoT
- **Rol:** Técnico mecánico
- **Prioridad:** Alta
- **Alcance:** Enlace inalámbrico con escáner OBD-II, lectura de parámetros en tiempo real, extracción de códigos DTC y registro local offline en fosa.

> **Como** técnico mecánico, **quiero** enlazar el escáner telemático al puerto OBD-II, leer códigos DTC, monitorear sensores en vivo y generar reportes predictivos con inteligencia artificial en un período seleccionado, **para** identificar fallas con precisión instrumental, contrastar las recomendaciones del modelo con mi criterio pericial y emitir diagnósticos en PDF para el cliente

<a id="us16"></a>

### US16 — Enlace inalámbrico y sincronización de escáner OBD-II en bahía

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP04 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** técnico mecánico asignado al diagnóstico automotriz en bahía, **quiero** conectar la aplicación móvil al adaptador telemático acoplado en el puerto OBD-II del automóvil, **para** habilitar el canal de comunicación bidireccional con la unidad de control del motor

**Criterios de aceptación**

*Escenario 1: Emparejamiento inalámbrico exitoso con la computadora del auto*

- **Dado que** el adaptador telemático se encuentra encendido en el puerto OBD-II y el vehículo mantiene el encendido en contacto
- **Cuando** el técnico inicia la vinculación desde el dispositivo móvil
- **Entonces** el sistema establece el enlace seguro mediante comandos de inicialización estándar
- **Y** confirma la lectura del protocolo de comunicación vehicular y el número de identificación del chasis

*Escenario 2: Notificación ante interrupción de señal o falta de contacto*

- **Dado que** el conmutador de encendido del vehículo se encuentra apagado o el adaptador experimenta una caída de tensión
- **Cuando** se intenta iniciar el enlace de diagnóstico
- **Entonces** el sistema notifica la ausencia de respuesta por parte de la computadora automotriz
- **Y** orienta al técnico a comprobar la alimentación eléctrica del conector y la posición del interruptor de encendido

<a id="us17"></a>

### US17 — Extracción y decodificación de códigos de falla computarizados

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP04 · **Sprint:** Sprint 3 · **Story points:** 2

**Como** técnico mecánico que efectúa el escaneo computarizado de una unidad, **quiero** interrogar la memoria de averías de la unidad de control vehicular para extraer los códigos de error almacenados y sus datos de cuadro congelado, **para** determinar los circuitos y actuadores mecánicos que originaron el encendido del testigo de fallo de motor

**Criterios de aceptación**

*Escenario 1: Detección y decodificación de anomalías electrónicas*

- **Dado que** la computadora del motor mantiene averías registradas en su memoria no volátil
- **Cuando** el técnico ejecuta el escaneo pericial de códigos de diagnóstico
- **Entonces** el sistema extrae los identificadores de falla alfanuméricos, su descripción normalizada y las variables motrices capturadas en el instante del fallo
- **Y** los asocia automáticamente al expediente de diagnóstico de la orden de trabajo activa

*Escenario 2: Confirmación de sistema motriz sin registros de avería*

- **Dado que** el vehículo no presenta desperfectos almacenados ni testigos de alerta activos
- **Cuando** se finaliza la secuencia de interrogación electrónica
- **Entonces** el sistema certifica la ausencia de códigos de anomalía en la memoria de la unidad de control
- **Y** asienta el resultado conforme en el registro de peritaje

<a id="us18"></a>

### US18 — Monitoreo dinámico de parámetros telemétricos en tiempo real

**Usuario:** Técnico mecánico · **Prioridad:** Media · **Épica:** EP04 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** técnico mecánico que monitorea el comportamiento de los sensores del motor, **quiero** supervisar flujos continuos de revoluciones de motor, temperatura de refrigerante, presión de admisión y voltaje eléctrico en la unidad intervenida, **para** analizar el comportamiento térmico y dinámico del propulsor bajo condiciones de prueba

**Criterios de aceptación**

*Escenario 1: Recepción continua y cálculo de métricas en ralentí y marcha*

- **Dado que** el motor del vehículo se encuentra en operación de prueba en la bahía de servicio
- **Cuando** el técnico activa la lectura en tiempo real de los canales de telemetría seleccionados
- **Entonces** el sistema procesa periódicamente las tramas de datos actualizando los valores numéricos instantáneos
- **Y** resalta con indicadores de precaución cualquier lectura que rebase los umbrales nominales de operación del fabricante

*Escenario 2: Congelamiento de datos instantáneos para análisis pericial*

- **Dado que** se observa una oscilación anómala en la temperatura o en el voltaje del alternador durante la aceleración
- **Cuando** el técnico instruye la fijación temporal de la muestra telemétrica
- **Entonces** el sistema congela el conjunto de parámetros instantáneos
- **Y** permite vincular la instantánea de datos como anexo técnico probatorio de la orden de trabajo

<a id="us19"></a>

### US19 — Almacenamiento local resiliente y sincronización diferida en fosa

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP04 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** técnico mecánico que labora en foso con conectividad inalámbrica inestable, **quiero** registrar diagnósticos computarizados y lecturas de telemetría de forma autónoma sin depender de conectividad a internet en áreas apantalladas o fosas subterráneas, **para** garantizar la continuidad operativa del escaneo y sincronizar los resultados al recuperar enlace de datos

**Criterios de aceptación**

*Escenario 1: Persistencia local de registros telemétricos en zona desconectada*

- **Dado que** la bahía de servicio o fosa de inspección carece de señal de red inalámbrica o cobertura de telefonía móvil
- **Cuando** el técnico finaliza la extracción de datos computarizados del vehículo
- **Entonces** el sistema resguarda íntegramente las tramas de telemetría y códigos de fallo en la base de datos local del dispositivo móvil
- **Y** mantiene el expediente en cola con estado de sincronización pendiente

*Escenario 2: Sincronización automática en bloque con el servidor central*

- **Dado que** el dispositivo móvil restablece la conectividad con la red del taller al salir de la fosa
- **Cuando** el servicio de sincronización en segundo plano detecta enlace estable a internet
- **Entonces** el sistema despacha automáticamente el lote de registros telemétricos pendientes hacia la plataforma central
- **Y** actualiza la orden de trabajo en la nube preservando la marca temporal original de captura en el foso

<a id="us20"></a>

### US20 — Borrado y verificación de restablecimiento de fallas en la computadora

**Usuario:** Técnico mecánico · **Prioridad:** Media · **Épica:** EP04 · **Sprint:** Sprint 3 · **Story points:** 2

**Como** técnico mecánico que concluye la subsanación de una avería automotriz, **quiero** transmitir el comando de borrado de códigos de avería a la unidad de control y comprobar el apagado de testigos tras completar la reparación física, **para** validar el éxito de la intervención mecánica y asegurar la entrega del vehículo en estado óptimo

**Criterios de aceptación**

*Escenario 1: Restablecimiento exitoso de memoria de averías y comprobación*

- **Dado que** la intervención mecánica correctiva ha finalizado satisfactoriamente y el motor se encuentra apagado con el conmutador en contacto
- **Cuando** el técnico confirma la orden de borrado de códigos de error
- **Entonces** el sistema transmite la instrucción normalizada de limpieza a la computadora vehicular, efectúa una re-interrogación inmediata
- **Y** confirma que la memoria de averías quedó vacía y el testigo de advertencia se encuentra apagado

*Escenario 2: Detección de persistencia de fallo por subsistencia de avería física*

- **Dado que** el componente mecánico o sensor reemplazado presenta un problema de cableado o defecto no subsanado
- **Cuando** se ejecuta la secuencia de borrado de códigos
- **Entonces** el sistema detecta la reaparición inmediata del código de avería en la comprobación posterior
- **Y** notifica al técnico la persistencia de la condición anómala requiriendo una nueva revisión física del circuito

<a id="us21"></a>

### US21 — Generación de informe pericial predictivo con inteligencia artificial y exportación en PDF

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP04 · **Sprint:** Sprint 3 · **Story points:** 5

**Como** técnico mecánico que requiere sustentar un diagnóstico predictivo avanzado, **quiero** solicitar un diagnóstico predictivo asistido por inteligencia artificial seleccionando una ventana temporal de telemetría y generar un reporte en PDF, **para** contrastar las recomendaciones del modelo con mi criterio mecánico profesional y emitir un dictamen pericial preventivo de alta precisión para el cliente

**Criterios de aceptación**

*Escenario 1: Generación de dictamen predictivo sobre período telemétrico seleccionado*

- **Dado que** el vehículo cuenta con registros de telemetría y fallos capturados a lo largo de un período de tiempo definido por el técnico
- **Cuando** el técnico solicita el análisis predictivo para la ventana temporal seleccionada
- **Entonces** el sistema procesa las variables termodinámicas y códigos de avería mediante el modelo de inteligencia artificial, calcula el índice de salud vehicular y el nivel de certeza del diagnóstico
- **Y** presenta las anomalías proyectadas con los servicios correctivos sugeridos para validación del técnico

*Escenario 2: Emisión y descarga de reporte pericial en formato PDF*

- **Dado que** el técnico ha revisado las recomendaciones del modelo y ratificado el diagnóstico según su criterio profesional
- **Cuando** confirma la generación del informe formal de salud vehicular
- **Entonces** el sistema renderiza un documento estructurado en formato PDF que consolida el historial de códigos DTC, curvas estadísticas de sensores, el dictamen pericial y las recomendaciones de mantenimiento
- **Y** lo vincula como anexo documental a la orden de trabajo para su remisión al cliente

*Escenario 3: Notificación de telemetría insuficiente para inferencia predictiva*

- **Dado que** la unidad vehicular carece de suficientes tramas telemétricas continuas en el rango de fechas seleccionado
- **Cuando** se solicita la ejecución del modelo de inteligencia artificial
- **Entonces** el sistema advierte que el volumen de datos muestreados es insuficiente para emitir un diagnóstico predictivo confiable
- **Y** orienta al técnico a ampliar la ventana de tiempo o realizar un escaneo directo en bahía

## EP05 — Presupuestos y Cotizaciones de Mantenimiento

- **Área:** Operaciones de Taller (MRO)
- **Rol:** Asesor de servicio
- **Prioridad:** Alta
- **Alcance:** Formulación de presupuestos MRO desglosando mano de obra y repuestos de catálogo, verificación de existencias y aprobación del cliente.

> **Como** asesor de servicio, **quiero** disponer del cálculo automático de costos de tareas y repuestos, auditar asignaciones de almacén y asentar la conformidad del cliente, **para** garantizar la transparencia tarifaria del taller y formalizar el inicio de labores tras la aceptación de la cotización

<a id="us22"></a>

### US22 — Cómputo automático en cascada de costos de tareas e impuestos

**Usuario:** Asesor de servicio · **Prioridad:** Alta · **Épica:** EP05 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** asesor de servicio que formula el presupuesto de mantenimiento, **quiero** que el sistema calcule automáticamente el precio de cada tarea, el subtotal acumulado, el impuesto de ley y el total general, **para** eliminar discrepancias aritméticas manuales y mantener la integridad contable de la orden de trabajo

**Criterios de aceptación**

*Escenario 1: Cálculo en cascada por adición de servicios y repuestos*

- **Dado que** una orden de trabajo incluye tareas técnicas con tarifas base de mano de obra y productos imputados con precio unitario y cantidad
- **Cuando** el sistema procesa los valores de la orden
- **Entonces** calcula el precio de cada tarea sumando la tarifa base del servicio y el importe total de sus productos asociados
- **Y** liquida el subtotal de la orden como la sumatoria de los precios de sus tareas, calculando el dieciocho por ciento de impuesto y el monto total final

*Escenario 2: Recálculo dinámico ante variaciones de insumos o mano de obra*

- **Dado que** se modifica la cantidad de un repuesto asignado o se incorpora una labor mecánica complementaria
- **Cuando** se confirman las modificaciones en la tarea correspondiente
- **Entonces** el sistema actualiza automáticamente el precio individual de la tarea intervenida
- **Y** propaga el recálculo inmediato hacia el subtotal, impuesto y total de la cabecera de la orden de trabajo

<a id="us23"></a>

### US23 — Imputación de repuestos a tareas y supervisión administrativa

**Usuario:** Encargado de inventario · **Prioridad:** Alta · **Épica:** EP05 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** encargado de inventario responsable de la reserva y abastecimiento de repuestos, **quiero** agregar productos del catálogo a una tarea mecánica y alertar a la administración sobre consumos realizados en bahía, **para** aprovisionar los materiales necesarios y permitir al administrador auditar o retirar piezas no aplicables reintegrándolas al almacén

**Criterios de aceptación**

*Escenario 1: Imputación de repuesto por el mecánico con alerta de supervisión*

- **Dado que** el mecánico requiere instalar un componente de almacén para ejecutar la labor asignada en bahía
- **Cuando** selecciona el producto del catálogo y registra la cantidad requerida en la tarea técnica
- **Entonces** el sistema vincula el artículo a la tarea actualizando su costo acumulado
- **Y** despacha una notificación de supervisión al panel del administrador de taller detallando el ítem, la tarea y la orden receptora

*Escenario 2: Retiro de producto improcedente por el administrador y reintegro a almacén*

- **Dado que** el administrador evalúa el repuesto imputado por el mecánico y constata tras coordinación verbal que la pieza no corresponde a la especificación del vehículo
- **Cuando** el administrador remueve el producto de la tarea técnica
- **Entonces** el sistema desvincula el artículo de la tarea recalculando los importes hacia la baja
- **Y** reintegra de forma lógica las unidades retiradas al inventario disponible del almacén

*Escenario 3: Imputación directa de materiales por el administrador*

- **Dado que** el administrador formula la programación de materiales de una orden de trabajo
- **Cuando** asocia directamente los repuestos requeridos a las tareas correspondientes
- **Entonces** el sistema efectúa la afectación de existencias y actualiza los costos presupuestados sin requerir confirmación adicional

<a id="us24"></a>

### US24 — Generación y remisión digital de proforma comercial en PDF

**Usuario:** Asesor de servicio · **Prioridad:** Media · **Épica:** EP05 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** asesor de servicio responsable del trato técnico-comercial con el cliente, **quiero** generar un documento de cotización estructurado en formato PDF y remitirlo digitalmente al cliente, **para** proporcionarle un desglose claro de mano de obra, repuestos y costos totales previo a solicitar su conformidad

**Criterios de aceptación**

*Escenario 1: Generación de proforma comercial estandarizada en PDF*

- **Dado que** la orden de trabajo cuenta con sus tareas técnicas y repuestos debidamente costeados
- **Cuando** el administrador solicita la exportación de la proforma comercial
- **Entonces** el sistema genera un archivo PDF formal con los datos fiscales del taller, ficha del auto, tabla desglosada de tareas y piezas, subtotal, impuesto y plazo de validez
- **Y** lo almacena como anexo documental descargable en el expediente de la orden

*Escenario 2: Despacho digital de cotización al contacto del cliente*

- **Dado que** el documento PDF de cotización ha sido generado conforme
- **Cuando** el administrador confirma el envío digital hacia el cliente
- **Entonces** el sistema remite el enlace seguro de acceso por mensajería o correo electrónico
- **Y** asienta el registro de proforma entregada dejando la orden lista para la consulta de aceptación

<a id="us25"></a>

### US25 — Formalización de resolución de presupuesto tras comunicación externa

**Usuario:** Asesor de servicio · **Prioridad:** Alta · **Épica:** EP05 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** asesor de servicio responsable del seguimiento de presupuestos con el cliente, **quiero** asentar en el sistema la decisión de aceptación o desistimiento comunicada externamente por el cliente, **para** habilitar el inicio formal de las faenas mecánicas en bahía o cancelar la orden liberando los materiales

**Criterios de aceptación**

*Escenario 1: Asentamiento de conformidad externa y transición a orden en progreso*

- **Dado que** el cliente manifiesta verbalmente, por llamada o mensajería su aceptación de los costos y condiciones de la cotización remitida
- **Cuando** el administrador registra la conformidad del propietario en la plataforma móvil
- **Entonces** el sistema actualiza el estado de la orden de trabajo a en progreso
- **Y** habilita formalmente las tareas asignadas para que los mecánicos inicien el cronometraje y ejecución en bahía

*Escenario 2: Asentamiento de desistimiento del cliente y cancelación de orden*

- **Dado que** el cliente comunica externamente que no autoriza los trabajos cotizados
- **Cuando** el administrador asienta el rechazo en el sistema especificando el motivo del desistimiento
- **Entonces** el sistema actualiza el estado de la orden de trabajo a cancelada
- **Y** libera automáticamente las reservas de repuestos reintegrándolos a disponibilidad en almacén y disponiendo el vehículo para retiro

## EP06 — Ejecución de Tareas en Bahía de Servicio

- **Área:** Operaciones de Taller (MRO)
- **Rol:** Técnico mecánico y mecánico jefe
- **Prioridad:** Alta
- **Alcance:** Gestión de tareas mecánicas asignadas, cronometraje de horas hombre y registro fotográfico de piezas desmontadas y repuestos nuevos.

> **Como** técnico mecánico y mecánico jefe, **quiero** gestionar mis tareas mecánicas asignadas en bahía, cronometrar tiempos efectivos de mano de obra con pausas auditadas y capturar evidencias fotográficas del desmontaje y montaje de piezas, **para** garantizar una ejecución metódica del servicio, sustentar pericialmente el trabajo ante el cliente y transparentar las horas hombre laboradas

<a id="us26"></a>

### US26 — Consulta y priorización de tareas mecánicas asignadas en bahía

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP06 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** técnico mecánico que inicia su turno de trabajo en bahía, **quiero** consultar en mi dispositivo móvil el listado ordenado de tareas mecánicas asignadas a mi nombre en las distintas órdenes de trabajo, **para** organizar las intervenciones del día según prioridad operativa y preparar las herramientas necesarias

**Criterios de aceptación**

*Escenario 1: Consulta de asignaciones activas de la jornada*

- **Dado que** el técnico automotriz inicia sesión en Atelier Workshop Mobile dentro de su sucursal de trabajo
- **Cuando** accede a la sección de asignaciones operativas en bahía
- **Entonces** el sistema presenta las tareas pendientes y en progreso asignadas a su identificador de colaborador
- **Y** muestra para cada labor la placa vehicular, el servicio solicitado, el tiempo estimado de mano de obra y la bahía asignada

*Escenario 2: Notificación en tiempo real de nueva labor asignada*

- **Dado que** el administrador de taller asigna una nueva tarea de mantenimiento a un mecánico en turno
- **Cuando** el sistema asienta la delegación de la labor técnica
- **Entonces** emite una alerta inmediata al dispositivo móvil del operario
- **Y** actualiza de manera automática su cola de trabajo incorporando la tarea con sus instrucciones técnicas

<a id="us27"></a>

### US27 — Cronometraje de labor efectiva, pausas operativas y sincronización con turnos laborales

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP06 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** técnico mecánico que ejecuta una tarea de mantenimiento asignada, **quiero** registrar el inicio de labor en bahía, pausar el cronómetro por refrigerio o fin de turno y sincronizar la suspensión con mi horario laboral validado, **para** garantizar un cómputo exacto de horas hombre efectivas, notificar mis pausas a la supervisión e impedir registros anómalos fuera de mi turno

**Criterios de aceptación**

*Escenario 1: Inicio formal de cronometraje en bahía de servicio*

- **Dado que** la tarea técnica se encuentra asignada al operario y el vehículo está posicionado en la bahía de trabajo
- **Cuando** el técnico confirma el inicio de la intervención mecánica en el dispositivo móvil
- **Entonces** el sistema actualiza el estado de la tarea a en progreso, registra la marca temporal exacta de inicio
- **Y** activa el cronómetro continuo de horas hombre efectivas computando los tiempos de labor

*Escenario 2: Pausa por horario de refrigerio con notificación al supervisor*

- **Dado que** el técnico automotriz interrumpe temporalmente la intervención por su período de refrigerio habitual
- **Cuando** instruye la pausa operativa en la plataforma móvil
- **Entonces** el sistema detiene inmediatamente el cómputo de horas hombre efectivas acumulando los segundos transcurridos
- **Y** emite una notificación de alerta al administrador con la hora exacta y el colaborador en refrigerio para auditar la legitimidad del intervalo

*Escenario 3: Bloqueo de pausa anticipada por presunto fin de turno ante turno vigente*

- **Dado que** el técnico intenta pausar su labor bajo la justificación de culminación de jornada a las cuatro de la tarde, pero el registro de turnos asignados en gestión de personal establece su horario de salida a las cinco de la tarde
- **Cuando** envía la instrucción de pausa por fin de turno
- **Entonces** el sistema contrasta la hora del servidor contra el horario del turno asignado
- **Y** rechaza la instrucción por inconsistencia horaria, manteniendo la tarea en progreso y alertando al administrador del intento de pausa prematura

*Escenario 4: Pausa automática por fin de turno laboral o marcación de salida mediante eventos de dominio*

- **Dado que** culmina la franja horaria configurada en el turno de trabajo o el técnico registra su marcación de salida laboral en el sistema de asistencia
- **Cuando** el contexto de gestión de personal publica el evento de salida laboral procesado a través de la capa anticorrupción
- **Entonces** el sistema de operaciones pausa automáticamente la tarea activa del operario deteniendo el cronómetro
- **Y** notifica al administrador del cese automático de faena consolidando el tiempo efectivo acumulado de la jornada

*Escenario 5: Reanudación de la faena tras pausa operativa*

- **Dado que** una tarea se encuentra pausada por refrigerio o por cambio de guardia laboral y el técnico retorna a la bahía
- **Cuando** confirma la reanudación de la labor técnica en el dispositivo
- **Entonces** el sistema reactiva el cronómetro sumando el nuevo tiempo al acumulado previo
- **Y** notifica al supervisor el reinicio de los trabajos en bahía

<a id="us28"></a>

### US28 — Registro fotográfico pericial de desmontaje y montaje de piezas en foso

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP06 · **Sprint:** Sprint 3 · **Story points:** 2

**Como** técnico mecánico que realiza el desmontaje y montaje de piezas en foso, **quiero** capturar fotografías de la pieza desgastada desmontada y del repuesto nuevo instalado en la tarea técnica, **para** vincular las evidencias en el registro de tareas y respaldar pericialmente la ejecución física del servicio ante el cliente

**Criterios de aceptación**

*Escenario 1: Captura de fotografía de pieza defectuosa desmontada*

- **Dado que** el técnico automotriz extrae un componente mecánico severamente deteriorado durante la ejecución de la tarea
- **Cuando** registra la evidencia fotográfica clasificándola como defecto en la aplicación móvil
- **Entonces** el sistema almacena la fotografía vinculada a la tarea específica
- **Y** genera la marca temporal probatoria del hallazgo en el registro técnico

*Escenario 2: Captura de evidencia de repuesto nuevo instalado*

- **Dado que** el operario concluye el montaje del nuevo repuesto en el conjunto mecánico del vehículo
- **Cuando** captura la fotografía de comprobación tipificándola como labor completada
- **Entonces** el sistema anexa la imagen al registro de evidencias de la tarea técnica
- **Y** consolida el expediente visual para sustentar la liquidación y entrega final al cliente

<a id="us29"></a>

### US29 — Solicitud y validación administrativa de suspensión por pieza defectuosa indispensable

**Usuario:** Mecánico jefe · **Prioridad:** Alta · **Épica:** EP06 · **Sprint:** Sprint 3 · **Story points:** 2

**Como** mecánico jefe que supervisa la viabilidad técnica y disponibilidad de piezas en bahía, **quiero** gestionar la solicitud de suspensión técnica cuando una pieza de recambio suministrada presente defecto insalvable que impida continuar la faena, **para** que el administrador audite la justificación técnica y autorice formalmente el pase a espera o desestime la pausa obligando a continuar en progreso

**Criterios de aceptación**

*Escenario 1: Solicitud de suspensión por defecto insalvable de pieza y aprobación administrativa a estado en espera*

- **Dado que** el técnico automotriz constata durante la faena que el repuesto suministrado presenta un defecto de fabricación o daño físico crítico que impide continuar el ensamblaje de la tarea
- **Cuando** emite la solicitud de suspensión especificando la justificación técnica de la pieza defectuosa y el ítem faltante a reponer
- **Entonces** el sistema notifica inmediatamente al administrador con el detalle probatorio de la avería
- **Y** al validar el supervisor que la causa es razonable e incapacitante para continuar, aprueba la suspensión transicionando la tarea al estado en espera y congelando el cronómetro

*Escenario 2: Desestimación de la suspensión por el administrador y continuidad obligatoria en progreso*

- **Dado que** el administrador evalúa la justificación técnica enviada por el operario y constata que la observación de la pieza no bloquea la continuidad ni compromete la seguridad del ensamblaje
- **Cuando** el administrador rechaza la solicitud de suspensión en la plataforma
- **Entonces** el sistema mantiene la tarea inalterada en estado en progreso
- **Y** continúa el cronometraje ininterrumpido notificando al operario la resolución denegatoria para proseguir con la faena asignada

*Escenario 3: Reanudación de la faena tras suministro del repuesto conforme*

- **Dado que** la tarea se encuentra formalmente en estado en espera y el área de almacén entrega la pieza sustituta en óptimas condiciones físicas
- **Cuando** el técnico confirma la recepción del repuesto idóneo e instruye la reanudación del trabajo
- **Entonces** el sistema restituye la tarea al estado en progreso reactivando el cronómetro de horas hombre
- **Y** consolida los minutos acumulados en suspensión dentro del expediente de la orden de trabajo

<a id="us30"></a>

### US30 — Cierre técnico de tarea con cómputo de horas hombre efectivas

**Usuario:** Técnico mecánico · **Prioridad:** Alta · **Épica:** EP06 · **Sprint:** Sprint 3 · **Story points:** 2

**Como** técnico mecánico que concluye una tarea técnica en bahía, **quiero** registrar la finalización formal de la faena mecánica en la aplicación móvil, **para** cerrar el cómputo de horas hombre reales trabajadas y habilitar la orden para el control de calidad

**Criterios de aceptación**

*Escenario 1: Cierre técnico conforme de la labor mecánica*

- **Dado que** el técnico ha completado el ensamblaje, pruebas mecánicas y adjuntado las evidencias fotográficas de montaje
- **Cuando** confirma la culminación de la tarea en el dispositivo móvil
- **Entonces** el sistema detiene definitivamente el cronómetro, liquida las horas hombre efectivas en formato decimal
- **Y** actualiza el estado de la tarea a completada registrando la marca temporal de cierre

*Escenario 2: Transición automática de la orden ante culminación de todas las tareas*

- **Dado que** el técnico finaliza la última tarea pendiente perteneciente a una orden de trabajo
- **Cuando** se asienta el cierre técnico de dicha labor
- **Entonces** el sistema constata que la totalidad de tareas mecánicas se encuentran culminadas
- **Y** actualiza el estado de la orden de trabajo notificando al supervisor que el vehículo está listo para la inspección final de calidad

## EP07 — Abastecimiento y Descargo de Repuestos FIFO

- **Área:** Logística e Inventario
- **Rol:** Encargado de inventario
- **Prioridad:** Media
- **Alcance:** Requisición de repuestos desde la bahía, descargo contable bajo regla estricta FIFO por lote y alertas de stock de seguridad en almacén.

> **Como** encargado de inventario, **quiero** consultar existencias de catálogo, liquidar descargos bajo la regla contable FIFO por lote, restituir materiales no utilizados y liquidar precios automáticos, **para** preservar la exactitud del kardex valorizado, calcular márgenes comerciales reales y prevenir quiebres de inventario que interrumpan los servicios

<a id="us31"></a>

### US31 — Búsqueda y consulta de disponibilidad de repuestos en catálogo

**Usuario:** Encargado de inventario · **Prioridad:** Alta · **Épica:** EP07 · **Sprint:** Sprint 2 · **Story points:** 2

**Como** encargado de inventario responsable de la gestión y existencias del almacén, **quiero** buscar piezas por SKU, categoría o denominación comercial para verificar existencias físicas y precio, **para** determinar la viabilidad inmediata de su instalación en bahía sin requerir verificación manual en anaquel

**Criterios de aceptación**

*Escenario 1: Búsqueda exitosa con saldo disponible positivo*

- **Dado que** el usuario ingresa el código SKU o término comercial de un repuesto activo en la plataforma móvil
- **Cuando** ejecuta la consulta en el catálogo de inventario
- **Entonces** el sistema presenta la denominación comercial del artículo, el código SKU, la categoría automotriz, el precio de venta unitario y el stock total consolidado disponible
- **Y** habilita la selección del material para su consumo en órdenes de trabajo activas

*Escenario 2: Restricción de selección ante repuesto con saldo en cero*

- **Dado que** un artículo del catálogo registra un stock total consolidado en cero unidades en el almacén del taller
- **Cuando** el colaborador consulta el artículo en el listado de materiales
- **Entonces** el sistema visualiza la partida resaltando el indicador de existencia agotada
- **Y** bloquea la selección para adición inmediata en tareas mecánicas indicando que requiere orden de reabastecimiento

<a id="us32"></a>

### US32 — Descargo contable y físico automatizado mediante regla estricta FIFO por lote

**Usuario:** Encargado de inventario · **Prioridad:** Alta · **Épica:** EP07 · **Sprint:** Sprint 2 · **Story points:** 5

**Como** encargado de inventario que despacha piezas para una orden de trabajo, **quiero** registrar el consumo de repuestos en una tarea mecánica rebajando los lotes cronológicos de adquisición más antiguos, **para** valorizar con exactitud el costo de ventas bajo el método FIFO e impedir la asignación de materiales inexistentes

**Criterios de aceptación**

*Escenario 1: Descargo cronológico FIFO a partir del lote más antiguo*

- **Dado que** existen múltiples lotes de adquisición para un juego de pastillas de freno con distintas fechas de ingreso
- **Cuando** el usuario confirma la instalación de dos unidades en la tarea mecánica asignada
- **Entonces** el sistema identifica el lote con saldo remanente positivo y fecha de ingreso más antigua
- **Y** deduce las unidades de dicho lote actualizando su saldo remanente, rebaja el stock consolidado del artículo y asienta la imputación en la orden de trabajo

*Escenario 2: Imputación multi-lote ante saldo parcial en lote inicial*

- **Dado que** la cantidad de aceite requerida para el servicio es de cuatro galones, pero el lote FIFO más antiguo solo cuenta con un saldo de un galón disponible
- **Cuando** se procesa el consumo en la tarea técnica
- **Entonces** el sistema agota completamente el saldo del lote inicial transicionándolo a cero remanente
- **Y** descuenta los tres galones restantes del siguiente lote cronológicamente subsiguiente, vinculando ambos orígenes al costo real de la faena

*Escenario 3: Bloqueo de descargo por saldo insuficiente en almacén*

- **Dado que** el operario intenta imputar cinco unidades de un filtro cuya existencia física consolidada en almacén es de solo dos unidades
- **Cuando** solicita confirmar el descargo de inventario
- **Entonces** el sistema rechaza la transacción impidiendo saldos negativos
- **Y** notifica la falta de existencias suficientes para completar la cantidad especificada

<a id="us33"></a>

### US33 — Reincorporación lógica de materiales a inventario por remoción en tarea técnica

**Usuario:** Encargado de inventario · **Prioridad:** Media · **Épica:** EP07 · **Sprint:** Sprint 2 · **Story points:** 3

**Como** encargado de inventario responsable de la auditoría y custodia de repuestos, **quiero** revertir la asignación de un repuesto registrado erróneamente en una tarea técnica y devolverlo a disponibilidad de almacén, **para** corregir el presupuesto de la orden y reintegrar las unidades a los lotes físicos de inventario

**Criterios de aceptación**

*Escenario 1: Reintegro exitoso de repuesto al lote de procedencia*

- **Dado que** el administrador coordina con el mecánico la no aplicación de un repuesto previamente asignado a una tarea en bahía
- **Cuando** el administrador ejecuta la eliminación del material en el desglose de productos de la faena
- **Entonces** el sistema cancela la imputación del artículo en la tarea recalculando el precio de la orden
- **Y** repone de forma inmediata las unidades al saldo remanente del lote físico correspondiente e incrementa el stock total disponible en almacén

*Escenario 2: Auditoría y trazabilidad del reintegro de materiales*

- **Dado que** se concreta la reversión de un repuesto desde una orden de trabajo activa
- **Cuando** el sistema asienta el retorno físico de las piezas al inventario
- **Entonces** genera un registro de auditoría con la identidad del administrador, la marca temporal exacta y la orden origen
- **Y** actualiza el estado del inventario para su consulta inmediata en la plataforma móvil

<a id="us34"></a>

### US34 — Disparo y desactivación de alertas automáticas de reposición por stock mínimo

**Usuario:** Encargado de inventario · **Prioridad:** Media · **Épica:** EP07 · **Sprint:** Sprint 3 · **Story points:** 2

**Como** encargado de inventario responsable del abastecimiento oportuno del almacén, **quiero** recibir alertas cuando el stock de un repuesto alcance el umbral mínimo configurado y que se desactiven al ingresar nuevas compras, **para** gestionar oportunamente las adquisiciones a proveedores antes de que ocurra una rotura operativa en bahía

**Criterios de aceptación**

*Escenario 1: Disparo de alerta crítica por consumo bajo el umbral de seguridad*

- **Dado que** un ítem de alta rotación posee un umbral de seguridad establecido en diez unidades y el saldo disponible se reduce a nueve tras un descargo en bahía
- **Cuando** el sistema asienta la transacción de salida en el inventario
- **Entonces** detecta que el stock consolidado es menor o igual al valor mínimo permitido
- **Y** emite una notificación de advertencia al administrador del taller marcando el artículo en estado de reposición requerida

*Escenario 2: Desactivación automática de la alerta tras recepción de compra*

- **Dado que** un repuesto mantiene activa una alerta por nivel crítico de existencias y se asienta la recepción de un nuevo lote procedente de una orden de compra
- **Cuando** el sistema procesa el ingreso de mercadería elevando el stock disponible por encima del umbral mínimo
- **Entonces** cancela de forma automática la condición de advertencia en el catálogo
- **Y** remueve la alerta de la bandeja de notificaciones operativas

<a id="us35"></a>

### US35 — Imputación automática de precios de venta de catálogo y conciliación de margen FIFO

**Usuario:** Encargado de inventario · **Prioridad:** Alta · **Épica:** EP07 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** encargado de inventario que supervisa la valorización contable de repuestos, **quiero** que el sistema replique automáticamente el precio base del catálogo al registrar productos en tareas y concilie el costo de adquisición de los lotes consumidos, **para** garantizar un cobro transparente y uniforme al cliente, automatizar los importes de la orden y registrar la rentabilidad bruta real del servicio

**Criterios de aceptación**

*Escenario 1: Replicación automática de precio de catálogo y cálculo de importe de tarea*

- **Dado que** el operario añade tres bujías de encendido a una tarea técnica y el catálogo registra para dicho artículo un precio base de doce soles
- **Cuando** el sistema asienta la asignación del repuesto a la faena mecánica
- **Entonces** asigna de forma automática el precio unitario en doce soles copiando el valor base de catálogo sin ingreso manual
- **Y** liquida el importe total de la línea en treinta y seis soles, sumándolo automáticamente al costo base del servicio para determinar el precio final de la tarea y actualizar el subtotal de la orden de trabajo

*Escenario 2: Conciliación de margen comercial ante consumo multi-lote con costos dispares*

- **Dado que** el consumo de tres unidades de un repuesto agota dos unidades de un lote antiguo con costo de adquisición de cinco soles y toma una unidad de un lote nuevo con costo de siete soles
- **Cuando** el motor de inventario ejecuta la asignación física de existencias bajo la regla FIFO
- **Entonces** mantiene inalterable el precio unitario uniforme de venta de doce soles por cada unidad facturada al cliente
- **Y** calcula en la contabilidad interna de la orden un costo de ventas total de diecisiete soles, registrando una ganancia bruta de diecinueve soles para el taller

*Escenario 3: Inmutabilidad del precio asignado ante variaciones posteriores del catálogo*

- **Dado que** una orden de trabajo mantiene repuestos asignados con un precio unitario de doce soles y con posterioridad la administración actualiza el precio base del catálogo a catorce soles
- **Cuando** el personal consulta o liquida la faena previamente acordada
- **Entonces** el sistema conserva inalterado el precio unitario de doce soles registrado al momento de la imputación
- **Y** preserva la integridad económica de la cotización pactada con el cliente impidiendo incrementos no autorizados

## EP08 — Facturación Electrónica y Cierre de Servicio

- **Área:** Facturación y Cumplimiento Fiscal
- **Rol:** Cajero y asesor de servicio
- **Prioridad:** Alta
- **Alcance:** Consolidación de costos de orden de trabajo, emisión de Boletas o Facturas UBL 2.1 ante SUNAT vía Nubefact, cobro y entrega vehicular.

> **Como** cajero y asesor de servicio, **quiero** liquidar automáticamente los importes de la orden, emitir comprobantes tributarios UBL 2.1 ante SUNAT, amortizar cobros y transicionar la orden a pagada, **para** garantizar el cumplimiento fiscal, certificar el abono dinerario del servicio y finalizar la custodia legal del vehículo

<a id="us36"></a>

### US36 — Liquidación económica automática en cascada de la orden de trabajo

**Usuario:** Cajero · **Prioridad:** Alta · **Épica:** EP08 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** cajero responsable de la liquidación económica y cierre contable de la orden, **quiero** que el sistema calcule automáticamente los importes consolidados de la orden de trabajo sumando los precios de tareas y repuestos, **para** disponer del importe exacto a cobrar y alimentar la base del comprobante electrónico sin digitación manual

**Criterios de aceptación**

*Escenario 1: Cálculo automático en cascada de tareas, subtotal, impuesto y total de orden*

- **Dado que** la orden de trabajo acumula tareas mecánicas con costos base de servicio y productos consumidos imputados
- **Cuando** el sistema procesa la liquidación económica de la orden
- **Entonces** calcula el precio de cada tarea sumando el costo base del servicio y el importe total de sus productos
- **Y** totaliza el subtotal de la orden sumando los precios de todas sus tareas, liquida el dieciocho por ciento de impuesto y determina el monto total final que alimenta el valor a cobrar en el comprobante electrónico

*Escenario 2: Actualización reactiva de importes ante modificaciones operativas*

- **Dado que** una tarea mecánica en ejecución incorpora un nuevo repuesto o registra una variación de alcance autorizada
- **Cuando** se asienta la modificación del material en la faena
- **Entonces** el sistema recalcula de manera inmediata y reactiva el precio de la tarea afectada
- **Y** actualiza en cascada el subtotal, el impuesto y el importe total general de la orden de trabajo conservando la consistencia contable

<a id="us37"></a>

### US37 — Emisión de comprobantes tributarios electrónicos UBL 2.1 ante SUNAT vía PSE

**Usuario:** Cajero · **Prioridad:** Alta · **Épica:** EP08 · **Sprint:** Sprint 3 · **Story points:** 5

**Como** cajero encargado de la emisión de comprobantes de pago electrónicos ante SUNAT, **quiero** emitir Boletas de Venta o Facturas Electrónicas con numeración correlativa estricta y enviarlas a SUNAT mediante el proveedor PSE, **para** otorgar comprobantes con plena validez fiscal y obtener la Constancia de Recepción oficial

**Criterios de aceptación**

*Escenario 1: Emisión exitosa de Factura Electrónica a empresa*

- **Dado que** el cliente corporativo cuenta con un RUC de once dígitos en condición de activo y habido en el padrón tributario
- **Cuando** el administrador confirma la emisión de la Factura Electrónica vinculada a la orden de trabajo
- **Entonces** el sistema incrementa el correlativo de la serie autorizada de la sede física, estructura el documento bajo el estándar UBL 2.1 y lo transmite al proveedor de servicios electrónicos
- **Y** almacena la Constancia de Recepción aprobada por SUNAT, el resumen hash de la firma digital y los enlaces a los archivos PDF y XML

*Escenario 2: Emisión exitosa de Boleta de Venta a persona natural*

- **Dado que** el cliente es una persona natural registrada con Documento Nacional de Identidad de ocho dígitos
- **Cuando** el administrador confirma la emisión de la Boleta de Venta Electrónica
- **Entonces** el sistema asigna el correlativo secuencial de la serie de boletas, envía el comprobante al servicio de facturación fiscal
- **Y** vincula el documento con su representación impresa conteniendo el código QR de verificación tributaria

*Escenario 3: Manejo de contingencia ante intermitencia del servicio fiscal*

- **Dado que** el servicio externo del proveedor de servicios electrónicos o SUNAT experimenta una interrupción temporal de comunicaciones
- **Cuando** se envía la instrucción de emisión del comprobante
- **Entonces** el sistema guarda el comprobante en estado emitido con su correlativo formal reservado sin duplicar numeraciones
- **Y** programa el reintento automático asíncrono para obtener la confirmación tributaria una vez normalizada la red

<a id="us38"></a>

### US38 — Recaudación y amortización de pagos multi-medio en mostrador y patio

**Usuario:** Cajero · **Prioridad:** Alta · **Épica:** EP08 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** cajero encargado de la cobranza y tesorería en mostrador, **quiero** registrar abonos dinerarios totales o parciales indicando el medio de pago y código de operación bancario, **para** amortizar el saldo adeudado del comprobante fiscal y conciliar la recaudación de la jornada

**Criterios de aceptación**

*Escenario 1: Pago íntegro con tarjeta o billetera digital*

- **Dado que** el cliente abona el total facturado mediante tarjeta de débito o billetera digital interoperable
- **Cuando** el administrador asienta el cobro registrando el medio financiero y la referencia de transacción de la operación
- **Entonces** el sistema crea el registro de pago vinculado al comprobante fiscal
- **Y** actualiza el estado de cobro a completado al extinguir en su totalidad la deuda monetaria

*Escenario 2: Amortización fraccionada con combinación de medios de pago*

- **Dado que** el cliente abona una parte del importe en efectivo en patio y el monto restante mediante transferencia bancaria
- **Cuando** el administrador registra sucesivamente cada pago parcial en la plataforma móvil
- **Entonces** el sistema descuenta de forma acumulativa cada abono del saldo exigible del comprobante
- **Y** mantiene la liquidación en estado pendiente hasta que el importe recaudado iguale el valor total facturado

<a id="us39"></a>

### US39 — Transición a orden pagada, emisión de pase de salida vehicular y entrega en patio

**Usuario:** Asesor de servicio · **Prioridad:** Alta · **Épica:** EP08 · **Sprint:** Sprint 3 · **Story points:** 2

**Como** asesor de servicio responsable de la entrega técnica vehicular en patio, **quiero** verificar la culminación de tareas, validar la amortización total del pago y registrar la conformidad de entrega del cliente, **para** transicionar la orden a pagada, emitir el pase de salida vehicular y extinguir la custodia legal del taller

**Criterios de aceptación**

*Escenario 1: Transición automática de orden a completada al culminar todas sus tareas*

- **Dado que** el operario en bahía registra el cierre técnico de la última tarea mecánica pendiente de la orden de trabajo
- **Cuando** el sistema constata que el cien por ciento de las faenas se encuentran en estado completada
- **Entonces** transiciona automáticamente el estado de la orden de trabajo a completada
- **Y** notifica al asesor de servicio que el vehículo se encuentra listo para retiro e inspección de calidad previa a la liquidación

*Escenario 2: Transición a orden pagada y emisión de pase de salida tras liquidación total*

- **Dado que** la orden de trabajo se encuentra completada y los abonos de pago cubren la totalidad del importe facturado en el comprobante fiscal
- **Cuando** el asesor de servicio confirma la entrega en patio y captura la firma de conformidad del cliente en el dispositivo móvil
- **Entonces** el sistema transiciona el estado de la orden de trabajo a pagada
- **Y** genera el pase de salida vehicular digital habilitando el retiro físico de la unidad y liberando la bahía asignada

*Escenario 3: Restricción de salida vehicular ante orden no cancelada*

- **Dado que** una orden de trabajo permanece en estado completada pero mantiene saldo económico pendiente de pago en el comprobante fiscal
- **Cuando** el usuario intenta expedir el pase de salida vehicular
- **Entonces** el sistema bloquea la emisión de la autorización de salida
- **Y** notifica que la orden de trabajo debe transicionar al estado pagada mediante la cancelación previa de la deuda

## EP09 — Monitoreo Operativo de Patio y Gestión Gerencial

- **Área:** Operaciones y Gestión de Negocio
- **Rol:** Mecánico jefe y dueño de taller
- **Prioridad:** Media
- **Alcance:** Supervisión en tiempo real de bahías, parametrización tributaria SUNAT y exportación documental de balances y diagnósticos periciales en formato PDF.

> **Como** personal directivo de taller (mecánico jefe y dueño de taller), **quiero** supervisar el estado de las bahías en tiempo real, configurar parámetros fiscales SUNAT y exportar reportes de flujo de caja e informes periciales en formato PDF, **para** optimizar el flujo físico de unidades en patio y gobernar la gestión financiera y comercial del negocio automotriz

<a id="us40"></a>

### US40 — Supervisión y reasignación de bahías de servicio en tiempo real

**Usuario:** Mecánico jefe · **Prioridad:** Media · **Épica:** EP09 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** mecánico jefe que supervisa el flujo operativo de las bahías de servicio en tiempo real, **quiero** visualizar la ocupación de las bahías mecánicas y transferir una orden de trabajo hacia un puesto desocupado, **para** asegurar la continuidad de las tareas ante imprevistos, averías en elevadores o redistribución de labores en patio

**Criterios de aceptación**

*Escenario 1: Reasignación exitosa de bahía por contingencia mecánica*

- **Dado que** una orden de trabajo se encuentra en ejecución en la bahía de servicio número uno y surge una eventualidad que exige reubicar el vehículo
- **Cuando** el administrador selecciona una bahía de destino desocupada de tipo compatible y confirma el traslado en el sistema
- **Entonces** el sistema actualiza la asignación de la orden de trabajo hacia la nueva bahía, libera el puesto de origen dejándolo disponible
- **Y** notifica en tiempo real al técnico mecánico responsable para continuar las tareas en la nueva ubicación

*Escenario 2: Restricción de reasignación hacia bahía ocupada o inhabilitada*

- **Dado que** el administrador intenta trasladar un vehículo hacia un puesto físico que se encuentra ocupado por otra unidad o bajo mantenimiento preventivo
- **Cuando** solicita la reasignación en el tablero de patio
- **Entonces** el sistema bloquea la operación indicando la indisponibilidad de la bahía seleccionada
- **Y** mantiene la asignación previa del vehículo y sus faenas sin alterar los registros de ejecución

*Escenario 3: Despliegue en tiempo real del tablero de ocupación de bahías*

- **Dado que** el personal de supervisión ingresa al módulo de monitoreo de patio de la sucursal activa
- **Cuando** el sistema consulta el estado de la infraestructura del taller
- **Entonces** despliega la lista consolidada de bahías con su indicador de disponibilidad (disponible, ocupada o en mantenimiento)
- **Y** muestra en las bahías ocupadas la placa del vehículo en atención, el mecánico a cargo y el tiempo transcurrido de labor

<a id="us41"></a>

### US41 — Exportación de reporte de flujo de caja en PDF para auditoría contable

**Usuario:** Dueño de taller · **Prioridad:** Alta · **Épica:** EP09 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** dueño de taller automotriz que realiza la auditoría contable y financiera, **quiero** generar y descargar el reporte consolidado de flujo de caja e ingresos fiscales en formato PDF, **para** entregarlo al contador externo de la empresa y facilitar la liquidación tributaria sin requerir que acceda a la plataforma Atelier

**Criterios de aceptación**

*Escenario 1: Descarga exitosa de flujo de caja mensual en documento PDF*

- **Dado que** el dueño de taller accede al módulo financiero seleccionando el mes fiscal y la sucursal de atención
- **Cuando** solicita la exportación del estado de flujo de caja en formato PDF
- **Entonces** el sistema consolida los cobros registrados y comprobantes electrónicos emitidos en el intervalo, compila el archivo PDF con membrete institucional, desglose de ingresos por mano de obra y repuestos y total de IGV retenido
- **Y** descarga el documento listo para su remisión al estudio contable externo

*Escenario 2: Reporte multisede consolidado para balance anual*

- **Dado que** la empresa automotriz opera múltiples sucursales físicas y se requiere consolidar el ejercicio contable
- **Cuando** el dueño selecciona la opción de balance corporativo para un rango de fechas personalizado
- **Entonces** el sistema agrega los flujos de caja de todas las sedes discriminando la recaudación por taller
- **Y** emite el documento PDF consolidado con los totales generales de facturación y tributos

<a id="us42"></a>

### US42 — Configuración de información fiscal y series SUNAT con PSE administrado por la plataforma

**Usuario:** Dueño de taller · **Prioridad:** Alta · **Épica:** EP09 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** dueño de taller automotriz responsable del cumplimiento tributario institucional, **quiero** registrar la razón social, el RUC y las series correlativas autorizadas de cada sede, sin gestionar credenciales propias ante el PSE porque la conexión con Nubefact la administra la plataforma Atelier, **para** empezar a facturar apenas completo el registro del taller y asegurar la validez de los comprobantes electrónicos ante la SUNAT

**Criterios de aceptación**

*Escenario 1: Activación de series tras el registro del taller*

- **Dado que** el taller quedó registrado con su RUC y razón social y el dueño configura una serie autorizada para su sede
- **Cuando** el sistema registra la serie usando la conexión con el PSE administrada por la plataforma
- **Entonces** activa las series alfanuméricas autorizadas vinculándolas a la sucursal de trabajo
- **Y** habilita la expedición regular de comprobantes electrónicos en las órdenes de servicio

*Escenario 2: Rechazo de series duplicadas*

- **Dado que** el dueño intenta registrar una serie que ya existe para la misma sede
- **Cuando** el sistema valida la configuración tributaria
- **Entonces** rechaza el registro informando que la serie ya está configurada
- **Y** previene correlativos duplicados ante la SUNAT

<a id="us43"></a>

### US43 — Descarga y remisión de informe pericial de salud vehicular en PDF

**Usuario:** Asesor de servicio · **Prioridad:** Media · **Épica:** EP09 · **Sprint:** Sprint 3 · **Story points:** 3

**Como** asesor de servicio que requiere descargar y remitir el informe pericial de salud vehicular al cliente, **quiero** descargar en formato PDF el informe pericial de salud vehicular generado por el modelo de IA a partir de telemetría y códigos DTC, **para** remitirlo al cliente o gestor de flota corporativa y justificar técnicamente el presupuesto de mantenimiento preventivo

**Criterios de aceptación**

*Escenario 1: Generación y descarga de informe pericial en PDF maquetado*

- **Dado que** se completó la inferencia del modelo generativo con los datos de telemetría y códigos de avería del vehículo
- **Cuando** el dueño de taller solicita la descarga documental del informe de salud automotriz
- **Entonces** el sistema compila un documento PDF institucional con membrete, semáforos de criticidad por subsistema, averías explicadas en lenguaje accesible y recomendaciones preventivas
- **Y** genera el archivo descargable para su envío directo al cliente por medios digitales

*Escenario 2: Consolidado ejecutivo para flota corporativa*

- **Dado que** una empresa de transporte mantiene múltiples unidades atendidas en el taller
- **Cuando** el dueño solicita el diagnóstico consolidado de la flota vehicular
- **Entonces** el sistema emite un informe agrupado en PDF destacando los vehículos en estado crítico y el presupuesto preventivo asociado
- **Y** habilita la remisión formal al responsable de mantenimiento de la flota comercial

---

## Apéndice — EP11 (landing web, fuera de alcance de la app móvil)

Estas historias pertenecen al sitio web comercial. Se listan solo como referencia: US48 lleva al flujo de registro (`POST /api/v1/auth/sign-up` con `planId` opcional), que la app móvil también puede ofrecer.


- **Área:** Comercial y Adquisición Digital
- **Rol:** Dueño de taller
- **Prioridad:** Alta
- **Alcance:** Presentación de la propuesta de valor de Atelier, comparativa interactiva de planes y proyección de ahorros, resolución de dudas operativas frecuentes y redirección hacia el flujo de alta de cuenta en la aplicación principal.

> **Como** dueño de taller, **quiero** comprender con rapidez el propósito de Atelier y su solución a los problemas de desorden operativo, pérdidas en repuestos y diagnóstico vehicular, comparar las opciones comerciales y resolver dudas técnicas en el portal web, **para** determinar si el ecosistema es valioso para mi negocio y decidir iniciar mi prueba en la plataforma

#### US45 — Presentación de la propuesta de valor central del ecosistema Atelier

**Usuario:** Dueño de taller · **Prioridad:** Alta · **Épica:** EP11 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** dueño de taller automotriz que explora soluciones digitales para su negocio, **quiero** que el sistema presente con claridad el contraste entre la gestión tradicional de un taller y la digitalización integral de Atelier, **para** entender de inmediato los problemas operativos que resuelve la plataforma y evaluar su pertinencia para mi negocio

**Criterios de aceptación**

*Escenario 1: Comprensión del propósito operativo general*

- **Dado que** el dueño de taller accede a la página principal del ecosistema
- **Cuando** explora la introducción del servicio
- **Entonces** el sistema presenta el contraste entre las pérdidas por descontrol de inventario y desorden en patio frente a la rentabilidad garantizada por el control exacto de repuestos y la sincronización en tiempo real
- **Y** destaca la capacidad de la plataforma para operar sin conexión a internet en fosas mecánicas

*Escenario 2: Exploración de capacidades tecnológicas diferenciales*

- **Dado que** el dueño de taller busca modernizar la atención técnica hacia sus clientes
- **Cuando** examina las capacidades centrales destacadas en la introducción
- **Entonces** el sistema expone el funcionamiento del diagnóstico vehicular en vivo y la generación de reportes preventivos con Inteligencia Artificial
- **Y** confirma la validez de los comprobantes electrónicos emitidos ante la entidad tributaria nacional

#### US46 — Exploración y comparativa dinámica de planes comerciales según periodicidad de facturación

**Usuario:** Dueño de taller · **Prioridad:** Alta · **Épica:** EP11 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** dueño de taller automotriz interesado en evaluar la rentabilidad del servicio, **quiero** consultar las tarifas y capacidades de los planes comerciales alternando entre frecuencia de facturación mensual y anual en el portal web, **para** identificar el nivel de servicio adecuado a mi presupuesto operativo y proyectar el ahorro económico por pago adelantado

**Criterios de aceptación**

*Escenario 1: Proyección de tarifas con frecuencia anual y bonificación de ahorro*

- **Dado que** el dueño de taller consulta las alternativas de suscripción en el sitio web
- **Cuando** selecciona la periodicidad de facturación anual
- **Entonces** el sistema actualiza la tarifa mensual equivalente a ciento nueve soles para el Plan Go, doscientos diecinueve soles para el Plan Pro y trescientos noventa y nueve soles para el Plan Max
- **Y** detalla el monto consolidado anualizado reflejando un ahorro equivalente a más de dos meses de servicio libre de costo

*Escenario 2: Despliegue de tarifas mensuales estándar con renovación flexible*

- **Dado que** el dueño de taller requiere flexibilidad en sus pagos operativos mes a mes
- **Cuando** selecciona la periodicidad de facturación mensual
- **Entonces** el sistema ajusta las tarifas a ciento treinta y nueve soles para el Plan Go, doscientos sesenta y nueve soles para el Plan Pro y cuatrocientos ochenta y nueve soles para el Plan Max
- **Y** notifica que el servicio opera bajo renovación de treinta días con libertad de cancelación o cambio de nivel en cualquier momento

*Escenario 3: Comparación progresiva de beneficios acumulativos entre planes*

- **Dado que** el dueño de taller evalúa las diferencias operativas entre niveles contiguos
- **Cuando** solicita el desglose de capacidades incluidas
- **Entonces** el sistema expone que cada plan superior conserva la totalidad de las funciones del plan anterior
- **Y** resalta la adición de escáneres en vivo, reportes con Inteligencia Artificial o soporte multi-sucursal según el nivel consultado

#### US47 — Resolución interactiva de dudas operativas y condiciones del servicio

**Usuario:** Dueño de taller · **Prioridad:** Media · **Épica:** EP11 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** dueño de taller automotriz que evalúa los requisitos técnicos y operativos de la plataforma, **quiero** consultar las respuestas a preguntas frecuentes sobre conectividad en fosa, escáneres compatibles, propiedad de los datos y políticas de cancelación en el portal web, **para** despejar mis inquietudes técnicas y contractuales antes de iniciar la adopción de la plataforma

**Criterios de aceptación**

*Escenario 1: Verificación del funcionamiento sin conexión a internet en áreas de fosa*

- **Dado que** el dueño de taller tiene dudas sobre la calidad de señal inalámbrica en sus instalaciones subterráneas o fosa de trabajo
- **Cuando** consulta las especificaciones de conectividad en el sitio web
- **Entonces** el sistema explica el mecanismo de sincronización local que permite a los técnicos trabajar sin conexión y transmitir los datos automáticamente al recuperar la señal
- **Y** garantiza que ninguna tarea ni registro fotográfico se pierde por interrupciones de internet

*Escenario 2: Comprobación de compatibilidad con escáneres de diagnóstico estándar*

- **Dado que** el dueño de taller desea saber si requiere adquirir hardware propietario de alto costo
- **Cuando** revisa los requerimientos de escaneo vehicular
- **Entonces** el sistema detalla la compatibilidad abierta con adaptadores estándar Bluetooth del mercado
- **Y** asegura que no es necesaria la compra de equipos exclusivos para utilizar las funciones de diagnóstico en vivo

*Escenario 3: Verificación de políticas de cancelación y exportación de datos contables*

- **Dado que** el dueño de taller busca certificar la libertad comercial del servicio
- **Cuando** consulta las condiciones de permanencia y resguardo de información
- **Entonces** el sistema confirma la ausencia de penalidades por cancelación anticipada
- **Y** garantiza que los registros históricos de clientes, vehículos e inventarios pueden exportarse íntegramente en formatos estándar en cualquier momento

#### US48 — Redirección y transición hacia el flujo de registro de prueba gratuita en la aplicación principal

**Usuario:** Dueño de taller · **Prioridad:** Alta · **Épica:** EP11 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** dueño de taller automotriz convencido de profesionalizar la gestión de su negocio, **quiero** disponer de un acceso directo para activar la prueba gratuita de catorce días desde el plan elegido en el portal web, **para** transicionar de forma transparente desde el sitio informativo hacia el flujo de alta de taller en la aplicación principal sin ingresar tarjeta de crédito

**Criterios de aceptación**

*Escenario 1: Transición hacia el registro de cuenta con selección de plan*

- **Dado que** el dueño de taller ha evaluado la propuesta de valor y las opciones comerciales en el portal web
- **Cuando** decide iniciar su experiencia con un plan específico
- **Entonces** el sistema transfiere la navegación hacia la dirección del módulo de registro de la aplicación principal conservando el identificador del plan seleccionado
- **Y** habilita el formulario de creación de organización en modo de prueba gratuita de catorce días sin requerir datos de medios de pago

*Escenario 2: Retención de parámetros de frecuencia de facturación en la transición*

- **Dado que** el dueño de taller seleccionó una periodicidad anual con descuento en el comparador de planes
- **Cuando** ejecuta la acción de inicio de prueba gratuita
- **Entonces** el sistema transfiere el contexto de la modalidad de cobro elegida hacia el módulo de alta
- **Y** preconfigura la suscripción proyectada respetando la bonificación económica anual para cuando el taller decida continuar con el servicio remunerado

