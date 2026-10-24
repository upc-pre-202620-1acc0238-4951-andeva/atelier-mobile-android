package com.andeva.atelier.mobile.features.auth.infrastructure.remote

import com.google.gson.annotations.SerializedName

// Modelos de request de docs/api/iam.md. La respuesta de sesión es AuthenticatedUserDto (núcleo).

data class SignInRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class GoogleSignInRequestDto(
    @SerializedName("idToken") val idToken: String
)

/** `token` es el OTP de 6 dígitos. */
data class VerifyEmailRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("token") val token: String
)

data class ResendVerificationRequestDto(
    @SerializedName("email") val email: String
)

data class ForgotPasswordRequestDto(
    @SerializedName("email") val email: String
)

data class ResetPasswordRequestDto(
    @SerializedName("token") val token: String,
    @SerializedName("newPassword") val newPassword: String
)

data class AcceptInvitationRequestDto(
    @SerializedName("token") val token: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("password") val password: String,
    @SerializedName("phone") val phone: String?
)
