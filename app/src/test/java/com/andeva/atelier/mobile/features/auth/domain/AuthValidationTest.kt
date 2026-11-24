package com.andeva.atelier.mobile.features.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `el token se toma tal cual o del enlace del correo`() {
        assertEquals("abc123", AuthValidation.tokenFrom("  abc123 "))
        assertEquals("abc123", AuthValidation.tokenFrom("https://atelier.andeva.tech/auth/accept-invitation?token=abc123"))
        assertEquals("a b+c", AuthValidation.tokenFrom("https://atelier.andeva.tech/auth/reset-password?lang=es&token=a%20b%2Bc#top"))
        assertEquals("https://atelier.andeva.tech/auth", AuthValidation.tokenFrom("https://atelier.andeva.tech/auth"))
    }

    @Test
    fun `tokenFrom soporta porcentaje malformado, token vacio y limite de 512 caracteres`() {
        assertEquals("abc%ZZ123", AuthValidation.tokenFrom("https://atelier.andeva.tech/auth/reset-password?token=abc%ZZ123"))
        assertEquals("", AuthValidation.tokenFrom("https://atelier.andeva.tech/auth/reset-password?token="))
        val longToken = "a".repeat(600)
        assertEquals(512, AuthValidation.tokenFrom(longToken).length)
    }

    @Test
    fun `isEmail rechaza dominios con etiqueta vacia o puntos consecutivos`() {
        assertTrue(AuthValidation.isEmail("andres@taller.pe"))
        assertFalse(AuthValidation.isEmail("a@.b"))
        assertFalse(AuthValidation.isEmail("a@b..pe"))
        assertFalse(AuthValidation.isEmail("a@-taller.pe"))
    }
}

