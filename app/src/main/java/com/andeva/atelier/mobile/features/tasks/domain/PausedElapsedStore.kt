package com.andeva.atelier.mobile.features.tasks.domain

/**
 * Tiempo efectivo que llevaba una tarea al pausarla. El backend no envía la hora de la pausa, así que sin este
 * dato el cronómetro de una tarea pausada no puede mostrar el tiempo real trabajado (hueco H3). Se guarda al
 * pausar en este teléfono y se borra al reanudar o cerrar; una pausa hecha en otro dispositivo no tiene valor.
 */
interface PausedElapsedStore {
    suspend fun get(id: TaskId): Long?

    suspend fun save(id: TaskId, seconds: Long)

    suspend fun clear(id: TaskId)
}
