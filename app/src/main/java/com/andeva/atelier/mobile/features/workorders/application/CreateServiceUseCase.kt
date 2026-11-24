package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import java.math.BigDecimal
import javax.inject.Inject

/** Registra un nuevo servicio estándar en el catálogo del taller (`operations:services:manage`). En línea. */
class CreateServiceUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(
        name: String,
        basePrice: BigDecimal,
        estimatedMinutes: Int,
        currency: String = Money.PEN
    ): Result<ServiceOption> {
        val trimmed = name.trim()
        val invalid = when {
            trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH -> "name"
            basePrice <= BigDecimal.ZERO -> "basePrice"
            estimatedMinutes <= 0 -> "estimatedMinutes"
            else -> null
        }
        if (invalid != null) return Result.failure(InvalidWorkOrderInputException(invalid))
        return workOrderRepository.createService(trimmed, basePrice, currency, estimatedMinutes)
    }

    private companion object {
        const val MAX_NAME_LENGTH = 150
    }
}
