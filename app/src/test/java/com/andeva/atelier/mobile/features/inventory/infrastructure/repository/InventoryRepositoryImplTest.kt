package com.andeva.atelier.mobile.features.inventory.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderLineInput
import com.andeva.atelier.mobile.features.inventory.domain.PurchaseOrderStatus
import com.andeva.atelier.mobile.features.inventory.infrastructure.InventoryTestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InventoryRepositoryImplTest {

    private val env = InventoryTestEnv()
    private lateinit var repository: InventoryRepositoryImpl

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    private fun photo() = kotlin.io.path.createTempFile(suffix = ".jpg").toFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }

    @Test
    fun `el catalogo se recarga pagina por pagina y respeta la vigencia de una hora`() = runTest {
        env.signIn()
        env.pages[0] = env.json(List(100) { env.part("p$it", "Repuesto $it", "SKU-$it") })
        env.pages[1] = env.json(listOf(env.part("p100", "Filtro", "FLT-100")))

        repository.refreshCatalog().getOrThrow()
        repository.refreshCatalog().getOrThrow()

        assertEquals(101, repository.observeCatalog(null).first().size)
        assertEquals(2, env.requested("GET", "/api/v1/inventory/items").size)
        assertEquals("100", env.requested("GET", "/api/v1/inventory/items").first().requestUrl!!.queryParameter("size"))
    }

    @Test
    fun `si una pagina falla se conserva el catalogo anterior`() = runTest {
        env.signIn()
        env.pages[0] = env.json(listOf(env.part()))
        repository.refreshCatalog().getOrThrow()
        env.pages[0] = env.json(List(100) { env.part("p$it", sku = "SKU-$it") })
        env.pages[1] = env.disconnect()

        assertTrue(repository.refreshCatalog(force = true).exceptionOrNull() is IOException)
        assertEquals(listOf("p1"), repository.observeCatalog(null).first().map { it.id })
    }

    @Test
    fun `buscar en linea guarda en cache y sin red busca por nombre o sku`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/items"] = env.json(listOf(env.part(), env.part("p2", "Filtro de aceite", "FLT-002")))
        val online = repository.searchParts(" freno ", ItemCategory.BRAKES).getOrThrow()
        assertEquals(2, online.size)
        val request = env.requested("GET", "/api/v1/inventory/items").single().requestUrl!!
        assertEquals("freno", request.queryParameter("search"))
        assertEquals("BRAKES", request.queryParameter("category"))

        env.responses["GET /api/v1/inventory/items"] = env.disconnect()
        assertEquals(listOf("p2"), repository.searchParts("flt", null).getOrThrow().map { it.id })
        assertEquals(listOf("p1"), repository.searchParts("pastillas", null).getOrThrow().map { it.id })
    }

    @Test
    fun `sku en mayusculas, 404 es null y sin red usa la cache`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/items/by-sku/BRK-001"] = env.json(env.part())

        assertEquals("p1", repository.findBySku(" brk-001 ").getOrThrow()!!.id)
        assertNull(repository.findBySku("XXX-999").getOrThrow())
        env.responses["GET /api/v1/inventory/items/by-sku/BRK-001"] = env.disconnect()
        assertEquals("p1", repository.findBySku("BRK-001").getOrThrow()!!.id)
    }

    @Test
    fun `los lotes quedan en orden FIFO con los agotados marcados`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/items/p1/batches"] = env.json(
            listOf(
                env.batch("b3", "2026-10-20T10:00:00Z", 10.0),
                env.batch("b2", "2026-09-15T10:00:00Z", 2.0),
                env.batch("b1", "2026-08-01T10:00:00Z", 0.0)
            )
        )

        repository.refreshBatches("p1").getOrThrow()

        val batches = repository.observeBatches("p1").first()
        assertEquals(listOf("b1", "b2", "b3"), batches.map { it.id })
        assertFalse(batches.first().isActive)
        assertTrue(batches[1].isActive)
    }

    @Test
    fun `sin batches read no se piden lotes y el detalle va sin ellos`() = runTest {
        env.signIn(permissions = listOf(Permission.PARTS_READ))
        env.responses["GET /api/v1/inventory/items/p1"] = env.json(env.part() + ("batches" to listOf(env.batch("b1", "2026-08-01T10:00:00Z", 3.0))))

        repository.refreshBatches("p1").getOrThrow()
        val detail = repository.getPartDetail("p1").getOrThrow()

        assertTrue(env.requested("GET", "/api/v1/inventory/items/p1/batches").isEmpty())
        assertTrue(detail.batches.isEmpty())
        assertTrue(repository.observeBatches("p1").first().isEmpty())
    }

    @Test
    fun `el detalle trae los lotes en FIFO y sin red sale de la cache`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/items/p1"] = env.json(
            env.part() + ("batches" to listOf(env.batch("b2", "2026-09-15T10:00:00Z", 2.0), env.batch("b1", "2026-08-01T10:00:00Z", 5.0)))
        )

        val online = repository.getPartDetail("p1").getOrThrow()
        assertEquals(listOf("b1", "b2"), online.batches.map { it.id })
        assertEquals(BigDecimal("10.0"), online.batches.first().initialQty)

        env.responses["GET /api/v1/inventory/items/p1"] = env.disconnect()
        val offline = repository.getPartDetail("p1").getOrThrow()
        assertEquals(listOf("b1", "b2"), offline.batches.map { it.id })
        assertNull(offline.batches.first().initialQty)
    }

    @Test
    fun `alertas en linea y sin red derivadas de la cache comparando numeros`() = runTest {
        env.signIn()
        env.pages[0] = env.json(listOf(env.part("p1", stock = 9.0, minimum = 10.0), env.part("p2", sku = "FLT-002", stock = 10.0, minimum = 9.0)))
        repository.refreshCatalog().getOrThrow()
        env.responses["GET /api/v1/inventory/alerts"] = env.json(
            listOf(mapOf("itemId" to "p1", "name" to "Pastillas", "sku" to "BRK-001", "category" to "BRAKES", "currentStock" to 9, "minimumStock" to 10, "deficit" to 1))
        )

        assertEquals(BigDecimal.ONE, repository.getStockAlerts().getOrThrow().single().deficit)
        env.responses["GET /api/v1/inventory/alerts"] = env.disconnect()

        val offline = repository.getStockAlerts().getOrThrow().single()
        assertEquals("p1", offline.itemId)
        assertEquals(0, BigDecimal.ONE.compareTo(offline.deficit))
    }

    @Test
    fun `recibir sube el comprobante, envia el cuerpo y la alerta sale de la bandeja`() = runTest {
        env.signIn()
        env.pages[0] = env.json(listOf(env.part(stock = 2.0, minimum = 5.0)))
        repository.refreshCatalog().getOrThrow()
        assertEquals(1, repository.observeLowStock().first().size)
        env.responses["GET /api/v1/inventory/purchase-orders/po1"] = env.json(env.purchaseOrder())
        env.responses["PUT /api/v1/inventory/purchase-orders/po1/receive"] = env.json(env.purchaseOrder(status = "RECEIVED"))
        env.responses["GET /api/v1/inventory/items/p1/batches"] = env.json(listOf(env.batch("b9", "2026-10-24T14:00:00Z", 10.0)))
        env.pages[0] = env.json(listOf(env.part(stock = 12.0, minimum = 5.0)))

        val received = repository.receivePurchaseOrder("po1", " F001-123 ", photo(), null).getOrThrow()

        assertEquals(PurchaseOrderStatus.RECEIVED, received.status)
        assertTrue(env.uploadedPaths.single().startsWith("t1/purchase-orders/po1/receipt/"))
        val body = env.requested("PUT", "/api/v1/inventory/purchase-orders/po1/receive").single().body.readUtf8()
        assertTrue(body.contains("\"receiptNumber\":\"F001-123\""))
        assertTrue(body.contains("t1/purchase-orders/po1/receipt/"))
        assertTrue(repository.observeLowStock().first().isEmpty())
        assertEquals(listOf("b9"), repository.observeBatches("p1").first().map { it.id })
    }

    @Test
    fun `una orden ya recibida responde exito sin subir la foto`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/purchase-orders/po1"] = env.json(env.purchaseOrder(status = "RECEIVED"))

        assertEquals(PurchaseOrderStatus.RECEIVED, repository.receivePurchaseOrder("po1", "F001-1", photo(), null).getOrThrow().status)
        assertTrue(env.uploadedPaths.isEmpty())
        assertTrue(env.requested("PUT", "/api/v1/inventory/purchase-orders/po1/receive").isEmpty())
    }

    @Test
    fun `un 400 real al recibir devuelve el message`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/purchase-orders/po1"] = env.json(env.purchaseOrder())
        env.responses["PUT /api/v1/inventory/purchase-orders/po1/receive"] = env.error(400, "BAD_REQUEST", "Missing receipt documents")

        val error = repository.receivePurchaseOrder("po1", "F001-1", photo(), null).exceptionOrNull() as ApiException

        assertEquals("Missing receipt documents", error.message)
        assertEquals(2, env.requested("GET", "/api/v1/inventory/purchase-orders/po1").size)
    }

    @Test
    fun `un 400 tras otra recepcion simultanea cuenta como exito`() = runTest {
        env.signIn()
        env.pages[0] = env.json(listOf(env.part()))
        env.sequences["GET /api/v1/inventory/purchase-orders/po1"] = ArrayDeque(listOf(env.json(env.purchaseOrder())))
        env.responses["GET /api/v1/inventory/purchase-orders/po1"] = env.json(env.purchaseOrder(status = "RECEIVED"))
        env.responses["PUT /api/v1/inventory/purchase-orders/po1/receive"] = env.error(400, "BAD_REQUEST", "Must be in ISSUED status")

        assertEquals(PurchaseOrderStatus.RECEIVED, repository.receivePurchaseOrder("po1", "F001-1", photo(), null).getOrThrow().status)
    }

    @Test
    fun `un corte en el PUT con la orden ya recibida es exito`() = runTest {
        env.signIn()
        env.pages[0] = env.json(listOf(env.part()))
        env.sequences["GET /api/v1/inventory/purchase-orders/po1"] = ArrayDeque(listOf(env.json(env.purchaseOrder())))
        env.responses["GET /api/v1/inventory/purchase-orders/po1"] = env.json(env.purchaseOrder(status = "RECEIVED"))
        env.responses["PUT /api/v1/inventory/purchase-orders/po1/receive"] = env.disconnect()

        val received = repository.receivePurchaseOrder("po1", "F001-1", photo(), null).getOrThrow()

        assertEquals(PurchaseOrderStatus.RECEIVED, received.status)
        assertEquals(1, env.uploadedPaths.size)
    }

    @Test
    fun `una orden en borrador no se recibe y sin red no se sube nada`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/purchase-orders/po1"] = env.json(env.purchaseOrder(status = "DRAFT"))
        assertTrue(repository.receivePurchaseOrder("po1", "F001-1", photo(), null).isFailure)

        env.responses["GET /api/v1/inventory/purchase-orders/po1"] = env.disconnect()
        assertTrue(repository.receivePurchaseOrder("po1", "F001-1", photo(), null).exceptionOrNull() is IOException)
        assertTrue(env.uploadedPaths.isEmpty())
    }

    @Test
    fun `valorizacion en PEN y ordenes emitidas`() = runTest {
        env.signIn()
        env.responses["GET /api/v1/inventory/items/valuation"] = env.json(mapOf("valuation" to 1520.5))
        env.responses["GET /api/v1/inventory/purchase-orders"] = env.json(listOf(env.purchaseOrder()))

        assertEquals(BigDecimal("1520.5"), repository.getValuation().getOrThrow().total.amount)
        val orders = repository.getPurchaseOrders(PurchaseOrderStatus.ISSUED).getOrThrow()
        assertTrue(orders.single().canBeReceived)
        assertEquals("ISSUED", env.requested("GET", "/api/v1/inventory/purchase-orders").single().requestUrl!!.queryParameter("status"))
    }

    @Test
    fun `crear y editar repuesto actualiza la cache local de Room`() = runTest {
        env.signIn(listOf(Permission.PARTS_READ, Permission.PARTS_MANAGE))
        env.responses["POST /api/v1/inventory/items"] = env.json(env.part("p9", "Bujía NGK", "ngk-01", stock = 0.0), 201)
        env.responses["PUT /api/v1/inventory/items/p9"] = env.json(env.part("p9", "Bujía NGK Iridium", "NGK-01", stock = 0.0))

        val created = repository.createPart("Bujía NGK", "ngk-01", ItemCategory.ENGINE, BigDecimal("42.00"), BigDecimal("4")).getOrThrow()
        assertEquals("p9", created.id)
        assertEquals("Bujía NGK", repository.observePart("p9").first()!!.name)

        val updated = repository.updatePart("p9", "Bujía NGK Iridium", ItemCategory.ENGINE, BigDecimal("48.00"), BigDecimal("6"), PartStatus.ACTIVE).getOrThrow()
        assertEquals("Bujía NGK Iridium", updated.name)
        assertEquals("Bujía NGK Iridium", repository.observePart("p9").first()!!.name)
    }

    @Test
    fun `proveedores listar crear y editar contra el backend`() = runTest {
        env.signIn(listOf(Permission.SUPPLIERS_MANAGE))
        val supplierJson = mapOf(
            "id" to "s1", "tenantId" to "t1", "businessName" to "AutoPartes SAC", "taxId" to "20512345678",
            "contactName" to "Luis", "phone" to "999888777", "email" to "ventas@autopartes.pe", "address" to "Lima", "active" to true
        )
        env.responses["GET /api/v1/inventory/suppliers"] = env.json(listOf(supplierJson))
        env.responses["POST /api/v1/inventory/suppliers"] = env.json(supplierJson, 201)
        env.responses["PUT /api/v1/inventory/suppliers/s1"] = env.json(supplierJson + ("active" to false))

        assertEquals(1, repository.getSuppliers("Auto").getOrThrow().size)
        assertEquals("AutoPartes SAC", repository.createSupplier("AutoPartes SAC", "20512345678").getOrThrow().businessName)
        assertFalse(repository.updateSupplier("s1", active = false).getOrThrow().active)
    }

    @Test
    fun `ciclo completo de orden de compra y lotes directos FIFO`() = runTest {
        env.signIn(
            listOf(
                Permission.PARTS_READ, Permission.BATCHES_READ, Permission.PURCHASE_ORDERS_READ,
                Permission.PURCHASE_ORDERS_CREATE, Permission.BATCHES_RECEIVE,
                Permission.BATCHES_DISPATCH_FIFO, Permission.BATCHES_RESTORE
            )
        )
        assertTrue(repository.createPurchaseOrder("s1", emptyList()).exceptionOrNull() is MissingBranchException)

        env.sessionManager.selectBranch("b1")
        env.responses["POST /api/v1/inventory/purchase-orders"] = env.json(env.purchaseOrder("po2", "DRAFT"), 201)
        env.responses["POST /api/v1/inventory/purchase-orders/po2/items"] = env.json(env.purchaseOrder("po2", "DRAFT"))
        env.responses["PUT /api/v1/inventory/purchase-orders/po2/issue"] = env.json(env.purchaseOrder("po2", "ISSUED"))
        env.responses["PUT /api/v1/inventory/purchase-orders/po2/cancel"] = env.json(env.purchaseOrder("po2", "CANCELED"))

        val draft = repository.createPurchaseOrder("s1", listOf(PurchaseOrderLineInput("p1", BigDecimal("4"), BigDecimal("25.00")))).getOrThrow()
        assertEquals(PurchaseOrderStatus.DRAFT, draft.status)
        assertTrue(env.requested("POST", "/api/v1/inventory/purchase-orders").single().body.readUtf8().contains("\"branchId\":\"b1\""))

        assertEquals(PurchaseOrderStatus.DRAFT, repository.addPurchaseOrderItem("po2", "p1", BigDecimal("2"), BigDecimal("30.00")).getOrThrow().status)
        assertEquals(PurchaseOrderStatus.ISSUED, repository.issuePurchaseOrder("po2").getOrThrow().status)
        assertEquals(PurchaseOrderStatus.CANCELED, repository.cancelPurchaseOrder("po2", "Proveedor sin stock").getOrThrow().status)

        env.responses["GET /api/v1/inventory/items/p1"] = env.json(env.part())
        env.responses["POST /api/v1/inventory/batches/receive"] = env.json(env.batch("b10", "2026-10-24T14:00:00Z", 5.0), 201)
        env.responses["POST /api/v1/inventory/batches/dispatch"] = env.json(
            mapOf(
                "allocationId" to "al1", "allocatedQuantity" to 2.0, "totalCogs" to 80.0,
                "deductions" to listOf(mapOf("batchId" to "b10", "quantityDeducted" to 2.0, "unitCost" to 40.0, "subtotal" to 80.0))
            )
        )
        env.responses["POST /api/v1/inventory/batches/restore"] = env.json(
            mapOf("status" to "RESTORED", "message" to "OK", "batchId" to "b10", "restoredQuantity" to 1.0)
        )

        assertEquals("b10", repository.receiveDirectBatch("p1", "L-10", BigDecimal("5"), BigDecimal("40")).getOrThrow().id)
        assertEquals(0, BigDecimal("2").compareTo(repository.dispatchFifoBatch("p1", BigDecimal("2")).getOrThrow().allocatedQuantity))
        assertEquals(0, BigDecimal("1").compareTo(repository.restoreBatch("p1", BigDecimal("1"), "b10", "Sobrante").getOrThrow().restoredQuantity))
    }
}
