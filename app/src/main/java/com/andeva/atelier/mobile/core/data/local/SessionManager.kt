package com.andeva.atelier.mobile.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.domain.Plan
import com.andeva.atelier.mobile.core.domain.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Sesión del usuario en DataStore. Claves de docs/06-offline-sync.md §5. */
@Singleton
class SessionManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TOKEN = stringPreferencesKey("token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val TOKEN_EXPIRES_AT = longPreferencesKey("token_expires_at")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val FULL_NAME = stringPreferencesKey("full_name")
        val TENANT_ID = stringPreferencesKey("tenant_id")
        val TENANT_NAME = stringPreferencesKey("tenant_name")
        val MEMBERSHIP_ID = stringPreferencesKey("membership_id")
        val PERMISSIONS = stringSetPreferencesKey("permissions")
        val BRANCH_ID = stringPreferencesKey("branch_id")
        val SHIFT_ID = stringPreferencesKey("shift_id")
        val PLAN_TIER = stringPreferencesKey("plan_tier")
        val PLAN_STATUS = stringPreferencesKey("plan_status")
        val PLAN_ACCESS_GRANTED = booleanPreferencesKey("plan_access_granted")
        val IOT_TELEMETRY_ENABLED = booleanPreferencesKey("iot_telemetry_enabled")
        val AI_DIAGNOSTICS_ENABLED = booleanPreferencesKey("ai_diagnostics_enabled")
    }

    /** null cuando no hay sesión: AppNavHost vuelve al login. */
    val session: Flow<Session?> = dataStore.data.map { it.toSession() }

    suspend fun getSession(): Session? = session.first()
    suspend fun getAuthToken(): String? = dataStore.data.map { it[TOKEN] }.first()
    suspend fun getRefreshToken(): String? = dataStore.data.map { it[REFRESH_TOKEN] }.first()
    suspend fun getTokenExpiresAt(): Long? = dataStore.data.map { it[TOKEN_EXPIRES_AT] }.first()

    /** Guarda la respuesta de sign-in, google-sign-in, invitations/accept o refresh. */
    suspend fun save(user: AuthenticatedUserDto) {
        dataStore.edit {
            it[TOKEN] = user.token
            it[REFRESH_TOKEN] = user.refreshToken
            JwtClaims.expiresAtMillis(user.token)?.let { exp -> it[TOKEN_EXPIRES_AT] = exp } ?: it.remove(TOKEN_EXPIRES_AT)
            it[USER_ID] = user.userId
            it[EMAIL] = user.email
            it[FULL_NAME] = user.fullName.orEmpty()
            it[PERMISSIONS] = user.permissions.orEmpty().toSet()
            user.activeTenant?.let { tenant ->
                it[TENANT_ID] = tenant.id
                it[TENANT_NAME] = tenant.name.orEmpty()
            }
            user.membershipId?.let { id -> it[MEMBERSHIP_ID] = id }
            val plan = user.plan
            if (plan != null) {
                it[PLAN_TIER] = plan.tier.orEmpty()
                it[PLAN_STATUS] = plan.status.orEmpty()
                it[PLAN_ACCESS_GRANTED] = plan.accessGranted
                it[IOT_TELEMETRY_ENABLED] = plan.iotTelemetryEnabled
                it[AI_DIAGNOSTICS_ENABLED] = plan.aiDiagnosticsEnabled
            } else {
                listOf(PLAN_TIER, PLAN_STATUS).forEach { key -> it.remove(key) }
                listOf(PLAN_ACCESS_GRANTED, IOT_TELEMETRY_ENABLED, AI_DIAGNOSTICS_ENABLED).forEach { key -> it.remove(key) }
            }
        }
    }

    /** Sede y turno asignados, de GET /hr/employees/membership/{membershipId}. */
    suspend fun updateAssignment(branchId: String?, shiftId: String?) {
        dataStore.edit {
            if (branchId != null) it[BRANCH_ID] = branchId else it.remove(BRANCH_ID)
            if (shiftId != null) it[SHIFT_ID] = shiftId else it.remove(SHIFT_ID)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toSession(): Session? {
        if (this[TOKEN] == null) return null
        val userId = this[USER_ID] ?: return null
        return Session(
            userId = userId,
            email = this[EMAIL].orEmpty(),
            fullName = this[FULL_NAME].orEmpty(),
            tenantId = this[TENANT_ID].orEmpty(),
            tenantName = this[TENANT_NAME].orEmpty(),
            membershipId = this[MEMBERSHIP_ID],
            permissions = this[PERMISSIONS].orEmpty(),
            plan = this[PLAN_TIER]?.let { tier ->
                Plan(
                    tier = tier,
                    status = this[PLAN_STATUS].orEmpty(),
                    accessGranted = this[PLAN_ACCESS_GRANTED] ?: true,
                    iotTelemetryEnabled = this[IOT_TELEMETRY_ENABLED] ?: true,
                    aiDiagnosticsEnabled = this[AI_DIAGNOSTICS_ENABLED] ?: true
                )
            },
            branchId = this[BRANCH_ID],
            shiftId = this[SHIFT_ID]
        )
    }
}
