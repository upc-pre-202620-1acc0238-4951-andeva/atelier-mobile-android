# Historias técnicas (TS) — Servicios REST del backend

Versión limpia de las historias técnicas de la sección 2.4.1 del informe. Describen el contrato que el backend ya cumple; la referencia viva de cada endpoint está en [../api/](../api/README.md).

## EP10 — Servicios e Interfaces RESTful del Backend

- **Área:** Servicios de Plataforma (API)
- **Rol:** Ingeniero de software
- **Prioridad:** Alta
- **Alcance:** Exposición de endpoints RESTful seguros con autenticación JWT, persistencia transaccional, validación de esquemas y trazabilidad de eventos.

> **Como** ingeniero de software, **quiero** implementar endpoints RESTful seguros, estandarizados bajo el protocolo HTTP y con contratos JSON normalizados en la capa de interfaces del backend, **para** proveer servicios transaccionales de alta disponibilidad, aislamiento multi-inquilino y respuesta predecible a las aplicaciones web y móviles de la plataforma Atelier

### TS01 — Autenticación de credenciales y expedición de tokens JWT

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/auth/sign-in` con verificación criptográfica de credenciales, **para** autenticar al usuario y expedir tokens JWT de acceso y renovación con contexto de inquilino validado

**Criterios de aceptación**

*Escenario 1: Autenticación exitosa y emisión de par de tokens*

- **Dado que** la API recibe una solicitud HTTP POST con cuerpo JSON válido conteniendo correo electrónico y contraseña registrados
- **Cuando** el servicio de autenticación valida las credenciales y corrobora que la cuenta y la membresía laboral se encuentran activas
- **Entonces** responde con código HTTP 200 OK retornando el identificador del usuario, la razón social del taller y los tokens JWT de acceso y renovación
- **Y** asienta el evento de acceso exitoso en la bitácora de auditoría

*Escenario 2: Rechazo por credenciales erróneas o cuenta inexistente*

- **Dado que** la API recibe una solicitud HTTP POST con una clave incorrecta o un correo no registrado
- **Cuando** el subsistema de seguridad verifica la correspondencia criptográfica del hash de la contraseña
- **Entonces** responde con código HTTP 401 Unauthorized y un cuerpo estructurado con el mensaje de credenciales no válidas
- **Y** bloquea la generación de cualquier token de sesión

### TS02 — Registro fundacional de organización y aprovisionamiento de inquilino

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/auth/sign-up` para crear atómicamente el taller y el usuario administrador, **para** aprovisionar el inquilino en la base de datos con aislamiento multi-inquilino y despachar el código OTP de verificación

**Criterios de aceptación**

*Escenario 1: Aprovisionamiento exitoso de nuevo taller automotriz*

- **Dado que** se recibe una petición HTTP POST con el RUC de la empresa, denominación comercial y los datos de identidad y contraseña del titular del taller
- **Cuando** el servicio comprueba que ni el RUC ni el correo electrónico existen previamente en la plataforma
- **Entonces** persiste atómicamente el registro del inquilino, crea la cuenta del administrador con membresía activa
- **Y** responde con código HTTP 201 Created despachando un código OTP de seis dígitos al correo electrónico registrado

*Escenario 2: Rechazo por duplicidad de documento tributario*

- **Dado que** se recibe una solicitud con un RUC que ya se encuentra registrado por otra empresa automotriz
- **Cuando** el validador de dominio detecta la colisión de unicidad tributaria
- **Entonces** responde con código HTTP 409 Conflict y el detalle del recurso en conflicto
- **Y** revierte cualquier operación de persistencia asociada

