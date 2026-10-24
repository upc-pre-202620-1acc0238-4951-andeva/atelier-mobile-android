package com.andeva.atelier.mobile.features.reception.infrastructure.remote

import com.google.gson.annotations.SerializedName

/** `CustomerResource` de docs/api/crm.md. */
data class CustomerDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String?,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("companyName") val companyName: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("taxId") val taxId: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("status") val status: String?
)

/** `VehicleResource`. */
data class VehicleDto(
    @SerializedName("id") val id: String,
    @SerializedName("plate") val plate: String,
    @SerializedName("vin") val vin: String?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("model") val model: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("engineType") val engineType: String?,
    @SerializedName("currentOwnerId") val currentOwnerId: String?,
    @SerializedName("currentMileage") val currentMileage: Int?
)

/** `AppointmentResource`. `scheduledAt` en UTC. */
data class AppointmentDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String,
    @SerializedName("customerId") val customerId: String,
    @SerializedName("vehicleId") val vehicleId: String,
    @SerializedName("scheduledAt") val scheduledAt: String,
    @SerializedName("estimatedDurationMinutes") val estimatedDurationMinutes: Int?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String?
)

/** Respuesta de `GET /customers/affiliation-lookup`. */
data class AffiliationLookupDto(
    @SerializedName("workshopCustomer") val workshopCustomer: CustomerDto?,
    @SerializedName("platformAccount") val platformAccount: PlatformAccountDto?,
    @SerializedName("vehicles") val vehicles: List<VehicleDto>?,
    @SerializedName("nextStep") val nextStep: String?
)

data class PlatformAccountDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("status") val status: String?
)

/** `CreateIndividualCustomerResource`. */
data class CreateIndividualCustomerRequestDto(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("taxId") val taxId: String,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?
)

/** `CreateCompanyCustomerResource`. */
data class CreateCompanyCustomerRequestDto(
    @SerializedName("companyName") val companyName: String,
    @SerializedName("taxId") val taxId: String,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?
)

/** `AffiliateCustomerResource`: `taxId` obligatorio (docs/03-traceability.md US44 E1). */
data class AffiliateCustomerRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("taxId") val taxId: String,
    @SerializedName("phone") val phone: String?
)

/** `CreateVehicleResource`. */
data class CreateVehicleRequestDto(
    @SerializedName("plate") val plate: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("engineType") val engineType: String,
    @SerializedName("customerId") val customerId: String?,
    @SerializedName("vin") val vin: String?,
    @SerializedName("year") val year: Int?
)
