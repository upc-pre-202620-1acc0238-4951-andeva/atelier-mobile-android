package com.andeva.atelier.mobile.features.auth.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.session.AuthRefreshService
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.RefreshTokenRequestDto
import com.andeva.atelier.mobile.core.data.sync.CacheMaintenanceDao
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.di.RefreshClient
import com.andeva.atelier.mobile.core.notifications.DeviceTokenRegistrar
import com.andeva.atelier.mobile.features.auth.domain.AcceptInvitation
import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import com.andeva.atelier.mobile.features.auth.domain.InvalidInvitationException
import com.andeva.atelier.mobile.features.auth.domain.InvitationPreview
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.AcceptInvitationRequestDto
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.AuthService
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.EmployeeService
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.ForgotPasswordRequestDto
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.GoogleSignInRequestDto
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.ResendVerificationRequestDto
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.ResetPasswordRequestDto
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.SignInRequestDto
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.VerifyEmailRequestDto
import com.google.gson.Gson
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val employeeService: EmployeeService,
    @RefreshClient private val refreshService: AuthRefreshService,
    private val sessionManager: SessionManager,
    private val deviceTokenRegistrar: DeviceTokenRegistrar,
    private val cacheMaintenanceDao: CacheMaintenanceDao,
    private val pendingOperationDao: PendingOperationDao,
    private val gson: Gson
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        apiCall(gson, { authService.signIn(SignInRequestDto(email.trim(), password)) }) { it }
            .mapCatching { startSession(it) }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        apiCall(gson, { authService.googleSignIn(GoogleSignInRequestDto(idToken)) }) { it }
            .mapCatching { startSession(it) }

    override suspend fun verifyEmail(email: String, code: String): Result<String?> =
        apiCall(gson, { authService.verifyEmail(VerifyEmailRequestDto(email.trim(), code)) }) { it.message }

    override suspend fun resendVerification(email: String): Result<String?> =
        apiCall(gson, { authService.resendVerification(ResendVerificationRequestDto(email.trim())) }) { it.message }

    override suspend fun validateInvitation(token: String): Result<InvitationPreview> =
        apiCall(gson, { authService.validateInvitation(token.trim()) }) { it }
            .mapCatching { dto ->
                if (!dto.valid) throw InvalidInvitationException()
                InvitationPreview(
                    email = dto.email.orEmpty(),
                    tenantName = dto.tenantName.orEmpty(),
                    roleName = dto.roleName.orEmpty()
                )
            }

    override suspend fun acceptInvitation(request: AcceptInvitation): Result<Unit> {
        val body = AcceptInvitationRequestDto(
            token = request.token.trim(),
            firstName = request.firstName.trim(),
            lastName = request.lastName.trim(),
            password = request.password,
            phone = request.phone?.trim()?.ifEmpty { null }
        )
        return apiCall(gson, { authService.acceptInvitation(body) }) { it }
            .mapCatching { startSession(it) }
    }

    override suspend fun forgotPassword(email: String): Result<String?> =
        apiCall(gson, { authService.forgotPassword(ForgotPasswordRequestDto(email.trim())) }) { it.message }

    override suspend fun resetPassword(token: String, newPassword: String): Result<String?> =
        apiCall(gson, { authService.resetPassword(ResetPasswordRequestDto(token.trim(), newPassword)) }) { it.message }

    override suspend fun pendingOperationCount(): Int {
        val userId = sessionManager.getSession()?.userId ?: return 0
        return pendingOperationDao.countPending(userId).first()
    }

    /**
     * Orden de docs/07-roadmap.md (Paso 2): token FCM → sign-out → cachés → sesión. Los fallos de
     * red se ignoran: sin conexión igual se sale. La cola `pending_operations` no se toca.
     */
    override suspend fun signOut() = withContext(NonCancellable) {
        val refreshToken = sessionManager.getRefreshToken()
        deviceTokenRegistrar.clear()
        if (refreshToken != null) {
            try {
                refreshService.signOut(RefreshTokenRequestDto(refreshToken))
            } catch (_: IOException) {
                // Sin red: el refresh token vence solo (30 días)
            }
        }
        cacheMaintenanceDao.clearCaches()
        sessionManager.clear()
    }

    /**
     * Post-login común de sign-in, Google y aceptar invitación. La sede se pide **antes** de guardar:
     * al guardar, AppNavHost cambia de grafo y destruye el ViewModel que llamó.
     */
    private suspend fun startSession(user: AuthenticatedUserDto) {
        val profile = user.membershipId?.let { membershipId ->
            // Dueño o administrador sin perfil laboral (404) o sin red: se entra sin sede
            apiCall(gson, { employeeService.getByMembership("Bearer ${user.token}", membershipId) }) { it }
                .getOrNull()
        }
        withContext(NonCancellable) {
            sessionManager.save(user)
            sessionManager.updateAssignment(profile?.branchId, profile?.assignedShiftId)
            // Sin google-services.json no hay FCM: el login sigue igual
            deviceTokenRegistrar.registerCurrentToken()
        }
    }
}
