package com.andeva.atelier.mobile.features.auth.application

import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import javax.inject.Inject

class ValidateInvitationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(token: String) = authRepository.validateInvitation(token)
}
