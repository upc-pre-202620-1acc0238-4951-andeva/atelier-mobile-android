package com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.DiagnosticsTestEnv
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HealthReportRepositoryImplTest {

    private val env = DiagnosticsTestEnv()

    @Before
    fun setUp() = runTest {
        env.start()
        env.signIn()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `generar envia el vehiculo y los dias a analizar`() = runTest {
        env.responses["POST /api/v1/iot/health-reports/generate"] = env.json(env.healthReport(), 201)

        val report = env.healthReports().generate("v1", 30, includeResolvedDtcHistory = true).getOrThrow()

        assertEquals(78, report.overallHealthScore)
        val request = env.requested("POST", "/api/v1/iot/health-reports/generate").single()
        assertEquals("v1", request.requestUrl!!.queryParameter("vehicleId"))
        assertEquals("""{"daysToAnalyze":30,"includeResolvedDtcHistory":true}""", request.body.readUtf8())
    }

    @Test
    fun `422 y 502 llegan con su codigo HTTP y el message`() = runTest {
        env.responses["POST /api/v1/iot/health-reports/generate"] =
            env.error(422, "ERR_INSUFFICIENT_TELEMETRY", "Telemetría insuficiente en el período")
        val insufficient = env.healthReports().generate("v1", 7, null).exceptionOrNull() as ApiException
        assertEquals(422, insufficient.httpStatus)

        env.responses["POST /api/v1/iot/health-reports/generate"] = env.error(502, "BAD_GATEWAY", "IA no disponible")
        assertEquals("IA no disponible", env.healthReports().generate("v1", 7, null).exceptionOrNull()?.message)
    }

    @Test
    fun `ultimo informe, detalle y PDF`() = runTest {
        assertNull(env.healthReports().getLatest("v1").getOrThrow())

        env.responses["GET /api/v1/iot/health-reports/latest"] = env.json(env.healthReport())
        assertEquals("r1", env.healthReports().getLatest("v1").getOrThrow()?.reportId)

        env.responses["GET /api/v1/iot/health-reports/r1"] = env.json(
            mapOf(
                "reportId" to "r1", "vehicleId" to "v1", "daysAnalyzed" to 30, "generatedAt" to "2026-10-24T14:00:00Z",
                "diagnosis" to mapOf(
                    "healthScore" to 78, "overallCondition" to "GOOD",
                    "recommendedActions" to listOf(mapOf("serviceName" to "Cambio de refrigerante", "estimatedCost" to 180.5))
                )
            )
        )
        val detail = env.healthReports().getDetail("r1").getOrThrow()
        assertEquals("Cambio de refrigerante", detail.diagnosis!!.recommendedActions.single().serviceName)
        assertTrue(detail.diagnosis!!.predictiveRisks.isEmpty())

        env.responses["GET /api/v1/iot/health-reports/r1/pdf"] = MockResponse().setResponseCode(200)
            .setHeader("Content-Disposition", "attachment; filename=\"informe-ABC123.pdf\"").setBody("%PDF-1.7")
        val pdf = env.healthReports().downloadPdf("r1").getOrThrow()
        assertEquals("informe-ABC123.pdf", pdf.name)
        assertEquals("%PDF-1.7", pdf.readText())
    }
}