### TS03 — Verificación de correo y activación mediante código OTP

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/auth/verify-email` para validar el código numérico recibido por correo, **para** transicionar la cuenta del usuario a estado verificado y autorizar su acceso formal a los servicios del sistema

**Criterios de aceptación**

*Escenario 1: Verificación satisfactoria de código OTP vigente*

- **Dado que** se recibe una petición HTTP POST con el correo electrónico del usuario y el código OTP de seis dígitos antes de su vencimiento
- **Cuando** el servicio valida la firma temporal y la coincidencia exacta con el código emitido
- **Entonces** conmuta la cuenta a estado verificado, anula el código OTP para evitar reutilizaciones
- **Y** responde con código HTTP 200 OK habilitando el acceso a la plataforma

*Escenario 2: Rechazo de código OTP caducado o discrepante*

- **Dado que** se recibe un código OTP numérico que no coincide con el registrado o que superó su ventana de vigencia de quince minutos
- **Cuando** el servicio efectúa la comprobación criptográfica
- **Entonces** responde con código HTTP 400 Bad Request detallando la anomalía
- **Y** mantiene la cuenta en estado pendiente de verificación

### TS04 — Emisión y despacho de invitaciones corporativas de personal

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful \texttt{POST /api/v1/invitations/tenant/{tenantId}} protegido por perfil de administrador, **para** generar tokens de invitación firmados y enviar correos transaccionales a nuevos colaboradores del taller

**Criterios de aceptación**

*Escenario 1: Emisión exitosa de invitación laboral*

- **Dado que** un usuario autenticado con rol administrativo envía una petición HTTP POST con el correo electrónico del colaborador, rol asignado y sucursal de destino
- **Cuando** el servicio valida que el destinatario no posee una membresía activa en el taller ni una invitación previa vigente
- **Entonces** genera un token criptográfico con vigencia de cuarenta y ocho horas, despacha el correo mediante la API de Resend
- **Y** responde con código HTTP 201 Created retornando los metadatos de la invitación

*Escenario 2: Denegación de emisión por invitación pendiente duplicada*

- **Dado que** se intenta emitir una invitación hacia un correo electrónico que ya cuenta con una invitación pendiente en la misma organización
- **Cuando** el validador de dominio constata la existencia de la invitación activa
- **Entonces** responde con código HTTP 409 Conflict informando la duplicidad
- **Y** bloquea la generación de un nuevo token

### TS05 — Alta de sucursales operativas y delimitación de geocercas satelitales

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/branches`, **para** registrar sedes físicas con código tributario SUNAT, coordenadas geográficas WGS84 y radio perimétrico en metros

**Criterios de aceptación**

*Escenario 1: Registro exitoso de sucursal física con geocerca válida*

- **Dado que** se recibe una petición HTTP POST con nombre de sucursal, código tributario de anexo SUNAT, latitud, longitud y radio en metros no inferior a cincuenta
- **Cuando** el servicio de infraestructura valida los límites de suscripción del taller y la consistencia geodésica de las coordenadas
- **Entonces** persiste la sucursal vinculada a la organización
- **Y** responde con código HTTP 201 Created retornando el identificador y los datos del local registrado

*Escenario 2: Rechazo por coordenadas geográficas fuera de rango*

- **Dado que** la solicitud contiene valores de latitud o longitud fuera de los rangos válidos o un radio perimétrico negativo
- **Cuando** el componente de validación inspecciona el cuerpo de la petición
- **Entonces** responde con código HTTP 400 Bad Request detallando los errores de validación
- **Y** descarta la persistencia de la sucursal

