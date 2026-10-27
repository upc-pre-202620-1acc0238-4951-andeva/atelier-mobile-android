package com.andeva.atelier.mobile.core.data.local

import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionManagerBranchChoiceTest {

    private lateinit var sessionManager: SessionManager

    private fun user() = AuthenticatedUserDto(
        userId = "u1", email = "dueno@taller.pe", fullName = "Dueño", token = "jwt-1", tokenType = "Bearer",
        activeTenant = null, permissions = listOf("iam:branches:read"), refreshToken = "refresh-1",
        membershipId = "m1", plan = null
    )

    @Before
    fun setUp() {
        sessionManager = SessionManager(InMemoryPreferencesDataStore())
    }

    @Test
    fun `la marca pendiente antes de guardar ya se ve en la primera sesion`() = runTest {
        sessionManager.markBranchChoicePending()
        assertNull(sessionManager.getSession())              // sin token no hay sesión
        sessionManager.save(user())
        assertTrue(sessionManager.getSession()!!.needsBranchChoice)
    }

    @Test
    fun `elegir sede la deja en la sesion y apaga el aviso`() = runTest {
        sessionManager.markBranchChoicePending()
        sessionManager.save(user())

        sessionManager.selectBranch("b2")

        val session = sessionManager.getSession()!!
        assertEquals("b2", session.branchId)
        assertFalse(session.needsBranchChoice)
    }

    @Test
    fun `la sede elegida sobrevive cuando el perfil laboral sigue sin existir`() = runTest {
        sessionManager.save(user())
        sessionManager.selectBranch("b2")

        sessionManager.updateAssignment(null, null)          // refreshAssignment ante un 404

        assertEquals("b2", sessionManager.getSession()!!.branchId)
    }

    @Test
    fun `una sede asignada por el backend reemplaza a la elegida`() = runTest {
        sessionManager.save(user())
        sessionManager.selectBranch("b2")

        sessionManager.updateAssignment("b1", "s1")

        val session = sessionManager.getSession()!!
        assertEquals("b1", session.branchId)
        assertEquals("s1", session.shiftId)
        assertFalse(session.needsBranchChoice)
    }

    @Test
    fun `elegir despues entra sin sede y no vuelve a preguntar`() = runTest {
        sessionManager.markBranchChoicePending()
        sessionManager.save(user())

        sessionManager.dismissBranchChoice()

        val session = sessionManager.getSession()!!
        assertNull(session.branchId)
        assertFalse(session.needsBranchChoice)
    }

    @Test
    fun `cerrar sesion borra la sede elegida`() = runTest {
        sessionManager.save(user())
        sessionManager.selectBranch("b2")
        sessionManager.clear()
        assertNull(sessionManager.getSession())
        sessionManager.save(user())
        assertNull(sessionManager.getSession()!!.branchId)
    }
}
