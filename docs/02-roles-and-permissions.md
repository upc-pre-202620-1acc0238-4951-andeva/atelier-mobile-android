# Roles y permisos (RBAC) — guía para la app móvil

Generado desde `PermissionCatalog.java` y `RoleTemplate.java` del backend (fuente de verdad). Descripción narrativa de cada rol: `docs/backend-documentation/atelier-roles.md` en `atelier-platform`.

## Reglas que la app debe seguir

1. **Decidir por permiso, nunca por rol.** Cada taller puede editar sus roles y crear roles personalizados; un "Mecánico" de un taller puede tener permisos distintos al de otro. Leer `permissions` de la respuesta de sesión (o del claim `permissions` del JWT) y preguntar `hasPermission("operations:tasks:track_time")`.
2. **Una persona puede tener varios roles** en su membresía; los permisos llegan ya unidos.
3. **El backend vuelve a validar todo.** Ocultar un botón es solo UX; si el permiso cambió desde el último login, el backend responde 403 y la app debe mostrar el `message`.
4. **Los permisos cambian solo al renovar la sesión** (sign-in o refresh). Tras un 403 inesperado conviene hacer un refresh y recalcular el menú.
5. `ROLE_*` también aparece dentro de `permissions` (para trazas y analítica); no lo uses para habilitar pantallas.

## Qué pantallas habilita cada permiso

| Pantalla / acción de la app | Permiso(s) requerido(s) | Historias |
| --- | --- | --- |
| Marcar ingreso / salida | `hr:attendance:clock_in`, `hr:attendance:clock_out` | US06–US08 |
| Mi asistencia de hoy / historial | `hr:attendance:read_own` | US06–US08 |
| Mi turno, tolerancia y días de descanso | `hr:shifts:read` (+ `hr:attendance:read_own` para mi perfil laboral) | US09 |
| Enviar descargo de tardanza | `hr:justifications:create` | US10 |
| Revisar descargos del equipo | `hr:justifications:approve` | US10 |
| Asistencia diaria de la sede | `hr:attendance:audit_all` | US10 |
| Buscar cliente / vehículo | `crm:customers:read`, `crm:vehicles:read` | US11, US44 |
| Registrar cliente / vehículo / afiliar | `crm:customers:create`, `crm:vehicles:create` | US11, US44 |
| Citas (agenda, llegada) | `crm:appointments:read`, `crm:appointments:update` | US11 |
| Abrir orden de trabajo y agregar tareas | `operations:work_orders:create` | US12 |
| Fotos de recepción | `operations:inspections:upload_photos` | US13 |
| Proponer tarea adicional | `operations:proposals:create` | US14 |
| Notificar / resolver propuesta | `operations:proposals:notify`, `operations:proposals:resolve` | US15 |
| Proforma (ver PDF, enviar, aprobar/rechazar) | `operations:quotations:read`, `operations:quotations:send`, `operations:quotations:approve` | US24, US25 |
| Imputar repuestos a mi tarea (mecánico) | `operations:tasks:charge_parts` | US23 E1 |
| Imputar / retirar repuestos de cualquier tarea | `operations:work_orders:update` | US23, US33 |
| Auditoría de reintegros y rentabilidad de la orden | `inventory:batches:read` (reintegros también con `inventory:batches:restore`; rentabilidad también con `invoicing:cashflow:export_pdf`) | US33 E2, US35 E2 |
| Mis tareas | `operations:tasks:read` | US26 |
| Iniciar / pausar / reanudar mi tarea | `operations:tasks:track_time` | US27 |
| Fotos de desmontaje / montaje | `operations:tasks:upload_photos` | US28 |
| Solicitar suspensión por pieza defectuosa | `operations:tasks:hold_request` | US29 |
| Aprobar / rechazar / levantar suspensión | `operations:tasks:hold_validate` | US29 |
| Cerrar tarea | `operations:tasks:complete` | US30 |
| Tablero de bahías / reasignar | `operations:bays:read`, `operations:bays:reassign` | US40 |
| Catálogo de repuestos | `inventory:parts:read` | US31 |
| Lotes FIFO y valorización | `inventory:batches:read` | US32, US35 |
| Alertas de stock mínimo | `inventory:alerts:read`, `inventory:alerts:resolve` | US34 |
| Escáner OBD-II (leer DTC, telemetría en vivo, subir lotes) | `iot:telemetry:ingest` + plan con `iotTelemetryEnabled` | US16–US19 |
| Ver fallas activas / borrarlas | `iot:faults:read`, `iot:faults:resolve` | US17, US20 |
| Instalar / retirar dispositivo OBD-II | `iot:installations:manage`, `iot:devices:read` | US16 |
| Informe de salud con IA | `iot:health_reports:generate`, `iot:health_reports:read` + plan con `aiDiagnosticsEnabled` | US21, US43 |
| Alertas predictivas | `iot:alerts:read`, `iot:alerts:acknowledge` | US21 |
| Emitir comprobante SUNAT | `invoicing:invoices:issue_sunat` | US37 |
| Registrar pago | `invoicing:payments:create` | US38 |
| Pase de salida (PDF) | `invoicing:exit_passes:issue` | US39 |
| Entregar vehículo | `operations:vehicle_handover:execute` | US39 |
| Flujo de caja (PDF) | `invoicing:cashflow:export_pdf` | US41 |
| Series SUNAT | `invoicing:fiscal_config:manage` | US42 |

