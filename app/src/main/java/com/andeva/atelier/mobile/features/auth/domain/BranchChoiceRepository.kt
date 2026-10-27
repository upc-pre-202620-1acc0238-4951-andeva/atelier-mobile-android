package com.andeva.atelier.mobile.features.auth.domain

/**
 * Elegir sede (pantalla A5b). Solo para cuentas que entraron sin perfil laboral (dueño o administrador):
 * al resto el backend les asigna una sede y un turno (docs/07-roadmap.md, paso 2).
 */
interface BranchChoiceRepository {
    /** `GET /branches`: sedes activas del taller. En línea. */
    suspend fun availableBranches(): Result<List<BranchOption>>

    /** Deja la sede en la sesión. No llama al backend: el perfil laboral sigue sin sede asignada. */
    suspend fun selectBranch(branchId: String)

    /** "Elegir después": se entra sin sede y no se vuelve a ofrecer. */
    suspend fun skipBranchChoice()
}
