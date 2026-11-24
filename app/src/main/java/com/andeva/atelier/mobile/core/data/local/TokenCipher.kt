package com.andeva.atelier.mobile.core.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra los tokens de sesión (`access` y `refresh`) antes de persistirlos en DataStore (US04).
 * Las pruebas unitarias en JVM sin Android Keystore usan [NoOpTokenCipher].
 */
interface TokenCipher {
    fun encrypt(plain: String): String
    fun decrypt(stored: String): String?

    companion object {
        const val PREFIX = "enc:v1:"
    }
}

object NoOpTokenCipher : TokenCipher {
    override fun encrypt(plain: String): String = plain
    override fun decrypt(stored: String): String? = stored
}

/**
 * Cifrado autenticado AES-256-GCM nativo sobre AndroidKeyStore (`minSdk = 26`).
 * Se inicializa de forma perezosa y se autorrecupera si la clave del hardware queda corrupta o
 * tras una restauración sin Keystore (borra el alias corrupto y devuelve `null` para forzar re-login limpio).
 */
class AndroidKeyStoreTokenCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : TokenCipher {

    private val lock = Any()

    override fun encrypt(plain: String): String {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return TokenCipher.PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    override fun decrypt(stored: String): String? {
        if (!stored.startsWith(TokenCipher.PREFIX)) return stored
        return try {
            val encoded = stored.removePrefix(TokenCipher.PREFIX)
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH_BYTES) return null
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val ciphertext = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
            val key = getExistingKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    private fun getExistingKey(): SecretKey? = synchronized(lock) {
        try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            ks.getKey(keyAlias, null) as? SecretKey
        } catch (_: GeneralSecurityException) {
            deleteCorruptKey()
            null
        } catch (_: IOException) {
            deleteCorruptKey()
            null
        }
    }

    private fun getOrCreateKey(): SecretKey = synchronized(lock) {
        val existing = getExistingKey()
        if (existing != null) return@synchronized existing
        try {
            generateKey()
        } catch (_: GeneralSecurityException) {
            deleteCorruptKey()
            generateKey()
        } catch (_: IOException) {
            deleteCorruptKey()
            generateKey()
        }
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_BITS)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun deleteCorruptKey() {
        runCatching {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (ks.containsAlias(keyAlias)) ks.deleteEntry(keyAlias)
        }
    }

    companion object {
        const val DEFAULT_KEY_ALIAS = "atelier_session_aes_gcm_v1"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val AES_KEY_BITS = 256
    }
}