> `billing:plans:write` protege la creación de planes comerciales: es un permiso de **administrador de la plataforma**, no lo recibe ningún rol de taller y la app móvil no lo usa.

## Roles de fábrica

| Código | Nombre | Abreviatura en la matriz | Propósito |
| --- | --- | --- | --- |
| `ROLE_WORKSHOP_OWNER` | Dueño de Taller | Dueño | Titular legal y comercial de la cuenta SaaS en Atelier Platform. |
| `ROLE_WORKSHOP_ADMINISTRATOR` | Administrador de Taller | Admin | Gerencia operativa diaria del taller mecánico. |
| `ROLE_CHIEF_MECHANIC` | Mecánico Jefe | Jefe | Liderazgo técnico, distribución de carga laboral y aseguramiento de la calidad en patio. |
| `ROLE_SERVICE_ADVISOR` | Asesor de Servicio | Asesor | Enlace técnico-comercial directo entre el cliente y el equipo operativo de taller. |
| `ROLE_RECEPTIONIST` | Recepcionista | Recep | Atención de counter de entrada, bienvenida al cliente y gestión de agenda de visitas. |
| `ROLE_MECHANIC` | Técnico Mecánico | Mecánico | Ejecución directa de labores mecánicas, eléctricas y de diagnóstico en bahía o foso. |
| `ROLE_INVENTORY_MANAGER` | Encargado de Inventario | Almacén | Custodia física, catalogación y valoración contable de repuestos, lubricantes e insumos. |
| `ROLE_CASHIER` | Cajero | Cajero | Recaudación económica, cobranza y liquidación tributaria de servicios. |

## Matriz rol × permiso

✔ = el rol de fábrica incluye el permiso al crear el taller. El dueño o administrador puede editar los roles de su taller, así que la app **nunca** debe decidir por nombre de rol: decide por el claim `permissions` del JWT.


### IAM & Tenancy

| Permiso | Descripción | Dueño | Admin | Jefe | Asesor | Recep | Mecánico | Almacén | Cajero |
| --- | --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `iam:tenants:read` | Consultar perfil y configuración del taller actual | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `iam:tenants:update` | Actualizar nombre y razón social del taller | ✔ |  |  |  |  |  |  |  |
| `iam:branches:read` | Consultar sedes de operación y geocercas | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `iam:branches:manage` | Crear sedes y configurar coordenadas GPS y radio de geocerca | ✔ | ✔ |  |  |  |  |  |  |
| `iam:members:read` | Consultar personal del taller y sus estados | ✔ | ✔ | ✔ |  |  |  |  |  |
| `iam:members:invite` | Emitir invitaciones laborales a nuevos colaboradores por correo | ✔ | ✔ |  |  |  |  |  |  |
| `iam:members:manage_roles` | Asignar o revocar roles de seguridad a miembros del taller | ✔ | ✔ |  |  |  |  |  |  |
| `iam:members:compensate` | Modificar esquemas de remuneración salarial (fijo o por hora) | ✔ |  |  |  |  |  |  |  |
| `iam:roles:read` | Consultar roles de fábrica y personalizados del taller | ✔ | ✔ |  |  |  |  |  |  |
| `iam:roles:create` | Crear roles personalizados combinando permisos del catálogo | ✔ | ✔ |  |  |  |  |  |  |
| `iam:permissions:read` | Consultar el catálogo transversal de permisos atómicos | ✔ | ✔ |  |  |  |  |  |  |
| `iam:members:deactivate` | Dar de baja la membresía de un colaborador del taller | ✔ | ✔ |  |  |  |  |  |  |
| `iam:roles:manage` | Editar permisos, restablecer a fábrica y eliminar roles del taller | ✔ | ✔ |  |  |  |  |  |  |

### CRM & Fleet

