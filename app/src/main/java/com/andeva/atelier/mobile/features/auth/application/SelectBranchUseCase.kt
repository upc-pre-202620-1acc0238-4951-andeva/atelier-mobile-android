package com.andeva.atelier.mobile.features.auth.application

import com.andeva.atelier.mobile.features.auth.domain.BranchChoiceRepository
import javax.inject.Inject

class SelectBranchUseCase @Inject constructor(
    private val branchChoiceRepository: BranchChoiceRepository
) {
    suspend operator fun invoke(branchId: String) = branchChoiceRepository.selectBranch(branchId)
}
