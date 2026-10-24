package com.andeva.atelier.mobile.features.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `el token se toma tal cual o del enlace del correo`() {
        assertEquals("abc123", AuthValidation.tokenFrom("  abc123 "))
        assertEquals("abc123", AuthValidation.tokenFrom("https://atelier.andeva.tech/auth/accept-invitation?token=abc123"))
        assertEquals("a b+c", AuthValidation.tokenFrom("https://atelier.andeva.tech/auth/reset-password?lang=es&token=a%20b%2Bc#top"))
        assertEquals("https://atelier.andeva.tech/auth", AuthValidation.tokenFrom("https://atelier.andeva.tech/auth"))
    }
}
