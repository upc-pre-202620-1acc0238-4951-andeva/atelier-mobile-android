package com.andeva.atelier.mobile.features.workorders.presentation.intake

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.workorders.domain.IntakeImage
import com.andeva.atelier.mobile.features.workorders.domain.PendingIntakePhoto
import java.io.File

/**
 * Paneles de la inspección de recepción (US13). [storedLabel] abre la descripción de la foto en el backend, que no
 * tiene un campo de posición (docs/api/operations.md): así se reconoce a qué panel pertenece cada foto.
 */
enum class InspectionAngle(val storedLabel: String) {
    FRONT("Frente"),
    REAR("Trasera"),
    LEFT("Lateral izquierdo"),
    RIGHT("Lateral derecho"),
    ROOF("Techo"),
    DASHBOARD("Tablero"),
    OTHER("Otra foto");

    companion object {
        /** Descripción que se guarda: el panel y, si hay, la nota del daño. */
        fun describe(angle: InspectionAngle, note: String): String {
            val trimmed = note.trim()
            return if (trimmed.isEmpty()) angle.storedLabel else "${angle.storedLabel}: $trimmed"
        }
    }
}

enum class PhotoStatus { UPLOADED, QUEUED, FAILED }

/** Foto de la galería: la que ya está en el backend o la que espera señal ([model] es la imagen a dibujar). */
data class GalleryPhoto(
    val key: String,
    val model: Any,
    val description: String?,
    val status: PhotoStatus
)

data class IntakeGalleryUiState(
    val note: String = "",
    val uploaded: List<IntakeImage> = emptyList(),
    val pending: List<PendingIntakePhoto> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val canUpload: Boolean = false,
    val canRead: Boolean = false
) {
    /** En cola primero (lo último que tomó el usuario) y luego las ya registradas. */
    val photos: List<GalleryPhoto>
        get() = pending.map { photo ->
            GalleryPhoto(
                key = photo.operationId,
                model = photo.localFilePath?.let(::File) ?: photo.imageUrl,
                description = photo.description,
                status = if (photo.status == FAILED) PhotoStatus.FAILED else PhotoStatus.QUEUED
            )
        } + uploaded.map { GalleryPhoto(it.id, it.imageUrl, it.description, PhotoStatus.UPLOADED) }

    fun photosFor(angle: InspectionAngle): List<GalleryPhoto> =
        if (angle == InspectionAngle.OTHER) {
            photos.filter { photo -> InspectionAngle.entries.none { it != InspectionAngle.OTHER && photo.matches(it) } }
        } else {
            photos.filter { it.matches(angle) }
        }

    private fun GalleryPhoto.matches(angle: InspectionAngle) = description?.startsWith(angle.storedLabel) == true

    private companion object {
        const val FAILED = "FAILED"
    }
}