| Permiso | Descripción | Dueño | Admin | Jefe | Asesor | Recep | Mecánico | Almacén | Cajero |
| --- | --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `crm:customers:read` | Buscar y consultar clientes por documento o datos de contacto | ✔ | ✔ | ✔ | ✔ | ✔ |  |  | ✔ |
| `crm:customers:create` | Dar de alta a nuevos clientes propietarios de vehículos | ✔ | ✔ |  | ✔ | ✔ |  |  |  |
| `crm:customers:update` | Modificar datos personales o fiscales del cliente | ✔ | ✔ |  | ✔ | ✔ |  |  |  |
| `crm:vehicles:read` | Consultar vehículos por placa, VIN o historial de servicio | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `crm:vehicles:create` | Registrar nuevos vehículos y asociarlos a su propietario | ✔ | ✔ |  | ✔ | ✔ |  |  |  |
| `crm:vehicles:update` | Actualizar odómetro, kilometraje y especificaciones técnicas | ✔ | ✔ | ✔ | ✔ | ✔ |  |  |  |
| `crm:fleets:manage` | Administrar flotas vehiculares corporativas y contratos B2B | ✔ | ✔ |  | ✔ |  |  |  |  |
| `crm:appointments:read` | Consultar la agenda de citas del taller | ✔ | ✔ |  | ✔ | ✔ |  |  |  |
| `crm:appointments:create` | Agendar citas de clientes (incluye convertir alertas predictivas en citas) | ✔ | ✔ |  | ✔ | ✔ |  |  |  |
| `crm:appointments:update` | Confirmar, registrar llegada, reprogramar o cancelar citas | ✔ | ✔ |  | ✔ | ✔ |  |  |  |

### Workshop Operations (MRO)

| Permiso | Descripción | Dueño | Admin | Jefe | Asesor | Recep | Mecánico | Almacén | Cajero |
| --- | --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `operations:work_orders:read` | Consultar órdenes de trabajo, estados y detalles | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `operations:work_orders:create` | Abrir nuevas órdenes de trabajo y configurar tareas iniciales | ✔ | ✔ | ✔ | ✔ |  |  |  |  |
| `operations:work_orders:cancel` | Cancelar órdenes de trabajo por desistimiento o inviabilidad | ✔ | ✔ |  |  |  |  |  |  |
| `operations:inspections:create` | Registrar el checklist pericial de recepción e inventario 360° | ✔ | ✔ | ✔ | ✔ |  |  |  |  |
| `operations:inspections:upload_photos` | Cargar fotografías de evidencias de daños con URLs firmadas S3 | ✔ | ✔ | ✔ | ✔ |  |  |  |  |
| `operations:quotations:read` | Consultar cotizaciones y proformas de mantenimiento | ✔ | ✔ | ✔ | ✔ |  |  | ✔ | ✔ |
| `operations:quotations:create` | Elaborar presupuestos con cómputo en cascada de mano de obra | ✔ | ✔ | ✔ | ✔ |  |  |  |  |
| `operations:quotations:send` | Generar y enviar proformas comerciales en PDF al cliente | ✔ | ✔ |  | ✔ |  |  |  |  |
| `operations:quotations:approve` | Registrar la resolución formal de aprobación del presupuesto | ✔ | ✔ |  | ✔ |  |  |  |  |
| `operations:proposals:create` | Proponer tareas adicionales por averías detectadas en foso | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |
| `operations:proposals:notify` | Remitir propuesta adicional al cliente para autorización técnica | ✔ | ✔ |  | ✔ |  |  |  |  |
| `operations:tasks:read` | Consultar tareas mecánicas asignadas a bahías | ✔ | ✔ | ✔ | ✔ |  | ✔ | ✔ |  |
| `operations:tasks:track_time` | Iniciar, pausar y reanudar cronómetros de labor efectiva |  |  | ✔ |  |  | ✔ |  |  |
| `operations:tasks:upload_photos` | Adjuntar fotografías periciales de desmontaje y montaje |  |  | ✔ |  |  | ✔ |  |  |
| `operations:tasks:hold_request` | Solicitar suspensión de tarea por repuesto defectuoso o faltante |  |  |  |  |  | ✔ |  |  |
| `operations:tasks:hold_validate` | Aprobar o resolver administrativamente la suspensión de tarea | ✔ | ✔ | ✔ |  |  |  |  |  |
| `operations:tasks:complete` | Registrar el cierre técnico de la labor con cómputo de horas |  |  | ✔ |  |  | ✔ |  |  |
| `operations:tasks:charge_parts` | Imputar repuestos del almacén a una tarea propia en curso |  |  | ✔ |  |  | ✔ |  |  |
| `operations:bays:read` | Consultar disponibilidad y ocupación de bahías en tiempo real | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |
| `operations:bays:reassign` | Reasignar mecánicos y vehículos entre bahías operativas | ✔ | ✔ | ✔ |  |  |  |  |  |
| `operations:vehicle_handover:execute` | Ejecutar entrega pericial del automóvil y validar pase de salida | ✔ | ✔ |  | ✔ |  |  |  |  |
| `operations:work_orders:update` | Actualizar órdenes y sus tareas, iniciar y concluir la orden, gestionar repuestos de tareas | ✔ | ✔ | ✔ | ✔ |  |  |  |  |
| `operations:proposals:resolve` | Registrar la aprobación o rechazo del cliente sobre una propuesta adicional | ✔ | ✔ |  | ✔ |  |  |  |  |
| `operations:services:read` | Consultar el catálogo de servicios del taller | ✔ | ✔ | ✔ | ✔ | ✔ |  |  |  |
| `operations:services:manage` | Crear y actualizar servicios del catálogo del taller | ✔ | ✔ |  |  |  |  |  |  |
| `operations:bays:manage` | Crear bahías y cambiar su estado de mantenimiento | ✔ | ✔ |  |  |  |  |  |  |