### TS06 — Alta de clientes individuales con validación de identidad

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/customers/individuals`, **para** incorporar personas naturales a la cartera de clientes con validación formal de DNI y canales de contacto directo

**Criterios de aceptación**

*Escenario 1: Alta exitosa de cliente persona natural*

- **Dado que** se recibe una petición HTTP POST autenticada con cabecera de inquilino que incluye tipo de documento, número de DNI de ocho dígitos, nombres, apellidos y teléfono
- **Cuando** el servicio comprueba que el cliente no se encuentra registrado en el taller bajo el mismo documento
- **Entonces** persiste el registro del cliente en la base de datos
- **Y** responde con código HTTP 201 Created retornando el recurso creado

*Escenario 2: Conflicto por documento de identidad duplicado en la organización*

- **Dado que** se envía una petición con un número de documento que ya pertenece a un cliente registrado en el mismo taller
- **Cuando** el repositorio ejecuta la comprobación de unicidad scoped al inquilino
- **Entonces** responde con código HTTP 409 Conflict retornando el identificador del cliente preexistente
- **Y** no genera registros redundantes

### TS07 — Registro técnico de vehículos con homologación de placa y VIN

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/vehicles`, **para** dar de alta unidades en el parque automotor validando la placa de rodaje y el número de chasis según norma ISO 3779

**Criterios de aceptación**

*Escenario 1: Registro exitoso de automóvil con validación técnica*

- **Dado que** se recibe una petición HTTP POST con placa de rodaje válida, número VIN de diecisiete caracteres alfanuméricos, marca, modelo, año y kilometraje actual
- **Cuando** el servicio de gestión de flotas valida la estructura del VIN y la normalización de la placa en mayúsculas
- **Entonces** almacena el vehículo asociándolo opcionalmente al cliente custodio inicial
- **Y** responde con código HTTP 201 Created con el recurso del vehículo registrado

*Escenario 2: Rechazo por formato de VIN o placa inválidos*

- **Dado que** la solicitud contiene un VIN con longitud diferente a diecisiete caracteres o caracteres no permitidos
- **Cuando** el validador sintáctico procesa los campos de entrada
- **Entonces** responde con código HTTP 400 Bad Request describiendo la infracción de formato
- **Y** desestima el registro

### TS08 — Agendamiento y reserva de citas de mantenimiento

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/appointments`, **para** programar atenciones preventivas y correctivas verificando la disponibilidad horaria en la sucursal de destino

**Criterios de aceptación**

*Escenario 1: Agendamiento exitoso de cita técnica*

- **Dado que** se recibe una petición HTTP POST con identificadores de cliente y vehículo, fecha y hora futura de atención, sucursal y motivo de servicio
- **Cuando** el servicio corrobora la disponibilidad horaria dentro de la jornada laboral de la sede física
- **Entonces** genera la cita en estado programada
- **Y** responde con código HTTP 201 Created despachando la confirmación preliminar

*Escenario 2: Rechazo por sobreposición o indisponibilidad horaria*

- **Dado que** la solicitud especifica una franja horaria que ya ha alcanzado el límite máximo de atenciones simultáneas en la sucursal
- **Cuando** el motor de agendamiento evalúa la capacidad de patio
- **Entonces** responde con código HTTP 409 Conflict informando la falta de cupo
- **Y** sugiere la siguiente franja horaria disponible

### TS09 — Registro de arribo a patio y apertura automática de orden

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful \texttt{POST /api/v1/appointments/{id}/arrive}, **para** registrar el ingreso físico del automóvil al patio y disparar la creación automática de la orden de trabajo

**Criterios de aceptación**

*Escenario 1: Arribo exitoso y apertura de orden de trabajo*

- **Dado que** el asesor de servicio envía una petición HTTP POST con el kilometraje y nivel de combustible de recepción del vehículo
- **Cuando** el servicio de citas verifica que la cita se encuentra en estado programada o confirmada
- **Entonces** transiciona la cita a estado en recepción, despacha un evento de dominio y abre una nueva orden de trabajo en estado borrador vinculando al vehículo y asesor
- **Y** responde con código HTTP 200 OK retornando los identificadores de la cita y de la orden generada

*Escenario 2: Rechazo por cita cancelada o previamente atendida*

- **Dado que** se intenta procesar el arribo sobre una cita que ya fue atendida con anterioridad o que se encuentra en estado cancelada
- **Cuando** el servicio comprueba la máquina de estados de la cita
- **Entonces** responde con código HTTP 422 Unprocessable Entity indicando la inviabilidad de la transición
- **Y** no altera los registros de operaciones

### TS10 — Apertura y parametrización de órdenes de trabajo en taller

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/work-orders`, **para** abrir órdenes de servicio registrando vehículo, sucursal, asesor responsable, kilometraje y notas de recepción

