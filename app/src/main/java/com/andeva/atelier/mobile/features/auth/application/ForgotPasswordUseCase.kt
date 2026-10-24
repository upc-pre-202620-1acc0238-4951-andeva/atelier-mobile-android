package com.andeva.atelier.mobile.features.auth.application

import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String) = authRepository.forgotPassword(email)
}
