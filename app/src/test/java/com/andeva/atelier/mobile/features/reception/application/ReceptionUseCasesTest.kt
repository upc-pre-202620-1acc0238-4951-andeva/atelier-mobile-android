package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.NewCompanyCustomer
import com.andeva.atelier.mobile.features.reception.domain.NewIndividualCustomer
import com.andeva.atelier.mobile.features.reception.domain.NewVehicle
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceptionUseCasesTest {

    private val repository = mockk<ReceptionRepository>()

    private fun field(result: Result<*>) = (result.exceptionOrNull() as InvalidReceptionInputException).field

    @Test
    fun `datos invalidos no llegan al backend`() = runTest {
        assertEquals("plate", field(FindVehicleByPlateUseCase(repository)("   ")))
        assertEquals("plate", field(FindVehicleByPlateUseCase(repository)("A".repeat(16))))
        assertEquals("taxId", field(RegisterIndividualCustomerUseCase(repository)(NewIndividualCustomer("Ana", "Torres", "123"))))
        assertEquals("email", field(RegisterIndividualCustomerUseCase(repository)(NewIndividualCustomer("Ana", "Torres", "45678912", "ana"))))
        assertEquals("taxId", field(RegisterCompanyCustomerUseCase(repository)(NewCompanyCustomer("Taller SAC", "4567891"))))
        assertEquals("year", field(RegisterVehicleUseCase(repository)(NewVehicle("ABC123", "Toyota", "Yaris", EngineType.HYBRID, "c1", year = 1900))))
        assertEquals("email", field(LookupAffiliationUseCase(repository)("sin-arroba")))
        assertEquals("taxId", field(AffiliateCustomerUseCase(repository)("ana@correo.pe", "")))
        coVerify(exactly = 0) { repository.registerIndividual(any()) }
        coVerify(exactly = 0) { repository.affiliate(any(), any(), any()) }
    }

    @Test
    fun `la placa se normaliza en mayusculas y sin espacios`() {
        assertEquals("ABC-123", ReceptionValidation.normalizePlate(" abc -123 "))
    }
}
