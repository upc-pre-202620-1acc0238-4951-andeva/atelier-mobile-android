package com.andeva.atelier.mobile.features.bays.domain

import kotlinx.coroutines.flow.Flow

/** Tablero de bahías de la sede de la sesión. Las firmas se completan con la implementación (paso 9, A3). */
interface BayRepository {
    suspend fun refreshBranchBays(): Result<Unit>

    fun observeBoard(): Flow<List<BayBoardItem>>

    suspend fun reassignBay(workOrderId: String, bayId: String): Result<Unit>

    suspend fun releaseBay(workOrderId: String): Result<Unit>

    suspend fun setBayMaintenance(bayId: String, reason: String): Result<WorkBay>

    suspend fun restoreBay(bayId: String): Result<WorkBay>

    suspend fun reassignMechanic(taskId: String, mechanicId: String): Result<Unit>

    suspend fun createBay(name: String, bayType: BayType): Result<WorkBay>
}
