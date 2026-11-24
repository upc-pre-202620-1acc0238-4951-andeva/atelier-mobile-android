package com.andeva.atelier.mobile.core.data.local

import androidx.datastore.preferences.core.stringPreferencesKey
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.TenantSummaryDto
import com.andeva.atelier.mobile.core.di.PlaintextTokenEncryptionMigration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class SessionManagerEncryptionTest {

    private class FakePrefixCipher : TokenCipher {
        override fun encrypt(plain: String): String =
            "enc:v1:" + Base64.getEncoder().encodeToString(plain.reversed().toByteArray(Charsets.UTF_8))

        override fun decrypt(stored: String): String {
            if (!stored.startsWith("enc:v1:")) return stored
            val decoded = String(Base64.getDecoder().decode(stored.removePrefix("enc:v1:")), Charsets.UTF_8)
            return decoded.reversed()
        }
    }

    private fun user() = AuthenticatedUserDto(
        userId = "u1",
        email = "dueno@taller.pe",
        fullName = "Dueño",
        token = "jwt-secret-token",
        tokenType = "Bearer",
        activeTenant = TenantSummaryDto("t1", "Taller Original", null),
        permissions = listOf("iam:tenants:manage"),
        refreshToken = "refresh-secret-token",
        membershipId = "m1",
        plan = null
    )

    @Test
    fun `cifra tokens en reposo y los descifra al leerlos`() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        val cipher = FakePrefixCipher()
        val manager = SessionManager(dataStore, cipher)

        manager.save(user())

        val rawPrefs = dataStore.data.first()
        val storedToken = rawPrefs[stringPreferencesKey("token")]!!
        val storedRefresh = rawPrefs[stringPreferencesKey("refresh_token")]!!
        assertTrue(storedToken.startsWith("enc:v1:"))
        assertTrue(storedRefresh.startsWith("enc:v1:"))
        assertNotEquals("jwt-secret-token", storedToken)

        assertEquals("jwt-secret-token", manager.getAuthToken())
        assertEquals("refresh-secret-token", manager.getRefreshToken())
    }

    @Test
    fun `migra tokens en texto plano al iniciar y actualiza el nombre del taller`() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        val plainManager = SessionManager(dataStore, NoOpTokenCipher)
        plainManager.save(user())

        val cipher = FakePrefixCipher()
        val migration = PlaintextTokenEncryptionMigration(cipher)
        val current = dataStore.data.first()
        assertTrue(migration.shouldMigrate(current))
        val migrated = migration.migrate(current)
        assertTrue(migrated[SessionManager.TOKEN]!!.startsWith("enc:v1:"))
        assertTrue(migrated[SessionManager.REFRESH_TOKEN]!!.startsWith("enc:v1:"))

        plainManager.updateTenantName("Automotriz El Sol S.A.C.")
        assertEquals("Automotriz El Sol S.A.C.", plainManager.getSession()!!.tenantName)
    }
}
