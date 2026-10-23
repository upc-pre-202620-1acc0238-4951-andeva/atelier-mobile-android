# IoT — Dispositivos OBD-II, telemetría, fallas, alertas e informes de salud

Dispositivos OBD-II, instalación en vehículos, telemetría, códigos DTC, alertas predictivas e informes de salud con IA.

Convenciones comunes (autenticación, errores, cabeceras, fechas, dinero): ver [README.md](README.md).

## Índice

| Método | Ruta | Permiso | Descripción |
| --- | --- | --- | --- |
| GET | [`/api/v1/iot/alerts/tenant`](#get-apiv1iotalertstenant) | `iot:alerts:read` | Get active predictive alerts dashboard for workshop tenant |
| GET | [`/api/v1/iot/alerts/vehicle/{vehicleId}`](#get-apiv1iotalertsvehiclevehicleid) | `iot:alerts:read` | Get historical predictive alerts for a vehicle |
| PATCH | [`/api/v1/iot/alerts/{id}/acknowledge`](#patch-apiv1iotalertsidacknowledge) | `iot:alerts:acknowledge` | Acknowledge and mark predictive alert as reviewed |
| POST | [`/api/v1/iot/alerts/{id}/convert-to-appointment`](#post-apiv1iotalertsidconvert-to-appointment) | `crm:appointments:create` | Convert predictive alert into preventative CRM workshop appointment |
| GET | [`/api/v1/iot/devices`](#get-apiv1iotdevices) | `iot:devices:read` | List workshop OBD-II devices with optional status filter |
| POST | [`/api/v1/iot/devices`](#post-apiv1iotdevices) | `iot:devices:register` | Register new OBD-II device in workshop inventory |
| GET | [`/api/v1/iot/devices/{id}`](#get-apiv1iotdevicesid) | `iot:devices:read` | Get OBD-II device by identifier |
| PATCH | [`/api/v1/iot/devices/{id}/status`](#patch-apiv1iotdevicesidstatus) | `iot:devices:register` | Update operational status of an OBD-II device |
| POST | [`/api/v1/iot/faults`](#post-apiv1iotfaults) | `iot:telemetry:ingest` | Register new electronic diagnostic trouble code (DTC) for vehicle |
| GET | [`/api/v1/iot/faults/vehicle/{vehicleId}/active`](#get-apiv1iotfaultsvehiclevehicleidactive) | `iot:faults:read` | Get all active unresolved faults for a vehicle |
| POST | [`/api/v1/iot/faults/{id}/resolve`](#post-apiv1iotfaultsidresolve) | `iot:faults:resolve` | Resolve and close an electronic vehicle fault |
| GET | [`/api/v1/iot/health-reports/fleet/pdf`](#get-apiv1iothealth-reportsfleetpdf) | `iot:health_reports:read` | Download the consolidated health report of a customer's fleet, critical units first |
| POST | [`/api/v1/iot/health-reports/generate`](#post-apiv1iothealth-reportsgenerate) | `iot:health_reports:generate` | Generate synchronous AI-assisted vehicle mechanical health diagnostic report |
| POST | [`/api/v1/iot/health-reports/generate-async`](#post-apiv1iothealth-reportsgenerate-async) | `iot:health_reports:generate` | Enqueue asynchronous vehicle health diagnostic report generation |
| GET | [`/api/v1/iot/health-reports/latest`](#get-apiv1iothealth-reportslatest) | `iot:health_reports:read` | Get latest stored health report for vehicle without re-running AI inference |
| GET | [`/api/v1/iot/health-reports/{reportId}`](#get-apiv1iothealth-reportsreportid) | `iot:health_reports:read` | Get the full structured diagnosis of an issued health report |
| GET | [`/api/v1/iot/health-reports/{reportId}/pdf`](#get-apiv1iothealth-reportsreportidpdf) | `iot:health_reports:read` | Download official institutional vehicle health diagnostic report in PDF format |
| POST | [`/api/v1/iot/installations/install`](#post-apiv1iotinstallationsinstall) | `iot:installations:manage` | Install OBD-II device on vehicle |
| GET | [`/api/v1/iot/installations/vehicle/{vehicleId}/active`](#get-apiv1iotinstallationsvehiclevehicleidactive) | `iot:devices:read` | Get active device installation for vehicle |
| POST | [`/api/v1/iot/installations/{id}/uninstall`](#post-apiv1iotinstallationsiduninstall) | `iot:installations:manage` | Uninstall OBD-II device from vehicle |
| POST | [`/api/v1/iot/telemetry/batch`](#post-apiv1iottelemetrybatch) | `iot:telemetry:ingest` | Ingest chronological batch of vehicle telemetry readings into TimescaleDB |
| GET | [`/api/v1/iot/telemetry/vehicle/{vehicleId}/history`](#get-apiv1iottelemetryvehiclevehicleidhistory) | `iot:telemetry:read` | Get aggregated historical telemetry using TimescaleDB time_bucket |
| GET | [`/api/v1/iot/telemetry/vehicle/{vehicleId}/latest`](#get-apiv1iottelemetryvehiclevehicleidlatest) | `iot:telemetry:read` | Get latest telemetry reading for vehicle tachometer and indicators |

## Endpoints

### GET /api/v1/iot/alerts/tenant

Get active predictive alerts dashboard for workshop tenant

- **Permiso:** `iot:alerts:read`
- **Respuesta exitosa:** 200 — `array<PredictiveAlertResponse>`

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### GET /api/v1/iot/alerts/vehicle/{vehicleId}

Get historical predictive alerts for a vehicle

- **Permiso:** `iot:alerts:read`
- **Respuesta exitosa:** 200 — `array<PredictiveAlertResponse>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `vehicleId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### PATCH /api/v1/iot/alerts/{id}/acknowledge

Acknowledge and mark predictive alert as reviewed

- **Permiso:** `iot:alerts:acknowledge`
- **Body** (application/json): `AcknowledgeAlertRequest` (opcional)
- **Respuesta exitosa:** 200 — `PredictiveAlertResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Predictive alert not found

### POST /api/v1/iot/alerts/{id}/convert-to-appointment

Convert predictive alert into preventative CRM workshop appointment

- **Permiso:** `crm:appointments:create`
- **Respuesta exitosa:** 200 — `AppointmentResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Predictive alert not found

### GET /api/v1/iot/devices

List workshop OBD-II devices with optional status filter

- **Permiso:** `iot:devices:read`
- **Respuesta exitosa:** 200 — `array<Obd2DeviceResponse>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `status` | query | string | no |

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### POST /api/v1/iot/devices

Register new OBD-II device in workshop inventory

- **Permiso:** `iot:devices:register`
- **Body** (application/json): `RegisterDeviceRequest`
- **Respuesta exitosa:** 201 — `Obd2DeviceResponse`

**Errores documentados:** **400** Validation failed; **401** Unauthorized; **403** Forbidden; **409** Device identifier already exists or quota exceeded

### GET /api/v1/iot/devices/{id}

Get OBD-II device by identifier

- **Permiso:** `iot:devices:read`
- **Respuesta exitosa:** 200 — `Obd2DeviceResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Device not found

### PATCH /api/v1/iot/devices/{id}/status

Update operational status of an OBD-II device

- **Permiso:** `iot:devices:register`
- **Body** (application/json): `UpdateDeviceStatusRequest`
- **Respuesta exitosa:** 200 — `Obd2DeviceResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **400** Validation failed; **401** Unauthorized; **403** Forbidden; **404** Device not found; **409** Device currently installed conflict

### POST /api/v1/iot/faults

Register new electronic diagnostic trouble code (DTC) for vehicle

- **Permiso:** `iot:telemetry:ingest`
- **Body** (application/json): `RegisterVehicleFaultRequest`
- **Respuesta exitosa:** 201 — `VehicleFaultResponse`

**Errores documentados:** **400** Invalid DTC code format or validation error; **401** Unauthorized; **403** Forbidden

### GET /api/v1/iot/faults/vehicle/{vehicleId}/active

Get all active unresolved faults for a vehicle

- **Permiso:** `iot:faults:read`
- **Respuesta exitosa:** 200 — `array<VehicleFaultResponse>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `vehicleId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden

### POST /api/v1/iot/faults/{id}/resolve

Resolve and close an electronic vehicle fault

- **Permiso:** `iot:faults:resolve`
- **Respuesta exitosa:** 200 — `VehicleFaultResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Vehicle fault not found

### GET /api/v1/iot/health-reports/fleet/pdf

Download the consolidated health report of a customer's fleet, critical units first

- **Permiso:** `iot:health_reports:read`
- **Respuesta exitosa:** 200 — archivo `application/pdf` (Content-Disposition con nombre de archivo)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `customerId` | query | string(uuid) | sí |

**Errores documentados:** **404** Customer not found in the workshop

### POST /api/v1/iot/health-reports/generate

Generate synchronous AI-assisted vehicle mechanical health diagnostic report

- **Permiso:** `iot:health_reports:generate`
- **Body** (application/json): `GenerateHealthReportRequest` (opcional)
- **Respuesta exitosa:** 201 — `HealthReportCreatedResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `vehicleId` | query | string(uuid) | sí |

**Errores documentados:** **400** Validation failed; **401** Unauthorized; **403** Quota exceeded or forbidden; **404** Vehicle not found; **502** AI inference service unavailable

### POST /api/v1/iot/health-reports/generate-async

Enqueue asynchronous vehicle health diagnostic report generation

- **Permiso:** `iot:health_reports:generate`
- **Body** (application/json): `GenerateHealthReportRequest` (opcional)
- **Respuesta exitosa:** 202 — `AsyncJobResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `vehicleId` | query | string(uuid) | sí |

**Errores documentados:** **400** Validation failed; **401** Unauthorized; **403** Quota exceeded or forbidden

### GET /api/v1/iot/health-reports/latest

Get latest stored health report for vehicle without re-running AI inference

- **Permiso:** `iot:health_reports:read`
- **Respuesta exitosa:** 200 — `HealthReportCreatedResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `vehicleId` | query | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** No previous health report found for vehicle

### GET /api/v1/iot/health-reports/{reportId}

Get the full structured diagnosis of an issued health report

- **Permiso:** `iot:health_reports:read`
- **Respuesta exitosa:** 200 — `HealthReportDetailResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `reportId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Report not found

### GET /api/v1/iot/health-reports/{reportId}/pdf

Download official institutional vehicle health diagnostic report in PDF format

- **Permiso:** `iot:health_reports:read`
- **Respuesta exitosa:** 200 — archivo `application/pdf` (Content-Disposition con nombre de archivo)

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `reportId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** Report not found

### POST /api/v1/iot/installations/install

Install OBD-II device on vehicle

- **Permiso:** `iot:installations:manage`
- **Body** (application/json): `InstallDeviceRequest`
- **Respuesta exitosa:** 201 — `DeviceInstallationResponse`

**Errores documentados:** **400** Validation failed; **401** Unauthorized; **403** Quota exceeded or forbidden; **404** Device or vehicle not found; **409** Active installation conflict

### GET /api/v1/iot/installations/vehicle/{vehicleId}/active

Get active device installation for vehicle

- **Permiso:** `iot:devices:read`
- **Respuesta exitosa:** 200 — `DeviceInstallationResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `vehicleId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** No active installation found

### POST /api/v1/iot/installations/{id}/uninstall

Uninstall OBD-II device from vehicle

- **Permiso:** `iot:installations:manage`
- **Body** (application/json): `UninstallDeviceRequest`
- **Respuesta exitosa:** 200 — `DeviceInstallationResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `id` | path | string(uuid) | sí |

**Errores documentados:** **400** Validation failed or final odometer invalid; **401** Unauthorized; **403** Forbidden; **404** Installation session not found

### POST /api/v1/iot/telemetry/batch

Ingest chronological batch of vehicle telemetry readings into TimescaleDB

- **Permiso:** `iot:telemetry:ingest`
- **Body** (application/json): `TelemetryBatchRequest`
- **Respuesta exitosa:** 202 — `TelemetryIngestionAckResponse`

**Errores documentados:** **400** Validation failed or empty batch; **401** Unauthorized; **403** Forbidden; **404** No active device installation found for vehicle; **500** TimescaleDB ingestion error

### GET /api/v1/iot/telemetry/vehicle/{vehicleId}/history

Get aggregated historical telemetry using TimescaleDB time_bucket

- **Permiso:** `iot:telemetry:read`
- **Respuesta exitosa:** 200 — `array<TelemetryHistoryBucketResponse>`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `vehicleId` | path | string(uuid) | sí |
| `from` | query | string(date-time) | no |
| `to` | query | string(date-time) | no |
| `bucket` | query | string | no |

**Errores documentados:** **400** Invalid bucket interval or date range; **401** Unauthorized; **403** Forbidden

### GET /api/v1/iot/telemetry/vehicle/{vehicleId}/latest

Get latest telemetry reading for vehicle tachometer and indicators

- **Permiso:** `iot:telemetry:read`
- **Respuesta exitosa:** 200 — `VehicleLatestTelemetryResponse`

| Parámetro | En | Tipo | Requerido |
| --- | --- | --- | --- |
| `vehicleId` | path | string(uuid) | sí |

**Errores documentados:** **401** Unauthorized; **403** Forbidden; **404** No telemetry readings found for vehicle

## Modelos

### PredictiveAlertResponse

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `vehicleId` | string(uuid) |
| `recommendedServiceId` | string(uuid) |
| `alertType` | string |
| `confidenceScore` | number |
| `message` | string |
| `status` | string |
| `createdAt` | string(date-time, UTC) |

### AcknowledgeAlertRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `acknowledgedBy` | string | no |  |

### AppointmentResponse

| Campo | Tipo |
| --- | --- |
| `appointmentId` | string(uuid) |
| `vehicleId` | string(uuid) |
| `customerId` | string(uuid) |
| `scheduledAt` | string(date-time, UTC) |
| `status` | string |
| `notes` | string |

### Obd2DeviceResponse

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `tenantId` | string(uuid) |
| `deviceIdentifier` | string |
| `connectionType` | string |
| `status` | string |
| `hardwareModel` | string |
| `firmwareVersion` | string |
| `createdAt` | string(date-time, UTC) |

### RegisterDeviceRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `connectionType` | string | sí | min 1 |
| `deviceIdentifier` | string | sí | min 1 |
| `firmwareVersion` | string | no |  |
| `hardwareModel` | string | no |  |

### UpdateDeviceStatusRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `status` | string | sí | min 1 |

### RegisterVehicleFaultRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `dtcCode` | string | sí | min 1, patrón `^[PBUC][0-9A-Fa-f]{4}$` |
| `severity` | string | sí | min 1, patrón `^(LOW\|MEDIUM\|CRITICAL)$` |
| `vehicleId` | string(uuid) | sí |  |
| `description` | string | no |  |

### VehicleFaultResponse

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `vehicleId` | string(uuid) |
| `dtcCode` | string |
| `severity` | string |
| `description` | string |
| `detectedAt` | string(date-time, UTC) |
| `isResolved` | boolean |
| `resolvedAt` | string(date-time, UTC) |

### GenerateHealthReportRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `daysToAnalyze` | integer(int32) | no | ≥ 7, ≤ 90 |
| `includeResolvedDtcHistory` | boolean | no |  |
| `triggerReason` | string | no |  |

### HealthReportCreatedResponse

| Campo | Tipo |
| --- | --- |
| `reportId` | string(uuid) |
| `vehicleId` | string(uuid) |
| `overallHealthScore` | integer |
| `executiveSummary` | string |
| `totalRisksDetected` | integer |
| `generatedAt` | string(date-time, UTC) |
| `jsonResourceUrl` | string |
| `pdfDownloadUrl` | string |

### AsyncJobResponse

| Campo | Tipo |
| --- | --- |
| `jobId` | string(uuid) |
| `status` | string |
| `vehicleId` | string(uuid) |
| `submittedAt` | string(date-time, UTC) |
| `estimatedCompletionTime` | string(date-time, UTC) |

### HealthReportDetailResponse

| Campo | Tipo |
| --- | --- |
| `reportId` | string(uuid) |
| `vehicleId` | string(uuid) |
| `daysAnalyzed` | integer |
| `generatedAt` | string(date-time, UTC) |
| `diagnosis` | VehicleHealthReportAiDto |
| `pdfDownloadUrl` | string |

### InstallDeviceRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `deviceId` | string(uuid) | sí |  |
| `vehicleId` | string(uuid) | sí |  |
| `currentOdometerKm` | integer(int32) | no | ≥ 0 |

### DeviceInstallationResponse

| Campo | Tipo |
| --- | --- |
| `id` | string(uuid) |
| `deviceId` | string(uuid) |
| `vehicleId` | string(uuid) |
| `tenantId` | string(uuid) |
| `installedAt` | string(date-time, UTC) |
| `uninstalledAt` | string(date-time, UTC) |
| `initialOdometerKm` | integer |
| `finalOdometerKm` | integer |
| `isActive` | boolean |

### UninstallDeviceRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `finalOdometerKm` | integer(int32) | no | ≥ 0 |
| `uninstalledAt` | string(date-time) | no |  |

### TelemetryBatchRequest (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `readings` | array<TelemetryReadingItemDto> | sí |  |
| `vehicleId` | string(uuid) | sí |  |

### TelemetryIngestionAckResponse

| Campo | Tipo |
| --- | --- |
| `vehicleId` | string(uuid) |
| `ingestedCount` | integer |
| `anomalyDetected` | boolean |
| `alertMessage` | string |

### TelemetryHistoryBucketResponse

| Campo | Tipo |
| --- | --- |
| `bucketTime` | string(date-time, UTC) |
| `vehicleId` | string(uuid) |
| `avgSpeedKmh` | integer |
| `avgEngineTempCelsius` | number |
| `avgEngineRpm` | integer |
| `avgFuelPercentage` | number |
| `avgBatteryVoltage` | number |
| `sampleCount` | integer |

### VehicleLatestTelemetryResponse

| Campo | Tipo |
| --- | --- |
| `vehicleId` | string(uuid) |
| `timestamp` | string(date-time, UTC) |
| `latitude` | number |
| `longitude` | number |
| `speedKmh` | integer |
| `engineTempCelsius` | number |
| `engineRpm` | integer |
| `batteryVoltage` | number |
| `fuelPercentage` | number |

### VehicleHealthReportAiDto

| Campo | Tipo |
| --- | --- |
| `healthScore` | integer |
| `overallCondition` | string |
| `executiveSummary` | string |
| `subsystemEvaluations` | array<SubsystemEvaluationDto> |
| `predictiveRisks` | array<PredictiveRiskDto> |
| `recommendedActions` | array<RecommendedServiceActionDto> |
| `dtcCorrelations` | array<DtcTelemetryCorrelationDto> |

### TelemetryReadingItemDto (request)

| Campo | Tipo | Requerido | Restricciones |
| --- | --- | --- | --- |
| `engineTempCelsius` | number(double) | sí |  |
| `timestamp` | string(date-time) | sí |  |
| `batteryVoltage` | number(double) | no |  |
| `engineRpm` | integer(int32) | no | ≥ 0 |
| `fuelPercentage` | number(double) | no |  |
| `latitude` | number(double) | no |  |
| `longitude` | number(double) | no |  |
| `speedKmh` | integer(int32) | no | ≥ 0 |

### SubsystemEvaluationDto

| Campo | Tipo |
| --- | --- |
| `subsystemName` | string |
| `status` | string |
| `score` | integer |
| `findings` | string |

### PredictiveRiskDto

| Campo | Tipo |
| --- | --- |
| `category` | string |
| `probability` | number |
| `timeHorizonDays` | integer |
| `consequences` | string |

### RecommendedServiceActionDto

| Campo | Tipo |
| --- | --- |
| `serviceCode` | string |
| `serviceName` | string |
| `urgency` | string |
| `estimatedCost` | number |
| `rationale` | string |

### DtcTelemetryCorrelationDto

| Campo | Tipo |
| --- | --- |
| `dtcCode` | string |
| `correlatedSensorReading` | string |
| `anomalyExplanation` | string |