**Criterios de aceptación**

*Escenario 1: Creación formal de orden de trabajo*

- **Dado que** se recibe una petición HTTP POST con los identificadores de vehículo, cliente, sucursal, kilometraje de entrada e inventario visual de recepción
- **Cuando** el servicio de operaciones MRO valida la existencia de los recursos y la integridad de los datos de ingreso
- **Entonces** persiste la orden de trabajo en estado borrador asignando un correlativo operativo secuencial único
- **Y** responde con código HTTP 201 Created con el detalle de la orden

*Escenario 2: Rechazo por orden de trabajo activa preexistente*

- **Dado que** se intenta abrir una orden de trabajo para un vehículo que ya mantiene otra orden en estado abierta o en progreso dentro del taller
- **Cuando** el repositorio constata la existencia de un servicio activo no finiquitado para dicha placa
- **Entonces** responde con código HTTP 409 Conflict señalando la orden en curso
- **Y** bloquea la apertura duplicada

### TS11 — Asignación y conmutación de bahías de servicio en orden

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 5

**Como** Developer, **quiero** exponer el endpoint RESTful `PUT /api/v1/work-orders/{id\`/bay}, **para** vincular una bahía de servicio a la orden de trabajo y actualizar concurrentemente los estados de ocupación de patio

**Criterios de aceptación**

*Escenario 1: Asignación exitosa de bahía desocupada*

- **Dado que** se envía una petición HTTP PUT con el identificador de una bahía disponible compatible con el tipo de intervención requerida
- **Cuando** el servicio de operaciones comprueba que la bahía se encuentra desocupada en la sucursal
- **Entonces** actualiza la orden de trabajo vinculando el puesto físico y conmuta el estado de la bahía a ocupada
- **Y** responde con código HTTP 200 OK con el recurso actualizado

*Escenario 2: Conflicto por intento de asignación a bahía ocupada*

- **Dado que** se envía la petición apuntando a una bahía que ya aloja a otro vehículo en ejecución
- **Cuando** el gestor de concurrencia detecta el estado ocupado del puesto
- **Entonces** responde con código HTTP 409 Conflict informando la indisponibilidad de la bahía
- **Y** mantiene inalterada la asignación previa de la orden

### TS12 — Incorporación de tareas técnicas a la orden de trabajo

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 5

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/work-orders/{id\`/tasks}, **para** anexar labores mecánicas a la orden copiando el precio base del servicio y recalculando el subtotal acumulado

**Criterios de aceptación**

*Escenario 1: Inclusión exitosa de tarea técnica con servicio de catálogo*

- **Dado que** se recibe una petición HTTP POST con el identificador de un servicio del catálogo, mecánico asignado y notas técnicas complementarias
- **Cuando** el servicio verifica que la orden está en estado borrador o en progreso, copia el precio base del servicio a la tarea y crea el registro en estado pendiente
- **Entonces** recalcula el subtotal acumulado de la orden sumando el precio de la nueva faena
- **Y** responde con código HTTP 201 Created con el recurso de la tarea creada

*Escenario 2: Rechazo por adición de tarea en orden completada*

- **Dado que** se intenta agregar una tarea a una orden que se encuentra en estado completada, pagada o cancelada
- **Cuando** el servicio valida las invariantes de inmutabilidad del ciclo de vida
- **Entonces** responde con código HTTP 422 Unprocessable Entity denegando la mutación
- **Y** no modifica la composición de la orden

### TS13 — Control cronometrado y finalización técnica de faenas

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/tasks/{id\`/complete}, **para** culminar técnicamente una tarea mecánica, registrar la duración efectiva y evaluar la transición automática de la orden a completada

**Criterios de aceptación**

*Escenario 1: Cierre técnico de tarea y verificación de orden completada*

- **Dado que** el mecánico asignado envía una petición HTTP POST para marcar la tarea como culminada
- **Cuando** el servicio valida que la labor se encontraba en progreso, sella la estampa cronológica de fin y calcula la duración real
- **Entonces** conmuta la tarea a completada, constata si todas las demás tareas de la orden están culminadas para transicionar la orden a completada automáticamente
- **Y** responde con código HTTP 200 OK con el recurso actualizado

*Escenario 2: Rechazo de finalización sobre tarea no iniciada*

- **Dado que** se intenta completar una labor mecánica que permanece en estado pendiente sin haber iniciado previamente su ejecución
- **Cuando** el motor de estados de tareas valida la precondición operativa
- **Entonces** responde con código HTTP 422 Unprocessable Entity indicando que la labor debe iniciarse antes de su cierre
- **Y** descarta la transacción

### TS14 — Requisición y descargo de repuestos bajo método FIFO

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/tasks/{id\`/products}, **para** vincular repuestos a una faena, asignando el precio base del catálogo al cliente y descargando existencias de lotes bajo orden FIFO estricto

**Criterios de aceptación**

*Escenario 1: Descargo exitoso de repuesto con stock suficiente*

- **Dado que** se recibe una petición HTTP POST con el identificador del ítem de inventario y la cantidad requerida para la faena
- **Cuando** el servicio de almacén verifica stock global suficiente, copia el precio base del repuesto como precio unitario a cobrar y deduce físicamente las unidades de los lotes de compra según su antigüedad de ingreso FIFO
- **Entonces** vincula el repuesto a la tarea, recalcula el importe de la faena y el subtotal de la orden de trabajo
- **Y** responde con código HTTP 201 Created retornando el detalle de la asignación

*Escenario 2: Rechazo por insuficiencia de existencias en almacén*

- **Dado que** la cantidad solicitada supera el inventario disponible consolidado en los lotes activos de la sucursal
- **Cuando** el asignador de stock evalúa la disponibilidad
- **Entonces** responde con código HTTP 409 Conflict indicando el stock disponible insuficiente
- **Y** no afecta las existencias de inventario ni modifica la tarea

### TS15 — Catálogo de repuestos e insumos con precio base y umbral crítico

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/inventory/items`, **para** dar de alta repuestos e insumos registrando código SKU, descripción, precio base al público y umbral mínimo de seguridad

