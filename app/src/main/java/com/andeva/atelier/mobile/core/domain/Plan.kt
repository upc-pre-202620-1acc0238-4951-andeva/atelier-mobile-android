package com.andeva.atelier.mobile.core.domain

data class Plan(
    val tier: String,                                 // GO | PRO | MAX | ENTERPRISE | NONE
    val status: String,
    val accessGranted: Boolean,
    val iotTelemetryEnabled: Boolean,
    val aiDiagnosticsEnabled: Boolean
)
