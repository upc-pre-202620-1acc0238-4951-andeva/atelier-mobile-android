package com.andeva.atelier.mobile.features.workorders.presentation.intake

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import com.andeva.atelier.mobile.features.workorders.application.AttachIntakePhotoUseCase
import com.andeva.atelier.mobile.features.workorders.application.GetWorkOrderDetailUseCase
import com.andeva.atelier.mobile.features.workorders.application.ObservePendingIntakePhotosUseCase
import com.andeva.atelier.mobile.features.workorders.domain.IntakeImage
import com.andeva.atelier.mobile.features.workorders.domain.PendingIntakePhoto
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.time.Instant

class IntakeGalleryViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<WorkOrderRepository>()
    private val pending = MutableStateFlow<List<PendingIntakePhoto>>(emptyList())
    private val photo = File("captura.jpg")

    private val allPermissions = listOf(Permission.WORK_ORDERS_READ, Permission.INSPECTIONS_UPLOAD_PHOTOS)

    private fun subida(id: String, description: String?) = IntakeImage(id, "https://x.test/$id.jpg", description, null)

    private fun enCola(id: String, description: String?, status: String = "PENDING") =
        PendingIntakePhoto(id, "/files/pending/$id.jpg", "https://x.test/$id.jpg", description, status, null, Instant.EPOCH)

    private fun detalle(vararg images: IntakeImage) = mockk<WorkOrderDetail> { every { intakeImages } returns images.toList() }

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "a@taller.pe", fullName = "Asesor", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(permissions: List<Permission> = allPermissions): IntakeGalleryViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        every { repository.observePendingIntakePhotos("wo1") } returns pending
        return IntakeGalleryViewModel(
            observeSession,
            AttachIntakePhotoUseCase(repository),
            ObservePendingIntakePhotosUseCase(repository),
            GetWorkOrderDetailUseCase(repository),
            SavedStateHandle(mapOf("workOrderId" to "wo1"))
        )
    }

    private fun IntakeGalleryViewModel.settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `los permisos de la sesion habilitan leer y subir fotos`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        val vm = viewModel()
        vm.settle()
        assertTrue(vm.uiState.value.canRead && vm.uiState.value.canUpload)

        val solo = viewModel(listOf(Permission.WORK_ORDERS_READ))
        solo.settle()
        assertFalse(solo.uiState.value.canUpload)
    }

    @Test
    fun `carga las fotos ya registradas de la orden`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle(subida("i1", "Frente"), subida("i2", "Trasera")))
        val vm = viewModel()
        vm.settle()

        assertEquals(2, vm.uiState.value.uploaded.size)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(PhotoStatus.UPLOADED, vm.uiState.value.photosFor(InspectionAngle.FRONT).single().status)
    }

    @Test
    fun `sin permiso de lectura no se pide el detalle`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(listOf(Permission.INSPECTIONS_UPLOAD_PHOTOS))
        vm.settle()

        coVerify(exactly = 0) { repository.getDetail(any()) }
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `sin red sigue con las fotos en cola y no rompe la pantalla`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.failure(IOException("sin red"))
        pending.value = listOf(enCola("p1", "Frente"))
        val vm = viewModel()
        vm.settle()

        assertEquals(1, vm.uiState.value.photos.size)
        assertEquals(PhotoStatus.QUEUED, vm.uiState.value.photos.single().status)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.error is UiText.Resource)
        pending.value = emptyList()
    }

    @Test
    fun `una foto que fallo se marca como fallida`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        pending.value = listOf(enCola("p1", "Tablero", status = "FAILED"))
        val vm = viewModel()
        vm.settle()

        assertEquals(PhotoStatus.FAILED, vm.uiState.value.photosFor(InspectionAngle.DASHBOARD).single().status)
        pending.value = emptyList()
    }

    @Test
    fun `tomar una foto la encola con el nombre del panel y limpia la nota`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        coEvery { repository.attachIntakePhoto("wo1", photo, "Frente") } returns Result.success(Unit)
        val vm = viewModel()
        vm.settle()
        vm.onPhotoCaptured(InspectionAngle.FRONT, photo)
        vm.settle()

        coVerify(exactly = 1) { repository.attachIntakePhoto("wo1", photo, "Frente") }
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `la nota del dano se agrega a la descripcion y se limpia al guardar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        coEvery { repository.attachIntakePhoto(any(), any(), any()) } returns Result.success(Unit)
        val vm = viewModel()
        vm.settle()
        vm.onNoteChange("  rayón en la puerta ")
        vm.onPhotoCaptured(InspectionAngle.LEFT, photo)
        vm.settle()

        coVerify { repository.attachIntakePhoto("wo1", photo, "Lateral izquierdo: rayón en la puerta") }
        assertEquals("", vm.uiState.value.note)
    }

    @Test
    fun `sin permiso de subir fotos no se encola nada`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        val vm = viewModel(listOf(Permission.WORK_ORDERS_READ))
        vm.settle()
        vm.onPhotoCaptured(InspectionAngle.FRONT, photo)
        vm.settle()

        coVerify(exactly = 0) { repository.attachIntakePhoto(any(), any(), any()) }
    }

    @Test
    fun `un error al guardar la foto muestra el mensaje y conserva la nota`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        coEvery { repository.attachIntakePhoto(any(), any(), any()) } returns Result.failure(ApiException(500, "ERR", "No se pudo guardar."))
        val vm = viewModel()
        vm.settle()
        vm.onNoteChange("golpe")
        vm.onPhotoCaptured(InspectionAngle.REAR, photo)
        vm.settle()

        assertEquals("No se pudo guardar.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertEquals("golpe", vm.uiState.value.note)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `una nota demasiado larga no llega al repositorio`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        val vm = viewModel()
        vm.settle()
        vm.onNoteChange("x".repeat(600))
        vm.onPhotoCaptured(InspectionAngle.FRONT, photo)
        vm.settle()

        coVerify(exactly = 0) { repository.attachIntakePhoto(any(), any(), any()) }
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `cuando una foto sale de la cola se vuelve a pedir el detalle`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        pending.value = listOf(enCola("p1", "Frente"))
        val vm = viewModel()
        vm.settle()
        coVerify(exactly = 1) { repository.getDetail("wo1") }

        coEvery { repository.getDetail("wo1") } returns Result.success(detalle(subida("i1", "Frente")))
        pending.value = emptyList()
        vm.settle()

        coVerify(exactly = 2) { repository.getDetail("wo1") }
        assertEquals(PhotoStatus.UPLOADED, vm.uiState.value.photos.single().status)
    }

    @Test
    fun `las fotos sin panel conocido van a otras fotos`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(
            detalle(subida("i1", "Frente: rayón"), subida("i2", "Otra foto: llanta"), subida("i3", "Foto del dueño"), subida("i4", null))
        )
        val vm = viewModel()
        vm.settle()

        assertEquals(listOf("i1"), vm.uiState.value.photosFor(InspectionAngle.FRONT).map { it.key })
        assertEquals(listOf("i2", "i3", "i4"), vm.uiState.value.photosFor(InspectionAngle.OTHER).map { it.key })
    }

    @Test
    fun `negar la camara avisa al usuario`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDetail("wo1") } returns Result.success(detalle())
        val vm = viewModel()
        vm.settle()
        vm.onCameraDenied()

        assertTrue(vm.uiState.value.error is UiText.Resource)
    }
}
