package com.andeva.atelier.mobile.core.data.local

import com.google.gson.JsonParser
import java.util.Base64

/** Lee claims del JWT sin verificar la firma (eso lo hace el backend). */
object JwtClaims {

    /** `exp` del token en epoch ms, o null si el token no se puede leer. */
    fun expiresAtMillis(token: String): Long? = runCatching {
        val payload = token.split(".")[1]
        val json = String(Base64.getUrlDecoder().decode(payload.padBase64()), Charsets.UTF_8)
        JsonParser.parseString(json).asJsonObject.get("exp")?.asLong?.times(1000)
    }.getOrNull()

    private fun String.padBase64(): String = this + "=".repeat((4 - length % 4) % 4)
}
