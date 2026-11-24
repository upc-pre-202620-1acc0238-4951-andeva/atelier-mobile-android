package com.andeva.atelier.mobile.core.di

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.andeva.atelier.mobile.core.data.local.AndroidKeyStoreTokenCipher
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.local.TokenCipher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideTokenCipher(): TokenCipher = AndroidKeyStoreTokenCipher()

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
        tokenCipher: TokenCipher
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(PlaintextTokenEncryptionMigration(tokenCipher)),
        produceFile = { context.preferencesDataStoreFile("atelier_session") }
    )
}

/**
 * Migra atómicamente tokens heredados en texto plano a formato cifrado (`enc:v1:`) una sola vez
 * antes de que cualquier observador o [com.andeva.atelier.mobile.core.data.remote.TokenAuthenticator] lea DataStore.
 */
internal class PlaintextTokenEncryptionMigration(
    private val tokenCipher: TokenCipher
) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val token = currentData[SessionManager.TOKEN]
        val refresh = currentData[SessionManager.REFRESH_TOKEN]
        val tokenNeedsEncryption = !token.isNullOrEmpty() && !token.startsWith(TokenCipher.PREFIX)
        val refreshNeedsEncryption = !refresh.isNullOrEmpty() && !refresh.startsWith(TokenCipher.PREFIX)
        return tokenNeedsEncryption || refreshNeedsEncryption
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val mutable = currentData.toMutablePreferences()
        mutable[SessionManager.TOKEN]?.let { raw ->
            if (raw.isNotEmpty() && !raw.startsWith(TokenCipher.PREFIX)) {
                mutable[SessionManager.TOKEN] = tokenCipher.encrypt(raw)
            }
        }
        mutable[SessionManager.REFRESH_TOKEN]?.let { raw ->
            if (raw.isNotEmpty() && !raw.startsWith(TokenCipher.PREFIX)) {
                mutable[SessionManager.REFRESH_TOKEN] = tokenCipher.encrypt(raw)
            }
        }
        return mutable.toPreferences()
    }

    override suspend fun cleanUp() = Unit
}
