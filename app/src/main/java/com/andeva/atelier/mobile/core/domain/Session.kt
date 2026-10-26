package com.andeva.atelier.mobile.core.domain

data class Session(
    val userId: String,
    val email: String,
    val fullName: String,
    val tenantId: String,
    val tenantName: String,
    val membershipId: String?,
    val permissions: Set<String>,
    val plan: Plan?,                                  // null: sin plan conocido → no ocultar, el backend decide
    val branchId: String? = null,                     // de GET /hr/employees/membership/{membershipId}
    val shiftId: String? = null
) {
    fun has(permission: Permission) = permission.value in permissions

    /** Diagnóstico OBD-II: permiso del rol y módulo incluido en el plan. */
    fun canUseTelemetry() = has(Permission.TELEMETRY_INGEST) && (plan?.iotTelemetryEnabled ?: true)

    /** Informe de salud con IA: permiso del rol y módulo incluido en el plan. */
    fun canUseAiDiagnostics() = has(Permission.HEALTH_REPORTS_GENERATE) && (plan?.aiDiagnosticsEnabled ?: true)

    /** Informes de salud con IA (PDF de flota): permiso de lectura y módulo incluido en el plan. */
    fun canReadAiReports() = has(Permission.HEALTH_REPORTS_READ) && (plan?.aiDiagnosticsEnabled ?: true)

    /** false solo si el plan dice que la suscripción no está vigente; sin plan conocido no se avisa (api/README.md §2). */
    fun hasSubscriptionAccess() = plan?.accessGranted != false
}
