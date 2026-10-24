package com.andeva.atelier.mobile.features.auth.application

import com.andeva.atelier.mobile.features.auth.domain.AcceptInvitation
import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import javax.inject.Inject

class AcceptInvitationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(request: AcceptInvitation) = authRepository.acceptInvitation(request)
}
