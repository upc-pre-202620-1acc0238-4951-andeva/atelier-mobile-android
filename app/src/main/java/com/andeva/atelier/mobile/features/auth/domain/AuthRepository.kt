package com.andeva.atelier.mobile.features.auth.domain

/**
 * Sesión y acceso (US01–US05, docs/api/iam.md). Todo es en línea: nada se encola.
 * Los métodos que devuelven `String?` entregan el `message` del backend (puede venir vacío).
 */
interface AuthRepository {
    /** Inicia sesión y deja guardada la sesión con su sede y turno. */
    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    suspend fun verifyEmail(email: String, code: String): Result<String?>

    suspend fun resendVerification(email: String): Result<String?>

    suspend fun validateInvitation(token: String): Result<InvitationPreview>

    /** Acepta la invitación; el backend devuelve la sesión completa y se entra directo. */
    suspend fun acceptInvitation(request: AcceptInvitation): Result<Unit>

    suspend fun forgotPassword(email: String): Result<String?>

    suspend fun resetPassword(token: String, newPassword: String): Result<String?>

    /** Operaciones de la cola del usuario actual que aún no se enviaron. */
    suspend fun pendingOperationCount(): Int

    /** Cierra sesión en el dispositivo. La cola `pending_operations` se conserva (docs/06-offline-sync.md §3). */
    suspend fun signOut()
}
