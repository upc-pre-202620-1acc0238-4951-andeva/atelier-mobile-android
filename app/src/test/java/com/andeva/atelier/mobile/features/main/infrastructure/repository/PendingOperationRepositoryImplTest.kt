package com.andeva.atelier.mobile.features.main.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.sync.FakePendingOperationDao
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.core.data.sync.PendingUploadDao
import com.andeva.atelier.mobile.core.data.sync.PendingUploadEntity
import com.andeva.atelier.mobile.core.data.sync.SyncScheduler
import com.andeva.atelier.mobile.core.data.sync.UploadFileHandler
import com.andeva.atelier.mobile.core.data.sync.UploadFilePayload
import com.andeva.atelier.mobile.features.main.domain.PendingOperationStatus
import com.google.gson.Gson
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PendingOperationRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val gson = Gson()
    private val dao = FakePendingOperationDao()
    private val uploadDao = FakePendingUploadDao()
    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: PendingOperationRepositoryImpl

    private fun user(userId: String) = AuthenticatedUserDto(
        userId = userId, email = "$userId@taller.pe", fullName = "Colaborador", token = "jwt", tokenType = "Bearer",
        activeTenant = null, permissions = emptyList(), refreshToken = "refresh", membershipId = "m-$userId", plan = null
    )

    private fun op(
        id: String,
        type: String = "CLOCK_IN",
        userId: String = "u1",
        status: String = PendingOperationEntity.STATUS_PENDING,
        dependsOn: String? = null,
        payload: String = "{}"
    ) = PendingOperationEntity(
        id = id, type = type, payload = payload, dedupeKey = "$type:$id", userId = userId, tenantId = "t1",
        status = status, attempts = if (status == PendingOperationEntity.STATUS_FAILED) 3 else 0,
        nextAttemptAt = 0, createdAt = id.hashCode().toLong(), dependsOn = dependsOn,
        lastErrorMessage = if (status == PendingOperationEntity.STATUS_FAILED) "Error del backend" else null
    )

    /** Una foto en filesDir/pending con su UPLOAD_FILE y el ATTACH_* que depende de ella. */
    private suspend fun photo(uploadStatus: String = PendingOperationEntity.STATUS_PENDING): File {
        val file = tempFolder.newFile("photo.jpg")
        uploadDao.insert(PendingUploadEntity("up1", file.path, "t1/work-orders/wo1/intake/up1.jpg", "INTAKE", createdAt = 0))
        dao.enqueue(op("upload", UploadFileHandler.TYPE, status = uploadStatus, payload = gson.toJson(UploadFilePayload("up1"))))
        dao.enqueue(op("attach", "ATTACH_INTAKE_IMAGE", dependsOn = "upload"))
        return file
    }

    @Before
    fun setUp() = runTest {
        sessionManager = SessionManager(InMemoryPreferencesDataStore())
        sessionManager.save(user("u1"))
        repository = PendingOperationRepositoryImpl(dao, uploadDao, sessionManager, syncScheduler, gson)
    }

    @Test
    fun `lista y contador solo traen la cola del usuario de la sesion`() = runTest {
        dao.enqueue(op("mine", status = PendingOperationEntity.STATUS_FAILED))
        dao.enqueue(op("theirs", userId = "u2"))

        val list = repository.observeAll().first()
        assertEquals(listOf("mine"), list.map { it.id })
        assertEquals(PendingOperationStatus.FAILED, list.single().status)
        assertEquals("Error del backend", list.single().lastErrorMessage)
        assertEquals(1, repository.observeCount().first())

        sessionManager.clear()
        assertEquals(emptyList<Any>(), repository.observeAll().first())
        assertEquals(0, repository.observeCount().first())
    }

    @Test
    fun `reintentar una FAILED la reactiva con su cadena y dispara el sync`() = runTest {
        dao.enqueue(op("upload", UploadFileHandler.TYPE, status = PendingOperationEntity.STATUS_FAILED))
        dao.enqueue(op("attach", "ATTACH_INTAKE_IMAGE", status = PendingOperationEntity.STATUS_FAILED, dependsOn = "upload"))

        assertTrue(repository.retry("upload"))

        listOf("upload", "attach").forEach { id ->
            val row = dao.findById(id)!!
            assertEquals(PendingOperationEntity.STATUS_PENDING, row.status)
            assertEquals(0, row.attempts)
        }
        verify { syncScheduler.requestSync() }
    }

    @Test
    fun `reintentar un hijo reactiva tambien su padre FAILED`() = runTest {
        dao.enqueue(op("clockIn", status = PendingOperationEntity.STATUS_FAILED))
        dao.enqueue(op("clockOut", "CLOCK_OUT", status = PendingOperationEntity.STATUS_FAILED, dependsOn = "clockIn"))

        assertTrue(repository.retry("clockOut"))

        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.findById("clockIn")!!.status)
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.findById("clockOut")!!.status)
    }

    @Test
    fun `reintentar no reactiva dependientes que no fallaron`() = runTest {
        dao.enqueue(op("parent", status = PendingOperationEntity.STATUS_FAILED))
        dao.enqueue(op("child", "CLOCK_OUT", status = PendingOperationEntity.STATUS_SYNCING, dependsOn = "parent"))

        repository.retry("parent")

        assertEquals(PendingOperationEntity.STATUS_SYNCING, dao.findById("child")!!.status)
    }

    @Test
    fun `reintentar una PENDING o de otro usuario no hace nada`() = runTest {
        dao.enqueue(op("pending"))
        dao.enqueue(op("theirs", userId = "u2", status = PendingOperationEntity.STATUS_FAILED))

        assertFalse(repository.retry("pending"))
        assertFalse(repository.retry("theirs"))
        assertEquals(PendingOperationEntity.STATUS_FAILED, dao.findById("theirs")!!.status)
        verify(exactly = 0) { syncScheduler.requestSync() }
    }

    @Test
    fun `descartar una subida se lleva su ATTACH, la fila y el archivo`() = runTest {
        val file = photo()

        assertTrue(repository.discard("upload"))

        assertNull(dao.findById("upload"))
        assertNull(dao.findById("attach"))
        assertNull(uploadDao.findById("up1"))
        assertFalse(file.exists())
    }

    @Test
    fun `descartar un ATTACH se lleva su UPLOAD_FILE y la foto`() = runTest {
        val file = photo()

        assertTrue(repository.discard("attach"))

        assertTrue(dao.all().isEmpty())
        assertNull(uploadDao.findById("up1"))
        assertFalse(file.exists())
    }

    @Test
    fun `descartar un CLOCK_OUT deja su CLOCK_IN`() = runTest {
        dao.enqueue(op("clockIn"))
        dao.enqueue(op("clockOut", "CLOCK_OUT", status = PendingOperationEntity.STATUS_FAILED, dependsOn = "clockIn"))

        assertTrue(repository.discard("clockOut"))

        assertNotNull(dao.findById("clockIn"))
        assertNull(dao.findById("clockOut"))
    }

    @Test
    fun `no se descarta nada si la cadena se esta enviando`() = runTest {
        val file = photo(uploadStatus = PendingOperationEntity.STATUS_SYNCING)

        assertFalse(repository.discard("attach"))

        assertNotNull(dao.findById("upload"))
        assertNotNull(dao.findById("attach"))
        assertTrue(file.exists())
    }

    @Test
    fun `no se descarta la cola de otro usuario`() = runTest {
        dao.enqueue(op("theirs", userId = "u2"))

        assertFalse(repository.discard("theirs"))
        assertNotNull(dao.findById("theirs"))
    }

    private class FakePendingUploadDao : PendingUploadDao {
        private val rows = mutableMapOf<String, PendingUploadEntity>()

        override suspend fun insert(upload: PendingUploadEntity) {
            rows[upload.id] = upload
        }

        override suspend fun findById(id: String) = rows[id]

        override fun observeByTarget(target: String): Flow<List<PendingUploadEntity>> =
            flowOf(rows.values.filter { it.target == target })

        override suspend fun setPublicUrl(id: String, publicUrl: String) {
            rows[id]?.let { rows[id] = it.copy(publicUrl = publicUrl) }
        }

        override suspend fun delete(id: String) {
            rows.remove(id)
        }
    }
}
