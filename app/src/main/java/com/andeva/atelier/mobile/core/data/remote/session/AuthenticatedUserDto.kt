package com.andeva.atelier.mobile.core.data.remote.session

import com.google.gson.annotations.SerializedName

// Modelos de docs/api/iam.md: AuthenticatedUserResource, TenantSummaryResource y SessionPlanResource.

data class AuthenticatedUserDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("token") val token: String,
    @SerializedName("tokenType") val tokenType: String?,
    @SerializedName("activeTenant") val activeTenant: TenantSummaryDto?,
    @SerializedName("permissions") val permissions: List<String>?,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("membershipId") val membershipId: String?,
    @SerializedName("plan") val plan: SessionPlanDto?
)

data class TenantSummaryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("taxId") val taxId: String?
)

data class SessionPlanDto(
    @SerializedName("planName") val planName: String?,
    @SerializedName("tier") val tier: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("accessGranted") val accessGranted: Boolean,
    @SerializedName("iotTelemetryEnabled") val iotTelemetryEnabled: Boolean,
    @SerializedName("aiDiagnosticsEnabled") val aiDiagnosticsEnabled: Boolean
)

data class RefreshTokenRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

data class RegisterDeviceTokenRequestDto(
    @SerializedName("fcmToken") val fcmToken: String?
)

data class MessageResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("timestamp") val timestamp: String?
)
