package com.andeva.atelier.mobile.features.reception.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.reception.application.MarkAppointmentArrivedUseCase
import com.andeva.atelier.mobile.features.reception.domain.AffiliationStep
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.domain.ArrivalResult
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.NewVehicle
import com.andeva.atelier.mobile.features.reception.infrastructure.ReceptionTestEnv
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceptionRepositoryImplTest {

    private val env = ReceptionTestEnv()
    private lateinit var repository: ReceptionRepositoryImpl
    private val today = LocalDate.of(2026, 10, 24)

    @Before
    fun setUp() {
        env.start()
        repository = env.receptionRepository()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `buscar por placa normaliza la placa y trae al titular`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/vehicles/by-plate/ABC123"] = env.json(env.vehicle())
        env.responses["GET /api/v1/customers/c1"] = env.json(env.customer())

        val found = repository.findVehicleByPlate(" abc 123 ").getOrThrow()!!

        assertEquals("ABC123", found.vehicle.plate)
        assertEquals(EngineType.GASOLINE, found.vehicle.engineType)
        assertEquals("Ana Torres", found.owner!!.displayName)
        assertEquals("Ana Torres", repository.observeCustomer("c1").first()!!.displayName)
    }

    @Test
    fun `placa sin antecedentes es primer ingreso y sin titular no se pide el cliente`() = runTest {
        env.signIn()
        assertNull(repository.findVehicleByPlate("XYZ999").getOrThrow())

        env.responses["GET /api/v1/vehicles/by-plate/ABC123"] = env.json(env.vehicle(ownerId = null))
        val found = repository.findVehicleByPlate("ABC123").getOrThrow()!!

        assertNull(found.owner)
        assertTrue(env.requested("GET", "/api/v1/customers/null").isEmpty())
    }

    @Test
    fun `getCustomer lee en linea y guarda en la cache`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/customers/c1"] = env.json(env.customer())

        val customer = repository.getCustomer("c1").getOrThrow()

        assertEquals("Ana Torres", customer.displayName)
        assertEquals("45678912", customer.taxId)
        assertEquals("Ana Torres", repository.observeCustomer("c1").first()!!.displayName)
    }

    @Test
    fun `sin red la placa ya vista sale de la cache`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/vehicles/by-plate/ABC123"] = env.json(env.vehicle())
        env.responses["GET /api/v1/customers/c1"] = env.json(env.customer())
        repository.findVehicleByPlate("ABC123").getOrThrow()
        env.responses["GET /api/v1/vehicles/by-plate/ABC123"] = env.disconnect()
        env.responses["GET /api/v1/vehicles/by-plate/NEW111"] = env.disconnect()

        val cached = repository.findVehicleByPlate("ABC123").getOrThrow()!!

        assertEquals("v1", cached.vehicle.id)
        assertEquals("c1", cached.owner!!.id)
        assertTrue(repository.findVehicleByPlate("NEW111").exceptionOrNull() is IOException)
    }

    @Test
    fun `un vehiculo con la placa de otra fila reemplaza la vieja sin chocar`() = runTest {
        env.signIn()
        env.db.vehicleDao().upsert(VehicleEntity("old", "ABC123", null, "Kia", "Rio", null, null, null, null, 0))
        env.responses["GET /api/v1/vehicles/by-plate/ABC123"] = env.json(env.vehicle())

        repository.findVehicleByPlate("ABC123").getOrThrow()

        assertNull(env.db.vehicleDao().findById("old"))
        assertEquals("v1", env.db.vehicleDao().findByPlate("ABC123")!!.id)
    }

    @Test
    fun `buscar clientes sin red usa la cache por documento o nombre`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/customers"] = env.json(listOf(env.customer(), env.customer("c2", "Taller Pérez SAC", "20123456789")))
        assertEquals(2, repository.searchCustomers("a").getOrThrow().size)
        env.responses["GET /api/v1/customers"] = env.disconnect()

        assertEquals(listOf("c2"), repository.searchCustomers("2012").getOrThrow().map { it.id })
        assertEquals(listOf("c1"), repository.searchCustomers("torres").getOrThrow().map { it.id })
    }

    @Test
    fun `alta de vehiculo envia la placa normalizada y lo deja en cache`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/vehicles"] = env.json(env.vehicle(), 201)

        val vehicle = repository.registerVehicle(NewVehicle("abc 123", "Toyota", "Yaris", EngineType.GASOLINE, "c1", vin = " ")).getOrThrow()

        val body = env.requested("POST", "/api/v1/vehicles").single().body.readUtf8()
        assertTrue(body.contains("\"plate\":\"ABC123\""))
        assertTrue(body.contains("\"engineType\":\"GASOLINE\""))
        assertTrue(!body.contains("\"vin\""))
        assertEquals("v1", repository.observeVehicle(vehicle.id).first()!!.id)
    }

    @Test
    fun `afiliar guarda al cliente y los vehiculos que ya tenia en su app`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/customers/affiliation-lookup"] = env.json(
            mapOf("workshopCustomer" to null, "platformAccount" to mapOf("userId" to "p1", "email" to "ana@correo.pe"),
                "vehicles" to listOf(env.vehicle()), "nextStep" to "AFFILIATE")
        )
        env.responses["POST /api/v1/customers/affiliations"] = env.json(env.customer(), 201)
        env.responses["GET /api/v1/customers/c1/vehicles"] = env.json(listOf(env.vehicle()))

        val lookup = repository.lookupAffiliation("ana@correo.pe").getOrThrow()
        repository.affiliate("ana@correo.pe", "45678912", null).getOrThrow()

        assertEquals(AffiliationStep.AFFILIATE, lookup.nextStep)
        assertEquals(
            """{"email":"ana@correo.pe","taxId":"45678912"}""",
            env.requested("POST", "/api/v1/customers/affiliations").single().body.readUtf8()
        )
        assertEquals("ABC123", repository.observeVehicle("v1").first()!!.plate)
    }

    @Test
    fun `la agenda del dia trae placa y cliente y reemplaza solo ese dia`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/appointments"] = env.json(listOf(env.appointment()))
        env.responses["GET /api/v1/vehicles/v1"] = env.json(env.vehicle())
        env.responses["GET /api/v1/customers/c1"] = env.json(env.customer())

        repository.refreshAppointments(today).getOrThrow()
        env.responses["GET /api/v1/appointments"] = env.json(emptyList<Any>())
        repository.refreshAppointments(today.plusDays(1)).getOrThrow()

        val agenda = repository.observeAppointments(today).first().single()
        assertEquals("ABC123", agenda.plate)
        assertEquals("Ana Torres", agenda.customerName)
        assertEquals(AppointmentStatus.CONFIRMED, agenda.status)
        val request = env.requested("GET", "/api/v1/appointments").first().requestUrl!!
        assertEquals("b1", request.queryParameter("branchId"))
        assertEquals("2026-10-24", request.queryParameter("date"))
    }

    @Test
    fun `sin sede la agenda falla con MissingBranchException`() = runTest {
        env.signIn(branchId = null)

        assertTrue(repository.refreshAppointments(today).exceptionOrNull() is MissingBranchException)
        assertTrue(repository.observeAppointments(today).first().isEmpty())
    }

    @Test
    fun `llegada con la orden ya creada devuelve su id`() = runTest {
        env.signIn()
        val arrive = MarkAppointmentArrivedUseCase(repository, env.workOrderRepository())
        env.responses["POST /api/v1/appointments/a1/arrive"] = env.json(env.appointment(status = "ARRIVED"))
        env.responses["GET /api/v1/work-orders"] = env.json(listOf(env.workOrderSummary("wo0", "IN_PROGRESS"), env.workOrderSummary()))

        val result = arrive("a1").getOrThrow()

        assertEquals("wo1", (result as ArrivalResult.WorkOrderReady).workOrderId)
        assertEquals(AppointmentStatus.ARRIVED, result.appointment.status)
    }

    @Test
    fun `llegada sin orden todavia reintenta y queda pendiente`() = runTest {
        env.signIn()
        val arrive = MarkAppointmentArrivedUseCase(repository, env.workOrderRepository())
        env.responses["POST /api/v1/appointments/a1/arrive"] = env.json(env.appointment(status = "ARRIVED"))
        env.responses["GET /api/v1/work-orders"] = env.json(emptyList<Any>())

        val result = arrive("a1").getOrThrow()

        assertTrue(result is ArrivalResult.WorkOrderPending)
        assertEquals(MarkAppointmentArrivedUseCase.ATTEMPTS, env.requested("GET", "/api/v1/work-orders").size)
    }

    @Test
    fun `una llegada rechazada trae el message del backend`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/appointments/a1/arrive"] = env.error(422, "UNPROCESSABLE_ENTITY", "La cita está cancelada")

        val error = MarkAppointmentArrivedUseCase(repository, env.workOrderRepository())("a1").exceptionOrNull() as ApiException

        assertEquals("La cita está cancelada", error.message)
        assertTrue(env.requested("GET", "/api/v1/work-orders").isEmpty())
    }
}
