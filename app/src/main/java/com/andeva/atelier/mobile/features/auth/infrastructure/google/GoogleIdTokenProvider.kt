package com.andeva.atelier.mobile.features.auth.infrastructure.google

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.andeva.atelier.mobile.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * `idToken` de Google con Credential Manager (docs/04-mobile-integration.md §1): el `serverClientId`
 * es el **Web client ID**, el mismo que acepta el backend. Necesita un contexto de Activity.
 */
object GoogleIdTokenProvider {

    sealed interface Outcome {
        data class Token(val idToken: String) : Outcome
        data object Cancelled : Outcome
        data object NotConfigured : Outcome
        data class Failed(val cause: Throwable) : Outcome
    }

    suspend fun request(activityContext: Context): Outcome {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return Outcome.NotConfigured
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Outcome.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Outcome.Failed(IllegalStateException("Unexpected credential type"))
            }
        } catch (_: GetCredentialCancellationException) {
            Outcome.Cancelled
        } catch (e: GetCredentialException) {
            Outcome.Failed(e)
        }
    }
}
