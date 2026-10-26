package com.andeva.atelier.mobile.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {

    private fun session(permissions: Set<String>, plan: Plan?) = Session(
        userId = "u1", email = "m@taller.pe", fullName = "Mecánico", tenantId = "t1", tenantName = "Taller",
        membershipId = "m1", permissions = permissions, plan = plan
    )

    private fun plan(iot: Boolean, ai: Boolean) = Plan("PRO", "ACTIVE", true, iot, ai)

    @Test
    fun `decide por permiso y no por nombre de rol`() {
        val withRoleOnly = session(setOf("ROLE_MECHANIC"), null)
        assertFalse(withRoleOnly.has(Permission.TASKS_TRACK_TIME))

        val withPermission = session(setOf("operations:tasks:track_time"), null)
        assertTrue(withPermission.has(Permission.TASKS_TRACK_TIME))
    }

    @Test
    fun `telemetria requiere permiso y plan con IoT`() {
        val permissions = setOf(Permission.TELEMETRY_INGEST.value)
        assertTrue(session(permissions, plan(iot = true, ai = false)).canUseTelemetry())
        assertFalse(session(permissions, plan(iot = false, ai = true)).canUseTelemetry())
        assertFalse(session(emptySet(), plan(iot = true, ai = true)).canUseTelemetry())
    }

    @Test
    fun `sin plan conocido no se oculta el modulo`() {
        assertTrue(session(setOf(Permission.TELEMETRY_INGEST.value), null).canUseTelemetry())
        assertTrue(session(setOf(Permission.HEALTH_REPORTS_GENERATE.value), null).canUseAiDiagnostics())
    }

    @Test
    fun `aviso de suscripcion solo con accessGranted false`() {
        assertFalse(session(emptySet(), Plan("PRO", "PAST_DUE", false, true, true)).hasSubscriptionAccess())
        assertTrue(session(emptySet(), plan(iot = true, ai = true)).hasSubscriptionAccess())
        assertTrue(session(emptySet(), null).hasSubscriptionAccess())
    }

    @Test
    fun `informe IA requiere plan con IA`() {
        val permissions = setOf(Permission.HEALTH_REPORTS_GENERATE.value)
        assertFalse(session(permissions, plan(iot = true, ai = false)).canUseAiDiagnostics())
        assertTrue(session(permissions, plan(iot = false, ai = true)).canUseAiDiagnostics())
    }

    @Test
    fun `canReadAiReports exige permiso y plan con IA`() {
        val permissions = setOf(Permission.HEALTH_REPORTS_READ.value)
        assertTrue(session(permissions, plan(iot = false, ai = true)).canReadAiReports())
        assertFalse(session(permissions, plan(iot = true, ai = false)).canReadAiReports())
        assertFalse(session(emptySet(), plan(iot = true, ai = true)).canReadAiReports())
        assertFalse(session(setOf(Permission.HEALTH_REPORTS_GENERATE.value), plan(iot = true, ai = true)).canReadAiReports())
        assertTrue(session(permissions, null).canReadAiReports())
    }
}
