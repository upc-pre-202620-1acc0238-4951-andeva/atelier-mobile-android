package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.EvidenceKind
import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskValidation
import java.io.File
import javax.inject.Inject

/**
 * Foto de desmontaje o montaje (US28 E1/E2) tomada con CameraX o el Photo Picker. Funciona sin red: se comprime,
 * se guarda y se encola; aparece en [ObservePendingEvidenceUseCase] hasta que el backend la registre.
 */
class AttachTaskEvidenceUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(id: TaskId, workOrderId: String, photo: File, kind: EvidenceKind, note: String? = null): Result<Unit> {
        val description = kind.describe(note)
        if (!TaskValidation.isEvidenceDescription(description)) return Result.failure(InvalidTaskInputException("description"))
        return taskRepository.attachEvidence(id, workOrderId, photo, description)
    }
}
