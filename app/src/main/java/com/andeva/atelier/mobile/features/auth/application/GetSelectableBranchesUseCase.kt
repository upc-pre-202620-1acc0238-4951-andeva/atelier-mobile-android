package com.andeva.atelier.mobile.features.auth.application

import com.andeva.atelier.mobile.features.auth.domain.BranchChoiceRepository
import javax.inject.Inject

class GetSelectableBranchesUseCase @Inject constructor(
    private val branchChoiceRepository: BranchChoiceRepository
) {
    suspend operator fun invoke() = branchChoiceRepository.availableBranches()
}
