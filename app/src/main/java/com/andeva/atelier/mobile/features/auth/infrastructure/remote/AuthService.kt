package com.andeva.atelier.mobile.features.auth.infrastructure.remote

import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.MessageResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Endpoints públicos de docs/api/iam.md (sin token; `refresh` y `sign-out` están en AuthRefreshService). */
interface AuthService {
    @POST("api/v1/auth/sign-in")
    suspend fun signIn(@Body body: SignInRequestDto): Response<AuthenticatedUserDto>

    @POST("api/v1/auth/google-sign-in")
    suspend fun googleSignIn(@Body body: GoogleSignInRequestDto): Response<AuthenticatedUserDto>

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequestDto): Response<MessageResponseDto>

    @POST("api/v1/auth/resend-verification")
    suspend fun resendVerification(@Body body: ResendVerificationRequestDto): Response<MessageResponseDto>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto): Response<MessageResponseDto>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): Response<MessageResponseDto>

    @GET("api/v1/invitations/validate")
    suspend fun validateInvitation(@Query("token") token: String): Response<InvitationValidationDto>

    /** 201 con la sesión completa. */
    @POST("api/v1/invitations/accept")
    suspend fun acceptInvitation(@Body body: AcceptInvitationRequestDto): Response<AuthenticatedUserDto>
}