### Inventory & Supply Chain

| Permiso | Descripción | Dueño | Admin | Jefe | Asesor | Recep | Mecánico | Almacén | Cajero |
| --- | --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `inventory:parts:read` | Consultar catálogo de repuestos, precios y niveles de stock | ✔ | ✔ | ✔ | ✔ |  | ✔ | ✔ |  |
| `inventory:parts:manage` | Crear y actualizar especificaciones de piezas y compatibilidades | ✔ | ✔ |  |  |  |  | ✔ |  |
| `inventory:suppliers:manage` | Administrar catálogo de proveedores comerciales | ✔ | ✔ |  |  |  |  | ✔ |  |
| `inventory:purchase_orders:read` | Consultar órdenes de compra emitidas a proveedores | ✔ | ✔ | ✔ |  |  |  | ✔ |  |
| `inventory:purchase_orders:create` | Crear y formalizar órdenes de compra de abastecimiento | ✔ | ✔ |  |  |  |  | ✔ |  |
| `inventory:batches:receive` | Registrar ingreso físico de lotes de repuestos con costeo unitario | ✔ | ✔ |  |  |  |  | ✔ |  |
| `inventory:batches:dispatch_fifo` | Ejecutar descargo contable automatizado bajo método FIFO | ✔ | ✔ |  |  |  |  | ✔ |  |
| `inventory:batches:restore` | Reincorporar repuestos removidos de tareas canceladas | ✔ | ✔ |  |  |  |  | ✔ |  |
| `inventory:alerts:read` | Consultar alertas de reposición por stock mínimo | ✔ | ✔ | ✔ |  |  |  | ✔ |  |
| `inventory:alerts:resolve` | Marcar alertas resueltas tras emitir orden de reposición | ✔ | ✔ |  |  |  |  | ✔ |  |
| `inventory:batches:read` | Consultar lotes de repuestos, costos unitarios y valorización del inventario | ✔ | ✔ |  |  |  |  | ✔ |  |

### Human Resources & Shifts

| Permiso | Descripción | Dueño | Admin | Jefe | Asesor | Recep | Mecánico | Almacén | Cajero |
| --- | --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `hr:attendance:clock_in` | Registrar marcación de ingreso laboral con validación de geocerca GPS |  |  | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `hr:attendance:clock_out` | Registrar marcación de salida y liquidar jornada efectiva |  |  | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `hr:attendance:read_own` | Consultar historial propio de asistencias y tiempos de labor | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `hr:attendance:audit_all` | Auditar marcaciones, anomalías perimétricas y asistencias del taller | ✔ | ✔ | ✔ |  |  |  |  |  |
| `hr:shifts:read` | Consultar programación semanal de turnos y tolerancias | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `hr:shifts:manage` | Configurar turnos, horarios laborales y márgenes de gracia | ✔ | ✔ |  |  |  |  |  |  |
| `hr:justifications:create` | Enviar descargo justificatorio de tardanza o ausencia |  |  | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `hr:justifications:approve` | Aprobar o rechazar descargos de tardanza de colaboradores | ✔ | ✔ | ✔ |  |  |  |  |  |
| `hr:plame:export` | Exportar reportes de jornada laboral compatibles con SUNAT PLAME | ✔ | ✔ |  |  |  |  |  |  |
| `hr:employees:manage` | Registrar perfiles laborales y cambiar el estado de empleo del personal | ✔ | ✔ |  |  |  |  |  |  |
| `hr:payroll:manage` | Generar, ajustar, aprobar y desembolsar planillas de pago | ✔ | ✔ |  |  |  |  |  |  |

### Invoicing & Compliance