**Criterios de aceptación**

*Escenario 1: Alta exitosa de artículo en catálogo*

- **Dado que** se recibe una petición HTTP POST con código SKU, descripción, marca, categoría, precio base de venta mayor a cero y nivel de stock mínimo de seguridad
- **Cuando** el servicio comprueba que el SKU no existe previamente en la organización del taller
- **Entonces** persiste el artículo de inventario con stock contable en cero a la espera de recepciones por compra
- **Y** responde con código HTTP 201 Created retornando los datos del recurso creado

*Escenario 2: Rechazo por colisión de SKU dentro de la organización*

- **Dado que** se envía una petición con un código SKU que ya fue asignado a otro producto en el mismo taller
- **Cuando** la restricción de integridad scoped al inquilino detecta la duplicidad
- **Entonces** responde con código HTTP 409 Conflict informando la colisión del código de parte
- **Y** deniega el guardado

### TS16 — Ingreso y valorización de lotes de repuestos por adquisición

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/inventory/items/{id\`/batches}, **para** asentar ingresos físicos de mercadería registrando costo unitario de compra y cantidad recibida para la cola de consumo FIFO

**Criterios de aceptación**

*Escenario 1: Asentamiento exitoso de lote de compras*

- **Dado que** se recibe una petición HTTP POST con el identificador del ítem, costo unitario de compra mayor a cero y cantidad de unidades ingresadas
- **Cuando** el servicio de logística valida la existencia del producto y la validez económica de los importes
- **Entonces** crea el lote con estampa de tiempo actual, incrementa el stock contable total del artículo y posiciona el lote en la cola de consumo FIFO
- **Y** responde con código HTTP 201 Created con el recurso del lote generado

*Escenario 2: Rechazo por costo unitario de compra o cantidad negativa*

- **Dado que** la solicitud contiene un costo unitario o una cantidad de unidades menor o igual a cero
- **Cuando** el interceptor de validación analiza los campos numéricos
- **Entonces** responde con código HTTP 400 Bad Request detallando los errores de restricción
- **Y** no altera los registros de almacén

### TS17 — Marcación geodésica de jornada laboral con validación Haversine

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/hr/attendances/clock-in`, **para** registrar la asistencia del personal comprobando la proximidad satelital a la sucursal mediante la fórmula de Haversine

**Criterios de aceptación**

*Escenario 1: Marcación exitosa de ingreso dentro del radio perimétrico*

- **Dado que** un colaborador autenticado envía una petición HTTP POST con su membresía, latitud y longitud GPS capturadas en su dispositivo móvil
- **Cuando** el servicio calcula la distancia geodésica respecto a la sucursal asignada mediante la fórmula de Haversine y corrobora que no supera el radio de tolerancia configurado
- **Entonces** asienta la marcación de ingreso laboral con estampa cronológica inmutable
- **Y** responde con código HTTP 201 Created confirmando el registro de asistencia

*Escenario 2: Rechazo de marcación por ubicación fuera de la geocerca*

- **Dado que** las coordenadas GPS enviadas sitúan al colaborador fuera del radio perimétrico autorizado de la sede física
- **Cuando** el algoritmo de Haversine constata la desviación espacial
- **Entonces** responde con código HTTP 422 Unprocessable Entity indicando la distancia excedente
- **Y** rechaza el registro de la marcación laboral

### TS18 — Emisión y timbrado de comprobantes electrónicos UBL 2.1

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/invoicing/vouchers`, **para** generar Boletas o Facturas electrónicas desde órdenes completadas, calculando impuestos y transmitiendo el XML UBL 2.1 a SUNAT

**Criterios de aceptación**

*Escenario 1: Emisión exitosa de comprobante tributario*

- **Dado que** se recibe una petición HTTP POST con el identificador de una orden de trabajo completada, tipo de comprobante (Boleta o Factura) y datos fiscales del receptor
- **Cuando** el servicio de facturación reserva el correlativo fiscal de la serie, calcula la base imponible y el dieciocho por ciento de IGV, genera el XML firmado UBL 2.1 y lo despacha al PSE Nubefact
- **Entonces** persiste el comprobante electrónico enlazado a la orden y almacena el enlace de descarga del archivo XML y la constancia de recepción CDR
- **Y** responde con código HTTP 201 Created con el resumen del comprobante emitido

*Escenario 2: Rechazo de emisión por orden de trabajo no completada*

- **Dado que** se intenta emitir un comprobante fiscal para una orden de trabajo que aún mantiene faenas en estado pendiente o en progreso
- **Cuando** el servicio valida las reglas de facturación del taller
- **Entonces** responde con código HTTP 422 Unprocessable Entity indicando que la orden no ha culminado sus labores técnicas
- **Y** no consume ningún número correlativo fiscal

### TS19 — Registro transaccional de pagos y liquidación de comprobantes

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/invoicing/payments`, **para** registrar cobros sobre un comprobante fiscal, actualizando el saldo adeudado y conmutando la orden a pagada al cubrirse el importe íntegro

**Criterios de aceptación**

*Escenario 1: Pago total del comprobante y liberación de pase de salida*

- **Dado que** se recibe una petición HTTP POST con el identificador del comprobante, método de pago e importe que salda el cien por ciento de la deuda pendiente
- **Cuando** el servicio registra la transacción financiera, deduce el saldo del comprobante hasta cero y transiciona la orden de trabajo vinculada a estado pagada
- **Entonces** emite digitalmente el pase de salida vehicular y libera la bahía ocupada
- **Y** responde con código HTTP 201 Created con la confirmación de la liquidación total

*Escenario 2: Rechazo por abono monetario superior al saldo adeudado*

- **Dado que** la solicitud especifica un monto de pago que excede el saldo remanente pendiente de liquidar en el comprobante electrónico
- **Cuando** el componente de validación financiera compara los importes
- **Entonces** responde con código HTTP 400 Bad Request informando que el abono supera la deuda
- **Y** revierte el registro del cobro

### TS20 — Ingesta masiva de telemetría vehicular IoT en series temporales

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 5

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/iot/telemetry/batch`, **para** recibir ráfagas de telemetría OBD-II e insertarlas de forma reactiva en la hipertabla TimescaleDB para análisis sensorial predictivo

**Criterios de aceptación**

*Escenario 1: Ingesta exitosa de ráfaga de telemetría sensorial*

- **Dado que** se recibe una petición HTTP POST autenticada con token de dispositivo que contiene entre una y cien lecturas de sensores (RPM, velocidad, temperatura del refrigerante y voltaje) con estampas de tiempo
- **Cuando** el controlador valida la estructura del payload y persiste concurrentemente el lote de mediciones en la hipertabla de TimescaleDB
- **Entonces** evalúa si algún parámetro sobrepasa los umbrales críticos de seguridad del motor
- **Y** responde de forma asíncrona con código HTTP 202 Accepted confirmando la ingesta de las muestras

*Escenario 2: Rechazo por lote vacío o ráfaga que excede el límite*

- **Dado que** la solicitud contiene un arreglo de telemetría sin elementos o con más de cien lecturas por paquete
- **Cuando** el validador perimetral inspecciona el tamaño de la carga útil
- **Entonces** responde con código HTTP 400 Bad Request señalando la infracción de volumen de datos
- **Y** no procesa la inserción en la base de datos de series temporales

### TS21 — Registro y catalogación de códigos de avería electrónica DTC

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 3

**Como** Developer, **quiero** exponer el endpoint RESTful `POST /api/v1/iot/faults`, **para** registrar códigos de falla electrónica DTC estandarizados bajo norma SAE J2012 vinculados a la ficha vehicular para diagnóstico en patio

**Criterios de aceptación**

*Escenario 1: Registro exitoso de código de falla vehicular DTC*

- **Dado que** se recibe una petición HTTP POST con el identificador del vehículo, código DTC normativo de cinco caracteres, descripción de la falla y nivel de criticidad
- **Cuando** el servicio comprueba la validez del identificador del automóvil y la existencia del subsistema de diagnóstico
- **Entonces** registra la avería electrónica en estado activa asociada a la ficha del vehículo para su atención en bahía
- **Y** responde con código HTTP 201 Created con el recurso de la falla asentada

*Escenario 2: Rechazo por formato de código DTC no estandarizado*

- **Dado que** se envía una petición con un código de falla que no sigue el formato normativo de cinco caracteres iniciando con las letras P, B, C o U
- **Cuando** la expresión regular de validación analiza el patrón textual
- **Entonces** responde con código HTTP 400 Bad Request indicando la anomalía de nomenclatura
- **Y** descarta el guardado de la falla

### TS22 — Generación de informe pericial asistido por IA predictiva

**Usuario:** Developer · **Prioridad:** Alta · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 5

**Como** Developer, **quiero** exponer el endpoint RESTful \texttt{POST /api/v1/iot/health-reports/generate?vehicleId={id}}, **para** extraer telemetría sensorial y fallas DTC, invocar al modelo de inteligencia artificial vía Spring AI y persistir el dictamen pericial con resumen y recomendaciones preventivas

**Criterios de aceptación**

*Escenario 1: Generación exitosa de informe de salud vehicular con IA*

- **Dado que** se recibe una petición HTTP POST con el identificador del vehículo y los parámetros de ponderación de diagnóstico
- **Cuando** el servicio extrae las series de telemetría de TimescaleDB y las fallas DTC activas, formula el prompt estructurado e invoca al modelo de lenguaje a través de Spring AI
- **Entonces** el modelo genera el dictamen estructurado con semáforo de criticidad y recomendaciones de mantenimiento preventivo
- **Y** la API persiste el informe de salud, emite la cabecera `Location` y responde con código HTTP 201 Created con el resumen pericial

*Escenario 2: Rechazo por datos de telemetría o fallas insuficientes*

- **Dado que** se solicita generar el informe para un vehículo que carece de registros de telemetría y códigos DTC en el intervalo de evaluación
- **Cuando** el servicio corrobora la disponibilidad de datos de entrada previa a la inferencia con el modelo generativo
- **Entonces** responde con código HTTP 422 Unprocessable Entity indicando la falta de información sensorial para el análisis predictivo
- **Y** no ejecuta llamadas externas hacia el proveedor de inteligencia artificial

### TS23 — Descarga documental de estado de flujo de caja en formato PDF

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful `GET /api/v1/invoicing/financial-reports/cash-flow/pdf`, **para** compilar y transmitir el balance de flujo de caja en un documento binario PDF para su uso por contadores externos

**Criterios de aceptación**

*Escenario 1: Transmisión binaria exitosa de documento PDF de flujo de caja*

- **Dado que** se recibe una petición HTTP GET con parámetros de consulta que especifican fecha inicial, fecha final y sucursal
- **Cuando** el servicio compila los registros de ingresos y egresos fiscales del intervalo y genera la plantilla tipográfica en formato binario PDF
- **Entonces** la API responde con código HTTP 200 OK, cabecera `Content-Type: application/pdf` y cabecera `Content-Disposition` que define el nombre del archivo descargable
- **Y** transmite el flujo de bytes del documento para su descarga directa

*Escenario 2: Rechazo por rango de fechas temporalmente invertido*

- **Dado que** los parámetros de consulta contienen una fecha de inicio posterior a la fecha de fin
- **Cuando** el validador de parámetros inspecciona los argumentos temporales de la petición
- **Entonces** la API responde con código HTTP 400 Bad Request detallando la incoherencia cronológica del intervalo
- **Y** no inicia la compilación del reporte PDF

### TS24 — Descarga documental de informe pericial de salud vehicular en PDF

**Usuario:** Developer · **Prioridad:** Media · **Épica:** EP10 · **Sprint:** Sprint 1 · **Story points:** 2

**Como** Developer, **quiero** exponer el endpoint RESTful \texttt{GET /api/v1/iot/health-reports/{reportId}/pdf}, **para** transmitir el archivo binario PDF maquetado con el dictamen pericial generado por IA y recomendaciones preventivas

**Criterios de aceptación**

*Escenario 1: Descarga exitosa de dictamen pericial en documento PDF*

- **Dado que** se recibe una petición HTTP GET con identificadores válidos de vehículo e informe pericial previamente generado
- **Cuando** el servicio de infraestructura renderiza la plantilla documental incorporando membrete del taller, semáforos de criticidad y recomendaciones de IA
- **Entonces** la API responde con código HTTP 200 OK con cabecera `Content-Type: application/pdf` y cabecera `Content-Disposition` para descarga adjunta
- **Y** transmite el documento binario completo

*Escenario 2: Notificación de recurso no encontrado por informe inexistente*

- **Dado que** se solicita el documento PDF para un identificador de informe que no existe o que no corresponde al vehículo indicado
- **Cuando** el repositorio ejecuta la búsqueda scoped en la base de datos
- **Entonces** la API responde con código HTTP 404 Not Found y una estructura `ProblemDetail` describiendo el recurso faltante
- **Y** no genera ningún archivo binario

