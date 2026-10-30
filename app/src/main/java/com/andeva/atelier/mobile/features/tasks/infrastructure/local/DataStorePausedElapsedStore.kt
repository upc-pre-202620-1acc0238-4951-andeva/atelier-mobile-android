package com.andeva.atelier.mobile.features.tasks.infrastructure.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.andeva.atelier.mobile.features.tasks.domain.PausedElapsedStore
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Guarda un valor por tarea en el DataStore de la sesión: se borra solo al cerrar sesión. */
@Singleton
class DataStorePausedElapsedStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PausedElapsedStore {

    override suspend fun get(id: TaskId): Long? = dataStore.data.first()[key(id)]

    override suspend fun save(id: TaskId, seconds: Long) {
        dataStore.edit { it[key(id)] = seconds }
    }

    override suspend fun clear(id: TaskId) {
        dataStore.edit { it.remove(key(id)) }
    }

    private fun key(id: TaskId) = longPreferencesKey("task_paused_elapsed_${id.value}")
}