| Permiso | Descripción | Dueño | Admin | Jefe | Asesor | Recep | Mecánico | Almacén | Cajero |
| --- | --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `invoicing:invoices:read` | Consultar comprobantes emitidos, notas de crédito y estados CDR | ✔ | ✔ |  | ✔ |  |  |  | ✔ |
| `invoicing:invoices:settle` | Preliquidar económicamente órdenes de trabajo finalizadas | ✔ | ✔ |  |  |  |  |  | ✔ |
| `invoicing:invoices:issue_sunat` | Emitir comprobantes tributarios electrónicos UBL 2.1 ante SUNAT vía PSE | ✔ | ✔ |  |  |  |  |  | ✔ |
| `invoicing:payments:create` | Registrar cobros y amortizaciones multimoneda y multimedio | ✔ | ✔ |  |  |  |  |  | ✔ |
| `invoicing:exit_passes:issue` | Emitir pase digital de salida del vehículo tras orden pagada | ✔ | ✔ |  |  |  |  |  | ✔ |
| `invoicing:cashflow:export_pdf` | Generar y exportar reporte contable de flujo de caja en PDF | ✔ |  |  |  |  |  |  |  |
| `invoicing:fiscal_config:manage` | Configurar certificados digitales, enlace PSE y series SUNAT | ✔ |  |  |  |  |  |  |  |

### SaaS Billing & Subscriptions

| Permiso | Descripción | Dueño | Admin | Jefe | Asesor | Recep | Mecánico | Almacén | Cajero |
| --- | --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `billing:subscriptions:read` | Consultar plan SaaS contratado, cuotas de uso y fecha de corte | ✔ | ✔ |  |  |  |  |  |  |
| `billing:subscriptions:manage_stripe` | Gestionar suscripción B2B, cambio de plan y portal de facturación Stripe | ✔ |  |  |  |  |  |  |  |
| `billing:plans:read` | Consultar catálogo público de planes comerciales y tarifas | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ | ✔ |
| `billing:invoices:read` | Consultar facturas por el uso del servicio SaaS de Atelier | ✔ |  |  |  |  |  |  |  |

### IoT Telemetry & Predictive Maintenance

| Permiso | Descripción | Dueño | Admin | Jefe | Asesor | Recep | Mecánico | Almacén | Cajero |
| --- | --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `iot:devices:read` | Consultar catálogo de escáneres OBD-II y estados de hardware | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |
| `iot:devices:register` | Registrar nuevos escáneres OBD-II por dirección MAC y serial | ✔ | ✔ | ✔ |  |  |  |  |  |
| `iot:installations:manage` | Vincular o desvincular escáneres a vehículos en bahía | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |
| `iot:telemetry:ingest` | Ingestar lotes de métricas telemétricas hacia TimescaleDB |  |  |  |  |  | ✔ |  |  |
| `iot:telemetry:read` | Consultar lecturas telemétricas en vivo para tacómetros y análisis | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |
| `iot:faults:read` | Consultar fallas electrónicas activas y códigos DTC leídos | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |
| `iot:faults:resolve` | Verificar borrado y subsanación de códigos de falla en motor | ✔ | ✔ | ✔ |  |  | ✔ |  |  |
| `iot:alerts:read` | Consultar alertas analíticas generadas por anomalías térmicas | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |
| `iot:alerts:acknowledge` | Confirmar recepción y lectura de alertas predictivas | ✔ | ✔ | ✔ |  |  | ✔ |  |  |
| `iot:health_reports:generate` | Solicitar inferencia pericial con Spring AI Groq Cloud LPU | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |
| `iot:health_reports:read` | Descargar reporte forense de salud automotriz en PDF | ✔ | ✔ | ✔ | ✔ |  | ✔ |  |  |

## Endpoints protegidos por cada permiso


### IAM & Tenancy

- `iam:tenants:read`: `GET /api/v1/tenants/current`
- `iam:tenants:update`: `PUT /api/v1/tenants/current`
- `iam:branches:read`: `GET /api/v1/branches/{id}`, `GET /api/v1/branches`
- `iam:branches:manage`: `POST /api/v1/branches`, `PUT /api/v1/branches/{id}`
- `iam:members:read`: `GET /api/v1/hr/employees/branch/{branchId}`, `GET /api/v1/hr/employees/membership/{membershipId}`, `GET /api/v1/hr/employees/{profileId}`, `GET /api/v1/memberships/{id}`, `GET /api/v1/memberships`
- `iam:members:invite`: `POST /api/v1/invitations/tenant/{tenantId}`
- `iam:members:manage_roles`: `PUT /api/v1/memberships/{id}/roles`
- `iam:members:compensate`: `PUT /api/v1/hr/employees/{profileId}/salary`, `PUT /api/v1/memberships/{id}/compensation`
- `iam:roles:read`: `GET /api/v1/tenants/{tenantId}/roles`
- `iam:roles:create`: `POST /api/v1/tenants/{tenantId}/roles`
- `iam:permissions:read`: `GET /api/v1/permissions`
- `iam:members:deactivate`: `DELETE /api/v1/memberships/{id}`
- `iam:roles:manage`: `DELETE /api/v1/tenants/{tenantId}/roles/{roleId}`, `POST /api/v1/tenants/{tenantId}/roles/{roleId}/reset-defaults`, `PUT /api/v1/tenants/{tenantId}/roles/{roleId}/permissions`

