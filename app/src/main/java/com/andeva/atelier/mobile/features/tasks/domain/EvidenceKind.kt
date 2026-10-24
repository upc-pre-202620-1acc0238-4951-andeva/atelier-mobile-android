package com.andeva.atelier.mobile.features.tasks.domain

/**
 * Tipo de foto de US28. `AttachTaskEvidenceResource` no tiene campo de tipo, así que va al inicio de la
 * `description` (docs/03-traceability.md US28). Es un dato que guarda el backend, no un texto de la app.
 */
enum class EvidenceKind(val label: String) {
    DISASSEMBLY("Desmontaje"),
    ASSEMBLY("Montaje");

    fun describe(note: String?): String = note?.trim()?.takeIf { it.isNotEmpty() }?.let { "$label: $it" } ?: label
}
