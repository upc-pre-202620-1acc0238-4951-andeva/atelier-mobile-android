package com.andeva.atelier.mobile.features.bays.infrastructure.local

import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.WorkBayDto

/** Sin `branchId` en la respuesta se usa la sede consultada, para que caiga en su alcance `bays:{branchId}`. */
fun WorkBayDto.toEntity(fallbackBranchId: String, syncedAt: Long) = BayEntity(
    id = id,
    branchId = branchId ?: fallbackBranchId,
    name = name.orEmpty(),
    bayType = bayType.orEmpty(),
    status = status.orEmpty(),
    currentWorkOrderId = currentWorkOrderId,
    syncedAt = syncedAt
)

/** Un tipo o estado que la app no conoce queda en null. */
fun BayEntity.toDomain() = WorkBay(
    id = id,
    branchId = branchId,
    name = name,
    type = BayType.entries.firstOrNull { it.name == bayType },
    status = BayStatus.entries.firstOrNull { it.name == status },
    currentWorkOrderId = currentWorkOrderId
)