### CRM & Fleet

- `crm:customers:read`: `GET /api/v1/customers/affiliation-lookup`, `GET /api/v1/customers/{customerId}/memberships`, `GET /api/v1/customers/{id}/vehicles`, `GET /api/v1/customers/{id}`, `GET /api/v1/customers`
- `crm:customers:create`: `POST /api/v1/customers/affiliations`, `POST /api/v1/customers/companies`, `POST /api/v1/customers/individuals`
- `crm:customers:update`: `PUT /api/v1/customers/{id}/contact`
- `crm:vehicles:read`: `GET /api/v1/vehicles/by-plate/{plate}`, `GET /api/v1/vehicles/{id}/ownerships`, `GET /api/v1/vehicles/{id}`
- `crm:vehicles:create`: `POST /api/v1/vehicles`
- `crm:vehicles:update`: `POST /api/v1/vehicles/{id}/ownerships`
- `crm:fleets:manage`: `DELETE /api/v1/customers/{customerId}/memberships/{userId}`, `POST /api/v1/customers/{customerId}/memberships`
- `crm:appointments:read`: `GET /api/v1/appointments/{id}`, `GET /api/v1/appointments`
- `crm:appointments:create`: `POST /api/v1/appointments`, `POST /api/v1/iot/alerts/{id}/convert-to-appointment`
- `crm:appointments:update`: `POST /api/v1/appointments/{id}/arrive`, `POST /api/v1/appointments/{id}/cancel`, `POST /api/v1/appointments/{id}/confirm`, `POST /api/v1/appointments/{id}/reschedule`

### Workshop Operations (MRO)

- `operations:work_orders:read`: `GET /api/v1/work-orders/{workOrderId}/proposals`, `GET /api/v1/work-orders/{workOrderId}`, `GET /api/v1/work-orders`
- `operations:work_orders:create`: `POST /api/v1/work-orders/{workOrderId}/tasks`, `POST /api/v1/work-orders`
- `operations:work_orders:cancel`: `POST /api/v1/work-orders/{workOrderId}/cancel`
- `operations:inspections:create`: *(sin endpoint REST; uso interno o reservado)*
- `operations:inspections:upload_photos`: `POST /api/v1/work-orders/{workOrderId}/intake-images`
- `operations:quotations:read`: `GET /api/v1/work-orders/{workOrderId}/quotation/pdf`
- `operations:quotations:create`: *(sin endpoint REST; uso interno o reservado)*
- `operations:quotations:send`: `POST /api/v1/work-orders/{workOrderId}/quotation/send`
- `operations:quotations:approve`: `POST /api/v1/work-orders/{workOrderId}/quotation/approve`, `POST /api/v1/work-orders/{workOrderId}/quotation/reject`
- `operations:proposals:create`: `POST /api/v1/work-orders/{workOrderId}/proposals`
- `operations:proposals:notify`: `POST /api/v1/work-orders/{workOrderId}/proposals/{proposalId}/notify`
- `operations:tasks:read`: `GET /api/v1/tasks/my`, `GET /api/v1/tasks/{taskId}`
- `operations:tasks:track_time`: `POST /api/v1/tasks/{taskId}/pause`, `POST /api/v1/tasks/{taskId}/start`
- `operations:tasks:upload_photos`: `POST /api/v1/tasks/{taskId}/evidence-images`
- `operations:tasks:hold_request`: `POST /api/v1/tasks/{taskId}/hold`
- `operations:tasks:hold_validate`: `POST /api/v1/tasks/{taskId}/hold/approve`, `POST /api/v1/tasks/{taskId}/hold/reject`, `POST /api/v1/tasks/{taskId}/resume`
- `operations:tasks:complete`: `POST /api/v1/tasks/{taskId}/complete`
- `operations:tasks:charge_parts`: `POST /api/v1/tasks/{taskId}/products`
- `operations:bays:read`: `GET /api/v1/work-bays/{bayId}`, `GET /api/v1/work-bays`
- `operations:bays:reassign`: `DELETE /api/v1/work-orders/{workOrderId}/bay`, `PUT /api/v1/tasks/{taskId}/mechanic`, `PUT /api/v1/work-orders/{workOrderId}/bay`
- `operations:vehicle_handover:execute`: `POST /api/v1/work-orders/{workOrderId}/deliver`
- `operations:work_orders:update`: `DELETE /api/v1/tasks/{taskId}/products/{productId}`, `POST /api/v1/tasks/{taskId}/products`, `POST /api/v1/tasks/{taskId}/reopen`, `POST /api/v1/work-orders/{workOrderId}/complete`, `POST /api/v1/work-orders/{workOrderId}/start`, `PUT /api/v1/tasks/{taskId}/products/{productId}`, `PUT /api/v1/tasks/{taskId}`, `PUT /api/v1/work-orders/{workOrderId}`
- `operations:proposals:resolve`: `POST /api/v1/work-orders/{workOrderId}/proposals/{proposalId}/approve`, `POST /api/v1/work-orders/{workOrderId}/proposals/{proposalId}/reject`
- `operations:services:read`: `GET /api/v1/services/{serviceId}`, `GET /api/v1/services`
- `operations:services:manage`: `POST /api/v1/services`, `PUT /api/v1/services/{serviceId}`
- `operations:bays:manage`: `POST /api/v1/work-bays`, `PUT /api/v1/work-bays/{bayId}/maintenance`, `PUT /api/v1/work-bays/{bayId}/restore`

