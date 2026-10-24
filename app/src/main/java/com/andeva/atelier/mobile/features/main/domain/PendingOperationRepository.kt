package com.andeva.atelier.mobile.features.main.domain

import kotlinx.coroutines.flow.Flow

/** Pantalla "Pendientes de sincronizar" e ícono con contador (docs/06-offline-sync.md §3, "Reglas de producto"). */
interface PendingOperationRepository {
    /** Solo la cola del usuario de la sesión; vacía sin sesión. */
    fun observeAll(): Flow<List<PendingOperation>>

    fun observeCount(): Flow<Int>

    /** Vuelve a encolar una operación FAILED (y su cadena FAILED). false si no aplicaba. */
    suspend fun retry(id: String): Boolean

    /** Borra la operación con sus dependientes (y la foto local si era una subida). false si no aplicaba. */
    suspend fun discard(id: String): Boolean
}
