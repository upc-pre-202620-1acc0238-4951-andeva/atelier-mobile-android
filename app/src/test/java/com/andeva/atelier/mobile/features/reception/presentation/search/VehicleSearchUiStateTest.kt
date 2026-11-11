package com.andeva.atelier.mobile.features.reception.presentation.search

import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cartera de clientes y padrón de vehículos de Recepción (filtros locales sobre Room). */
class VehicleSearchUiStateTest {

    private val ana = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres", "45678912", "ana@mail.pe", "999111222", "ACTIVE")
    private val acme = Customer("c2", CustomerType.COMPANY, "Transportes ACME SAC", "20123456789", null, null, "ACTIVE")
    private val yaris = Vehicle("v1", "ABC123", null, "Toyota", "Yaris", 2019, EngineType.GASOLINE, "c1", 42_000)
    private val hilux = Vehicle("v2", "XYZ789", null, "Toyota", "Hilux", 2021, EngineType.DIESEL, "c2", 80_000)

    private val base = VehicleSearchUiState(
        portfolioCustomers = listOf(ana, acme),
        workshopVehicles = listOf(VehicleWithOwner(yaris, ana), VehicleWithOwner(hilux, acme))
    )

    @Test
    fun `filtra clientes por nombre, documento o contacto`() {
        assertEquals(listOf(ana), base.copy(customerFilterQuery = "torres").filteredPortfolioCustomers)
        assertEquals(listOf(acme), base.copy(customerFilterQuery = "2012345").filteredPortfolioCustomers)
        assertEquals(listOf(ana), base.copy(customerFilterQuery = "999111").filteredPortfolioCustomers)
    }

    @Test
    fun `filtra clientes por tipo`() {
        assertEquals(listOf(acme), base.copy(customerFilterType = CustomerType.COMPANY).filteredPortfolioCustomers)
    }

    @Test
    fun `filtra vehiculos por placa con guion, modelo o propietario`() {
        assertEquals(listOf("v1"), base.copy(vehicleFilterQuery = "abc-123").filteredWorkshopVehicles.map { it.vehicle.id })
        assertEquals(listOf("v2"), base.copy(vehicleFilterQuery = "hilux").filteredWorkshopVehicles.map { it.vehicle.id })
        assertEquals(listOf("v2"), base.copy(vehicleFilterQuery = "acme").filteredWorkshopVehicles.map { it.vehicle.id })
    }

    @Test
    fun `el filtro del directorio sigue la pestana activa`() {
        val state = base.copy(customerFilterQuery = "ana", vehicleFilterQuery = "hilux")
        assertEquals("ana", state.copy(section = ReceptionSection.CUSTOMERS).directoryFilter)
        assertEquals("hilux", state.copy(section = ReceptionSection.VEHICLES).directoryFilter)
    }

    @Test
    fun `la placa buscada se precarga solo en modo placa`() {
        assertEquals("ABC123", base.copy(query = " ABC123 ").initialPlateCandidate)
        assertNull(base.copy(mode = SearchMode.DOCUMENT, query = "45678912").initialPlateCandidate)
        assertNull(base.copy(query = "  ").initialPlateCandidate)
    }
}