### Inventory & Supply Chain

- `inventory:parts:read`: `GET /api/v1/inventory/items/by-category/{category}`, `GET /api/v1/inventory/items/by-sku/{sku}`, `GET /api/v1/inventory/items/low-stock`, `GET /api/v1/inventory/items/{id}`, `GET /api/v1/inventory/items`
- `inventory:parts:manage`: `POST /api/v1/inventory/items`, `PUT /api/v1/inventory/items/{id}`
- `inventory:suppliers:manage`: `GET /api/v1/inventory/suppliers/{id}`, `GET /api/v1/inventory/suppliers`, `POST /api/v1/inventory/suppliers`, `PUT /api/v1/inventory/suppliers/{id}`
- `inventory:purchase_orders:read`: `GET /api/v1/inventory/purchase-orders/{id}`, `GET /api/v1/inventory/purchase-orders`
- `inventory:purchase_orders:create`: `POST /api/v1/inventory/purchase-orders/{id}/items`, `POST /api/v1/inventory/purchase-orders`, `PUT /api/v1/inventory/purchase-orders/{id}/cancel`, `PUT /api/v1/inventory/purchase-orders/{id}/issue`
- `inventory:batches:receive`: `POST /api/v1/inventory/batches/receive`, `PUT /api/v1/inventory/purchase-orders/{id}/receive`
- `inventory:batches:dispatch_fifo`: `POST /api/v1/inventory/batches/dispatch`
- `inventory:batches:restore`: `POST /api/v1/inventory/batches/restore`, `GET /api/v1/work-orders/{workOrderId}/part-movements`
- `inventory:alerts:read`: `GET /api/v1/inventory/alerts`
- `inventory:alerts:resolve`: `POST /api/v1/inventory/alerts/evaluate`, `POST /api/v1/inventory/alerts/{id}/resolve`
- `inventory:batches:read`: `GET /api/v1/inventory/batches/by-part/{partId}`, `GET /api/v1/inventory/items/valuation`, `GET /api/v1/inventory/items/{id}/batches`, `GET /api/v1/work-orders/{workOrderId}/part-movements`, `GET /api/v1/work-orders/{workOrderId}/profitability`

### Human Resources & Shifts

- `hr:attendance:clock_in`: `POST /api/v1/hr/attendances/clock-in`
- `hr:attendance:clock_out`: `POST /api/v1/hr/attendances/clock-out`
- `hr:attendance:read_own`: `GET /api/v1/hr/attendances/employee/{membershipId}/history`, `GET /api/v1/hr/attendances/employee/{membershipId}/status-today`, `GET /api/v1/hr/employees/membership/{membershipId}`
- `hr:attendance:audit_all`: `GET /api/v1/hr/attendances/branch/{branchId}/daily`, `GET /api/v1/hr/attendances/employee/{membershipId}/history`, `GET /api/v1/hr/attendances/employee/{membershipId}/status-today`
- `hr:shifts:read`: `GET /api/v1/hr/work-shifts/{shiftId}`, `GET /api/v1/hr/work-shifts`
- `hr:shifts:manage`: `PATCH /api/v1/hr/work-shifts/{shiftId}/activate`, `PATCH /api/v1/hr/work-shifts/{shiftId}/deactivate`, `POST /api/v1/hr/work-shifts`, `PUT /api/v1/hr/employees/{profileId}/shift`, `PUT /api/v1/hr/work-shifts/{shiftId}`
- `hr:justifications:create`: `POST /api/v1/hr/attendances/{attendanceId}/justification`
- `hr:justifications:approve`: `POST /api/v1/hr/attendances/{attendanceId}/justification/approve`, `POST /api/v1/hr/attendances/{attendanceId}/justification/reject`, `POST /api/v1/hr/attendances/{attendanceId}/justify`
- `hr:plame:export`: `GET /api/v1/hr/payrolls/export/sunat-rem`
- `hr:employees:manage`: `PATCH /api/v1/hr/employees/{profileId}/status`, `POST /api/v1/hr/employees`
- `hr:payroll:manage`: `GET /api/v1/hr/payrolls/{payrollId}`, `GET /api/v1/hr/payrolls`, `POST /api/v1/hr/payrolls/generate`, `POST /api/v1/hr/payrolls/{payrollId}/approve`, `POST /api/v1/hr/payrolls/{payrollId}/bonuses`, `POST /api/v1/hr/payrolls/{payrollId}/deductions`, `POST /api/v1/hr/payrolls/{payrollId}/disburse`

