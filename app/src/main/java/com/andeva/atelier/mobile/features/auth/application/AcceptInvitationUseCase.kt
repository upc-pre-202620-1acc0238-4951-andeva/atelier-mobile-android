package com.andeva.atelier.mobile.features.auth.application

import com.andeva.atelier.mobile.features.auth.domain.AcceptInvitation
import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import com.andeva.atelier.mobile.features.auth.domain.AuthValidation
import javax.inject.Inject

/** Acepta el token o el enlace completo del correo de invitación (H1). */
class AcceptInvitationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(request: AcceptInvitation) =
        authRepository.acceptInvitation(request.copy(token = AuthValidation.tokenFrom(request.token)))
}
