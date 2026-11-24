package com.andeva.atelier.mobile.features.main.presentation.navigation

import com.andeva.atelier.mobile.core.domain.Plan
import com.andeva.atelier.mobile.core.domain.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que la barra inferior y su acción circular se decidan estrictamente por permiso atómico
 * para los 8 roles de fábrica (Segmento 1 y Segmento 2) y roles personalizados, sin bloqueos.
 */
class MainNavigationTest {

    private fun sessionWith(vararg perms: String, iot: Boolean = true, ai: Boolean = true) = Session(
        userId = "u1",
        email = "staff@atelier.pe",
        fullName = "Staff",
        tenantId = "t1",
        tenantName = "Taller",
        membershipId = "m1",
        permissions = perms.toSet(),
        plan = Plan(
            tier = if (iot) "PRO" else "GO",
            status = "ACTIVE",
            accessGranted = true,
            iotTelemetryEnabled = iot,
            aiDiagnosticsEnabled = ai
        ),
        branchId = "b1",
        shiftId = "s1"
    )

    @Test
    fun ownerAndAdminKeepSegment1TabsAndReceiveAction() {
        val owner = sessionWith(
            "operations:work_orders:read", "operations:work_orders:create",
            "hr:attendance:audit_all", "invoicing:cashflow:export_pdf"
        )
        assertTrue(owner.prefersOperationalDashboard())
        assertEquals(listOf(MainTab.Today, MainTab.Orders, MainTab.Team), MainTab.tabsFor(owner))
        assertTrue(MainTab.hasModules(owner))
        assertEquals(BarPrimaryAction.RECEIVE_VEHICLE, BarPrimaryAction.forSession(owner))
    }

    @Test
    fun chiefMechanicGetsTodayOrdersTeamAndModules() {
        val chief = sessionWith(
            "hr:attendance:clock_in", "hr:attendance:audit_all",
            "operations:work_orders:read", "operations:work_orders:create",
            "operations:tasks:read", "operations:tasks:track_time", "operations:bays:reassign"
        )
        assertTrue(chief.prefersOperationalDashboard())
        assertEquals(listOf(MainTab.Today, MainTab.Orders, MainTab.Team), MainTab.tabsFor(chief))
        assertEquals(BarPrimaryAction.RECEIVE_VEHICLE, BarPrimaryAction.forSession(chief))
    }

    @Test
    fun serviceAdvisorGetsTodayReceptionOrdersAndReceiveAction() {
        val advisor = sessionWith(
            "hr:attendance:clock_in", "crm:customers:read", "crm:appointments:read",
            "operations:work_orders:read", "operations:work_orders:create", "operations:quotations:approve"
        )
        assertTrue(advisor.prefersOperationalDashboard())
        assertEquals(listOf(MainTab.Today, MainTab.Reception, MainTab.Orders), MainTab.tabsFor(advisor))
        assertEquals(BarPrimaryAction.RECEIVE_VEHICLE, BarPrimaryAction.forSession(advisor))
    }

    @Test
    fun mechanicWithTelemetryGetsShiftTasksObdWithoutDuplicateTasksAction() {
        val mech = sessionWith(
            "hr:attendance:clock_in", "hr:attendance:read_own",
            "operations:tasks:read", "operations:tasks:track_time",
            "operations:bays:read", "iot:telemetry:ingest",
            iot = true
        )
        assertFalse(mech.prefersOperationalDashboard())
        assertEquals(listOf(MainTab.Shift, MainTab.Tasks, MainTab.Obd), MainTab.tabsFor(mech))
        assertTrue(MainTab.hasModules(mech))
        assertEquals(BarPrimaryAction.NONE, BarPrimaryAction.forSession(mech))
    }

    @Test
    fun mechanicOnGoPlanFallsBackFromObdToBays() {
        val mechGo = sessionWith(
            "hr:attendance:clock_in", "operations:tasks:read", "operations:tasks:track_time",
            "operations:bays:read", "iot:telemetry:ingest",
            iot = false
        )
        assertEquals(listOf(MainTab.Shift, MainTab.Tasks, MainTab.Bays), MainTab.tabsFor(mechGo))
    }

    @Test
    fun receptionistNeverLockedOutAndGetsShiftReceptionOrders() {
        val recep = sessionWith(
            "hr:attendance:clock_in", "crm:customers:read", "crm:customers:create",
            "crm:appointments:read", "crm:appointments:create", "operations:work_orders:read"
        )
        assertFalse(recep.prefersOperationalDashboard())
        assertEquals(listOf(MainTab.Shift, MainTab.Reception, MainTab.Orders), MainTab.tabsFor(recep))
        assertTrue(MainTab.hasModules(recep))
        assertEquals(BarPrimaryAction.NEW_APPOINTMENT, BarPrimaryAction.forSession(recep))
    }

    @Test
    fun inventoryManagerNeverLockedOutAndGetsShiftInventoryPurchases() {
        val inv = sessionWith(
            "hr:attendance:clock_in", "inventory:parts:read", "inventory:parts:manage",
            "inventory:purchase_orders:read", "inventory:purchase_orders:create",
            "inventory:batches:receive", "operations:work_orders:read"
        )
        assertFalse(inv.prefersOperationalDashboard())
        assertEquals(listOf(MainTab.Shift, MainTab.Inventory, MainTab.Purchases), MainTab.tabsFor(inv))
        assertTrue(MainTab.hasModules(inv))
        assertEquals(BarPrimaryAction.SCAN_SKU, BarPrimaryAction.forSession(inv))
    }

    @Test
    fun cashierNeverLockedOutAndGetsShiftOrdersCash() {
        val cashier = sessionWith(
            "hr:attendance:clock_in", "operations:work_orders:read",
            "invoicing:invoices:read", "invoicing:invoices:issue_sunat", "invoicing:payments:create"
        )
        assertFalse(cashier.prefersOperationalDashboard())
        assertEquals(listOf(MainTab.Shift, MainTab.Orders, MainTab.Cash), MainTab.tabsFor(cashier))
        assertTrue(MainTab.hasModules(cashier))
        assertEquals(BarPrimaryAction.COLLECT_PAYMENT, BarPrimaryAction.forSession(cashier))
    }

    @Test
    fun minimalCustomRoleAlwaysStartsWithVisibleShiftTabAndModules() {
        val minimal = sessionWith("hr:attendance:read_own")
        assertFalse(minimal.prefersOperationalDashboard())
        assertEquals(listOf(MainTab.Shift), MainTab.tabsFor(minimal))
        assertTrue(MainTab.hasModules(minimal))
        assertEquals(BarPrimaryAction.NONE, BarPrimaryAction.forSession(minimal))
    }
}
