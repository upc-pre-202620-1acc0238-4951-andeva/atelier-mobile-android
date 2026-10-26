package com.andeva.atelier.mobile.features.workorders.infrastructure.local

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto

/**
 * Deja al día la cabecera que devolvió una acción o el detalle de una orden. Una orden cerrada (`PAID`, `CANCELED`)
 * sale de la caché de la sede en vez de guardarse (docs/06-offline-sync.md §4).
 */
suspend fun WorkOrderDao.saveHeaderOrDeleteClosed(dto: WorkOrderDto, syncedAt: Long): WorkOrder {
    val row = dto.toEntity(syncedAt)
    val closed = WorkOrderStatus.entries.any { it.name == dto.status && it.isClosed }
    if (closed) {
        delete(dto.id)
        return row.toDomain()
    }
    saveHeader(row)
    return findById(dto.id)?.toDomain() ?: row.toDomain()
}
