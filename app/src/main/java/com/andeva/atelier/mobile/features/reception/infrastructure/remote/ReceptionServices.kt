package com.andeva.atelier.mobile.features.reception.infrastructure.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Clientes y afiliación de docs/api/crm.md. */
interface CustomerService {
    @GET("api/v1/customers")
    suspend fun search(
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null
    ): Response<List<CustomerDto>>

    @GET("api/v1/customers/{id}")
    suspend fun getCustomer(@Path("id") id: String): Response<CustomerDto>

    /** Vehículos con titularidad vigente del cliente. */
    @GET("api/v1/customers/{id}/vehicles")
    suspend fun getVehicles(@Path("id") id: String): Response<List<VehicleDto>>

    @POST("api/v1/customers/individuals")
    suspend fun createIndividual(@Body body: CreateIndividualCustomerRequestDto): Response<CustomerDto>

    @POST("api/v1/customers/companies")
    suspend fun createCompany(@Body body: CreateCompanyCustomerRequestDto): Response<CustomerDto>

    @PUT("api/v1/customers/{id}/contact")
    suspend fun updateContact(@Path("id") id: String, @Body body: UpdateCustomerContactRequestDto): Response<CustomerDto>

    @GET("api/v1/customers/affiliation-lookup")
    suspend fun lookupAffiliation(@Query("email") email: String): Response<AffiliationLookupDto>

    @POST("api/v1/customers/affiliations")
    suspend fun affiliate(@Body body: AffiliateCustomerRequestDto): Response<CustomerDto>
}

/** Vehículos de docs/api/crm.md. `tasks` también lo usa para mostrar la placa (US26). */
interface VehicleService {
    @GET("api/v1/vehicles/{id}")
    suspend fun getVehicle(@Path("id") id: String): Response<VehicleDto>

    @GET("api/v1/vehicles/by-plate/{plate}")
    suspend fun getByPlate(@Path("plate") plate: String): Response<VehicleDto>

    @POST("api/v1/vehicles")
    suspend fun create(@Body body: CreateVehicleRequestDto): Response<VehicleDto>
}

/** Citas de docs/api/crm.md. */
interface AppointmentService {
    /** Agenda de una sede; `date` para un día puntual o `from`+`to` para un rango de días de Lima (`yyyy-MM-dd`). */
    @GET("api/v1/appointments")
    suspend fun getAppointments(
        @Query("branchId") branchId: String,
        @Query("date") date: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<AppointmentDto>>

    @POST("api/v1/appointments")
    suspend fun schedule(@Body body: ScheduleAppointmentRequestDto): Response<AppointmentDto>

    @POST("api/v1/appointments/{id}/confirm")
    suspend fun confirm(@Path("id") id: String): Response<AppointmentDto>

    @POST("api/v1/appointments/{id}/reschedule")
    suspend fun reschedule(@Path("id") id: String, @Body body: RescheduleAppointmentRequestDto): Response<AppointmentDto>

    @POST("api/v1/appointments/{id}/cancel")
    suspend fun cancel(@Path("id") id: String, @Body body: CancelAppointmentRequestDto): Response<AppointmentDto>

    /** Devuelve la cita; la orden en borrador se crea después, de forma asíncrona (H10). */
    @POST("api/v1/appointments/{id}/arrive")
    suspend fun arrive(@Path("id") id: String): Response<AppointmentDto>
}
