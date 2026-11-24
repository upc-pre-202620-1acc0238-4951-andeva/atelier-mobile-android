package com.andeva.atelier.mobile.features.diagnostics.presentation.report

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Plan
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.diagnostics.application.DownloadHealthReportPdfUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GenerateHealthReportUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetHealthReportDetailUseCase
import com.andeva.atelier.mobile.features.diagnostics.application.GetLatestHealthReportUseCase
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthDiagnosis
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReport
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportDetail
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportRepository
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.reception.application.ObserveVehicleUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant

class AiHealthReportViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<HealthReportRepository>()
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns MutableStateFlow(true) }

    private fun createVm(
        plan: Plan = Plan("MAX", "ACTIVE", true, iotTelemetryEnabled = true, aiDiagnosticsEnabled = true)
    ): AiHealthReportViewModel {
        val session = Session(
            "u1", "d@taller.pe", "Dueño", "t1", "Atelier", "m1",
            setOf(Permission.HEALTH_REPORTS_READ.value, Permission.HEALTH_REPORTS_GENERATE.value),
            plan
        )
        val observeSession = mockk<ObserveSessionUseCase> { every { this@mockk.invoke() } returns flowOf(session) }
        val observeVehicle = mockk<ObserveVehicleUseCase> { every { this@mockk.invoke("v1") } returns flowOf(null) }

        return AiHealthReportViewModel(
            observeSession = observeSession,
            observeVehicle = observeVehicle,
            connectivity = connectivity,
            getLatestHealthReport = GetLatestHealthReportUseCase(repository),
            getHealthReportDetail = GetHealthReportDetailUseCase(repository),
            generateHealthReport = GenerateHealthReportUseCase(repository),
            downloadHealthReportPdf = DownloadHealthReportPdfUseCase(repository),
            savedStateHandle = SavedStateHandle(mapOf("vehicleId" to "v1", "plate" to "BKL482"))
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `plan sin IA muestra estado bloqueado`() = runTest(mainRule.dispatcher) {
        val vm = createVm(Plan("PRO", "ACTIVE", true, iotTelemetryEnabled = true, aiDiagnosticsEnabled = false))
        settle()

        assertFalse(vm.uiState.value.planIncludesAi)
        assertFalse(vm.uiState.value.canGenerate)
    }

    @Test
    fun `generar informe maneja 422 luego genera y descarga PDF`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getLatest("v1") } returns Result.success(null)
        coEvery { repository.generate("v1", 30, true) } returnsMany listOf(
            Result.failure(ApiException(422, "ERR_INSUFFICIENT_TELEMETRY", "Faltan lecturas", emptyList())),
            Result.success(HealthReport("r1", "v1", 82, "Motor estable", 0, Instant.EPOCH))
        )
        coEvery { repository.getDetail("r1") } returns Result.success(
            HealthReportDetail(
                "r1", "v1", 30, Instant.EPOCH,
                HealthDiagnosis(82, "GOOD", "Motor estable", emptyList(), emptyList(), emptyList(), emptyList())
            )
        )
        val pdfFile = File("reporte-r1.pdf")
        coEvery { repository.downloadPdf("r1") } returns Result.success(pdfFile)

        val vm = createVm()
        settle()

        vm.openGenerateSheet()
        vm.confirmGenerate()
        settle()
        assertNotNull(vm.uiState.value.error)

        vm.confirmGenerate()
        settle()
        assertEquals(82, vm.uiState.value.effectiveScore)
        assertFalse(vm.uiState.value.generateSheetOpen)

        vm.downloadPdf()
        settle()
        assertEquals(pdfFile, vm.uiState.value.pendingPdfFile)
    }
}
