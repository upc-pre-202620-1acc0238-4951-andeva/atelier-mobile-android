package com.andeva.atelier.mobile.features.workorders.application

import com.andeva.atelier.mobile.features.workorders.domain.InvalidWorkOrderInputException
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderValidation
import java.io.File
import javax.inject.Inject

/**
 * Foto de un panel en la inspección de recepción con la descripción del daño (US13 E1). Funciona sin red: se
 * comprime, se guarda y se encola; aparece en [ObservePendingIntakePhotosUseCase] hasta que el backend la registre.
 */
class AttachIntakePhotoUseCase @Inject constructor(
    private val workOrderRepository: WorkOrderRepository
) {
    suspend operator fun invoke(id: String, photo: File, description: String? = null): Result<Unit> {
        if (!WorkOrderValidation.isOptionalText(description, WorkOrderValidation.PHOTO_DESCRIPTION_MAX)) {
            return Result.failure(InvalidWorkOrderInputException("description"))
        }
        return workOrderRepository.attachIntakePhoto(id, photo, description)
    }
}
