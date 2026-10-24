package com.andeva.atelier.mobile.features.inventory.infrastructure

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.TenantSummaryDto
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.media.SupabaseUploader
import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.InventoryService
import com.andeva.atelier.mobile.features.inventory.infrastructure.repository.InventoryRepositoryImpl
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Backend simulado (MockWebServer) + Room en memoria + sesión en memoria para los tests de `inventory`. Hora fija:
 * 2026-10-24 09:00 de Lima. Supabase simulado: `compress` copia el archivo y `upload` devuelve la URL pública.
 */
class InventoryTestEnv {
    val gson = Gson()
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)
    private val server = MockWebServer()

    /** "METHOD /ruta" sin query → respuesta; [pages] responde `GET /api/v1/inventory/items` según `page`. Lo demás, 404. */
    val responses = ConcurrentHashMap<String, MockResponse>()
    val pages = ConcurrentHashMap<Int, MockResponse>()

    /** Respuestas en orden para una ruta ("METHOD /ruta"); al agotarse se usa [responses]. */
    val sequences = ConcurrentHashMap<String, ArrayDeque<MockResponse>>()
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    val uploadedPaths = CopyOnWriteArrayList<String>()
    val uploader = mockk<SupabaseUploader>().also { uploader ->
        coEvery { uploader.compress(any(), any()) } coAnswers {
            val target = secondArg<File>()
            target.parentFile?.mkdirs()
            firstArg<File>().copyTo(target, overwrite = true)
        }
        coEvery { uploader.upload(any(), any()) } coAnswers {
            uploadedPaths += secondArg<String>()
            Result.success("https://demo.supabase.co/storage/v1/object/public/atelier/${secondArg<String>()}")
        }
    }

    val context: Context get() = ApplicationProvider.getApplicationContext()

    lateinit var db: AppDatabase
    lateinit var sessionManager: SessionManager
    private lateinit var retrofit: Retrofit

    fun start() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionManager = SessionManager(InMemoryPreferencesDataStore())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.path!!.substringBefore('?')
                val page = request.requestUrl?.queryParameter("page")?.toIntOrNull()
                if (request.method == "GET" && path == "/api/v1/inventory/items" && page != null) {
                    return pages[page] ?: json(emptyList<Any>())
                }
                val key = "${request.method} $path"
                synchronized(sequences) { sequences[key]?.removeFirstOrNull() }?.let { return it }
                return responses[key] ?: error(404, "NOT_FOUND", "No encontrado")
            }
        }
        server.start()
        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(sessionManager)).build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun stop() {
        server.shutdown()
        db.close()
    }

    fun repository() = InventoryRepositoryImpl(
        inventoryService = retrofit.create(InventoryService::class.java),
        partDao = db.partDao(),
        batchDao = db.batchDao(),
        cacheMetadataDao = db.cacheMetadataDao(),
        uploader = uploader,
        sessionManager = sessionManager,
        context = context,
        gson = gson,
        clock = clock
    )

    /** Encargado de almacén. */
    suspend fun signIn(
        permissions: List<Permission> = listOf(
            Permission.PARTS_READ, Permission.BATCHES_READ, Permission.ALERTS_READ, Permission.PURCHASE_ORDERS_READ,
            Permission.BATCHES_RECEIVE
        )
    ) {
        sessionManager.save(
            AuthenticatedUserDto(
                userId = "u1", email = "almacen@taller.pe", fullName = "Almacén", token = "jwt", tokenType = "Bearer",
                activeTenant = TenantSummaryDto("t1", "Taller Suárez", null), permissions = permissions.map { it.value },
                refreshToken = "refresh", membershipId = "m1", plan = null
            )
        )
    }

    fun json(body: Any, code: Int = 200): MockResponse = MockResponse().setResponseCode(code).setBody(gson.toJson(body))

    fun error(code: Int, errorCode: String, message: String): MockResponse =
        MockResponse().setResponseCode(code).setBody("""{"code":"$errorCode","message":"$message","details":[]}""")

    fun disconnect(): MockResponse = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

    fun part(
        id: String = "p1",
        name: String = "Pastillas de freno",
        sku: String = "BRK-001",
        stock: Double = 12.0,
        minimum: Double = 5.0,
        status: String = "ACTIVE"
    ): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "name" to name, "sku" to sku, "category" to "BRAKES", "basePrice" to 85.0,
        "totalStock" to stock, "minimumStock" to minimum, "unitOfMeasure" to "UNIT", "status" to status
    )

    fun batch(id: String, arrival: String, remaining: Double, itemId: String = "p1"): Map<String, Any?> = mapOf(
        "id" to id, "itemId" to itemId, "supplierId" to "s1", "purchaseOrderId" to null, "batchNumber" to "L-$id",
        "initialQty" to 10.0, "remainingQty" to remaining, "unitCost" to 40.0, "arrivalDate" to arrival, "receiptImageUrl" to null
    )

    fun purchaseOrder(id: String = "po1", status: String = "ISSUED", itemId: String = "p1"): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "supplierId" to "s1", "branchId" to "b1", "orderNumber" to "OC-0007", "status" to status,
        "totalCost" to 400.0, "receiptImageUrl" to null, "receiptNumber" to null, "receivedAt" to null,
        "items" to listOf(mapOf("id" to "l1", "itemId" to itemId, "quantity" to 10.0, "unitCost" to 40.0, "totalCost" to 400.0))
    )

    fun requested(method: String, path: String) = requests.filter { it.method == method && it.path!!.substringBefore('?') == path }
}
