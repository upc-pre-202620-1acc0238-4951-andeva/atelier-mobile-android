package com.andeva.atelier.mobile.features.cashier.infrastructure

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.TenantSummaryDto
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.cashier.domain.CustomerFiscalInfo
import com.andeva.atelier.mobile.features.cashier.domain.FiscalDocumentType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherItemType
import com.andeva.atelier.mobile.features.cashier.domain.VoucherLineDraft
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.CashierService
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.HandoverService
import com.andeva.atelier.mobile.features.cashier.infrastructure.repository.CashierRepositoryImpl
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Backend simulado (MockWebServer) + Room en memoria + sesión en memoria para los tests de `cashier`. Hora fija:
 * 2026-10-24 09:00 de Lima. El cliente HTTP lee con 1 s de tope para que un `NO_RESPONSE` falle rápido.
 */
class CashierTestEnv {
    val gson = Gson()
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)
    private val server = MockWebServer()

    /** "METHOD /ruta" sin query → respuesta. Lo demás, 404. */
    val responses = ConcurrentHashMap<String, MockResponse>()

    /** Respuestas en orden para una ruta ("METHOD /ruta"); al agotarse se usa [responses]. */
    val sequences = ConcurrentHashMap<String, ArrayDeque<MockResponse>>()
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    /** Si devuelve una respuesta, gana sobre [sequences] y [responses] (respuestas que dependen del orden). */
    @Volatile
    var handler: ((RecordedRequest) -> MockResponse?)? = null

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
                val key = "${request.method} ${request.path!!.substringBefore('?')}"
                return handler?.invoke(request)
                    ?: sequences[key]?.let { synchronized(it) { it.removeFirstOrNull() } }
                    ?: responses[key]
                    ?: error(404, "NOT_FOUND", "No encontrado")
            }
        }
        server.start()
        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor(sessionManager))
                    .readTimeout(1, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun stop() {
        server.shutdown()
        db.close()
    }

    /** Servicio Retrofit contra el backend simulado, para tests de otras features. */
    fun <T> api(service: Class<T>): T = retrofit.create(service)

    fun repository(workOrderDao: WorkOrderDao = db.workOrderDao()) = CashierRepositoryImpl(
        cashierService = retrofit.create(CashierService::class.java),
        handoverService = retrofit.create(HandoverService::class.java),
        workOrderDao = workOrderDao,
        pdfDownloader = PdfDownloader(context, gson),
        voucherDao = db.voucherDao(),
        sessionManager = sessionManager,
        gson = gson,
        clock = clock
    )

    /** Cajero con sede `b1` (o sin sede si [branchId] es null). */
    suspend fun signIn(
        permissions: List<Permission> = listOf(Permission.INVOICES_READ, Permission.INVOICES_ISSUE_SUNAT),
        branchId: String? = "b1"
    ) {
        sessionManager.save(
            AuthenticatedUserDto(
                userId = "u1", email = "cajero@taller.pe", fullName = "Cajero", token = "jwt", tokenType = "Bearer",
                activeTenant = TenantSummaryDto("t1", "Taller Suárez", null), permissions = permissions.map { it.value },
                refreshToken = "refresh", membershipId = "m1", plan = null
            )
        )
        sessionManager.updateAssignment(branchId, null)
    }

    fun json(body: Any, code: Int = 200): MockResponse = MockResponse().setResponseCode(code).setBody(gson.toJson(body))

    fun error(code: Int, errorCode: String, message: String): MockResponse =
        MockResponse().setResponseCode(code).setBody("""{"code":"$errorCode","message":"$message","details":[]}""")

    fun enqueue(key: String, vararg answers: MockResponse) {
        sequences.getOrPut(key) { ArrayDeque() }.let { queue -> synchronized(queue) { queue.addAll(answers) } }
    }

    fun requested(method: String, path: String) = requests.filter { it.method == method && it.path!!.substringBefore('?') == path }

    fun voucher(
        id: String = "v1",
        workOrderId: String? = "wo1",
        type: String = "03",
        status: String = "ACCEPTED_SUNAT",
        total: Double = 118.0,
        paid: Double = 0.0
    ): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "branchId" to "b1", "customerId" to "c1", "workOrderId" to workOrderId,
        "voucherType" to type, "serie" to if (type == "01") "F001" else "B001", "number" to 7,
        "subtotal" to 100.0, "igvAmount" to 18.0, "totalAmount" to total, "currency" to "PEN", "status" to status,
        "customerInfo" to mapOf("taxId" to "12345678", "legalName" to "Juan Pérez", "fiscalAddress" to null, "documentType" to "DNI"),
        "digitalReceipts" to mapOf("pdfUrl" to "https://cdn/b001-7.pdf", "xmlUrl" to null, "cdrUrl" to null),
        "sunatResponse" to mapOf("responseCode" to "0", "description" to "Aceptado"),
        "lines" to listOf(
            mapOf(
                "id" to "l1", "itemId" to "s1", "itemType" to "SERVICE", "description" to "Frenos", "quantity" to 1,
                "unitValue" to 100.0, "unitPrice" to 118.0, "igvAmount" to 18.0, "totalLine" to 118.0
            )
        ),
        "payments" to emptyList<Any>(), "issuedAt" to "2026-10-24T13:30:00Z",
        "paidAmount" to paid, "pendingBalance" to total - paid, "paymentStatus" to if (paid > 0) "PARTIAL" else "PENDING"
    )

    fun workOrder(id: String = "wo1", status: String = "COMPLETED"): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "internalNumber" to 15, "vehicleId" to "v1", "customerId" to "c1",
        "branchId" to "b1", "currentBayId" to null, "bayName" to null, "status" to status,
        "totalAmount" to 118.0, "currency" to "PEN", "createdAt" to "2026-10-24T13:00:00Z"
    )

    fun payment(id: String = "p1", amount: Double = 50.0, method: String = "CASH"): Map<String, Any?> = mapOf(
        "id" to id, "voucherId" to "v1", "amount" to amount, "currency" to "PEN", "paymentMethod" to method,
        "transactionReference" to null, "status" to "COMPLETED", "paidAt" to "2026-10-24T13:45:00Z"
    )

    fun draft(type: VoucherType = VoucherType.BOLETA) = VoucherDraft(
        workOrderId = "wo1",
        customerId = "c1",
        type = type,
        customerInfo = CustomerFiscalInfo(FiscalDocumentType.DNI, "12345678", "Juan Pérez", null),
        currency = "PEN",
        lines = listOf(VoucherLineDraft(VoucherItemType.SERVICE, "s1", "Frenos", BigDecimal.ONE, BigDecimal("118.00")))
    )
}