### Invoicing & Compliance

- `invoicing:invoices:read`: `GET /api/v1/invoicing/payments/branch/{branchId}/daily`, `GET /api/v1/invoicing/payments/voucher/{voucherId}`, `GET /api/v1/invoicing/vouchers/work-order/{workOrderId}`, `GET /api/v1/invoicing/vouchers/{id}`, `GET /api/v1/invoicing/vouchers`
- `invoicing:invoices:settle`: *(sin endpoint REST; uso interno o reservado)*
- `invoicing:invoices:issue_sunat`: `GET /api/v1/invoicing/series-configurations/branch/{branchId}`, `POST /api/v1/invoicing/vouchers/credit-notes`, `POST /api/v1/invoicing/vouchers/{id}/void`, `POST /api/v1/invoicing/vouchers`
- `invoicing:payments:create`: `POST /api/v1/invoicing/payments`, `POST /api/v1/work-orders/{workOrderId}/mark-as-paid`
- `invoicing:exit_passes:issue`: `GET /api/v1/work-orders/{workOrderId}/exit-pass/pdf`
- `invoicing:cashflow:export_pdf`: `GET /api/v1/invoicing/financial-reports/cash-flow/pdf`, `GET /api/v1/invoicing/financial-reports/cash-flow`, `GET /api/v1/work-orders/{workOrderId}/profitability`
- `invoicing:fiscal_config:manage`: `GET /api/v1/invoicing/series-configurations/branch/{branchId}`, `POST /api/v1/invoicing/series-configurations`, `PUT /api/v1/invoicing/series-configurations/{id}/status`

### SaaS Billing & Subscriptions

- `billing:subscriptions:read`: `GET /api/v1/billing/subscriptions/me`
- `billing:subscriptions:manage_stripe`: `POST /api/v1/billing/subscriptions/cancel`, `POST /api/v1/billing/subscriptions/checkout-session`, `POST /api/v1/billing/subscriptions/customer-portal`
- `billing:plans:read`: *(sin endpoint REST; uso interno o reservado)*
- `billing:invoices:read`: `GET /api/v1/billing/invoices/{id}/pdf`, `GET /api/v1/billing/invoices/{id}`, `GET /api/v1/billing/invoices`

### IoT Telemetry & Predictive Maintenance

- `iot:devices:read`: `GET /api/v1/iot/devices/{id}`, `GET /api/v1/iot/devices`, `GET /api/v1/iot/installations/vehicle/{vehicleId}/active`
- `iot:devices:register`: `PATCH /api/v1/iot/devices/{id}/status`, `POST /api/v1/iot/devices`
- `iot:installations:manage`: `POST /api/v1/iot/installations/install`, `POST /api/v1/iot/installations/{id}/uninstall`
- `iot:telemetry:ingest`: `POST /api/v1/iot/faults`, `POST /api/v1/iot/telemetry/batch`
- `iot:telemetry:read`: `GET /api/v1/iot/telemetry/vehicle/{vehicleId}/history`, `GET /api/v1/iot/telemetry/vehicle/{vehicleId}/latest`
- `iot:faults:read`: `GET /api/v1/iot/faults/vehicle/{vehicleId}/active`
- `iot:faults:resolve`: `POST /api/v1/iot/faults/{id}/resolve`
- `iot:alerts:read`: `GET /api/v1/iot/alerts/tenant`, `GET /api/v1/iot/alerts/vehicle/{vehicleId}`
- `iot:alerts:acknowledge`: `PATCH /api/v1/iot/alerts/{id}/acknowledge`
- `iot:health_reports:generate`: `POST /api/v1/iot/health-reports/generate-async`, `POST /api/v1/iot/health-reports/generate`
- `iot:health_reports:read`: `GET /api/v1/iot/health-reports/fleet/pdf`, `GET /api/v1/iot/health-reports/latest`, `GET /api/v1/iot/health-reports/{reportId}/pdf`, `GET /api/v1/iot/health-reports/{reportId}`

