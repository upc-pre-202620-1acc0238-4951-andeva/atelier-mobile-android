package com.andeva.atelier.mobile.core.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtClaimsTest {

    private fun jwt(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256"}""".toByteArray())
        return "$header.${encoder.encodeToString(payload.toByteArray())}.firma"
    }

    @Test
    fun `lee exp en milisegundos`() {
        val token = jwt("""{"sub":"7b1c","tenantId":"c9a2","exp":1761000000}""")
        assertEquals(1_761_000_000_000L, JwtClaims.expiresAtMillis(token))
    }

    @Test
    fun `token ilegible devuelve null`() {
        assertNull(JwtClaims.expiresAtMillis("no-es-un-jwt"))
        assertNull(JwtClaims.expiresAtMillis(jwt("""{"sub":"7b1c"}""")))
    }
}
