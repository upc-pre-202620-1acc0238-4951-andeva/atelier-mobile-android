package com.andeva.atelier.mobile.features.auth.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.BranchService
import com.andeva.atelier.mobile.features.auth.domain.BranchChoiceRepository
import com.andeva.atelier.mobile.features.auth.domain.BranchOption
import com.google.gson.Gson
import javax.inject.Inject

/** Reutiliza `BranchService` de `attendance` (mismo `GET /branches`): no se crea otro cliente. */
class BranchChoiceRepositoryImpl @Inject constructor(
    private val branchService: BranchService,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : BranchChoiceRepository {

    override suspend fun availableBranches(): Result<List<BranchOption>> =
        apiCall(gson, { branchService.getBranches() }) { branches ->
            branches
                .filter { it.isActive != false && !it.name.isNullOrBlank() }
                .map { BranchOption(id = it.id, name = it.name.orEmpty().trim(), geofenceRadiusMeters = it.geofenceRadiusMeters) }
                .sortedBy { it.name.lowercase() }
        }

    override suspend fun selectBranch(branchId: String) = sessionManager.selectBranch(branchId)

    override suspend fun skipBranchChoice() = sessionManager.dismissBranchChoice()
}
