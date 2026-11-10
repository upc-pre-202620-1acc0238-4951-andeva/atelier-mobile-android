package com.andeva.atelier.mobile.core.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Recuerda en el DataStore de la sesión si ya se pidió `POST_NOTIFICATIONS`. Como `SessionManager.clear()` vacía ese
 * DataStore, el permiso se pide una vez por inicio de sesión y no en cada arranque de la app.
 */
@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    suspend fun wasAsked(): Boolean = dataStore.data.first()[ASKED] == true

    suspend fun markAsked() {
        dataStore.edit { it[ASKED] = true }
    }

    private companion object {
        val ASKED = booleanPreferencesKey("notification_permission_asked")
    }
}
