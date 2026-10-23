package com.andeva.atelier.mobile.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushRouterTest {

    private fun route(type: String, vararg extra: Pair<String, String>) =
        PushRouter.route(mapOf("type" to type) + extra)

    private val task = arrayOf("workOrderId" to "wo1", "taskId" to "t1")

    @Test
    fun `los diez tipos de la tabla abren su pantalla`() {
        assertEquals(PushDestination.TaskDetail("wo1", "t1"), route("TASK_ASSIGNED", *task))
        assertEquals(PushDestination.TaskDetail("wo1", "t1"), route("TASK_PAUSED", *task))
        assertEquals(PushDestination.TaskDetail("wo1", "t1"), route("TASK_RESUMED", *task))
        assertEquals(PushDestination.TaskSuspensionReview("wo1", "t1"), route("TASK_SUSPENSION_REQUESTED", *task))
        assertEquals(PushDestination.TaskProducts("wo1", "t1"), route("TASK_PART_CHARGED", *task))
        assertEquals(PushDestination.WorkOrderDetail("wo1"), route("WORK_ORDER_COMPLETED", "workOrderId" to "wo1"))
        assertEquals(
            PushDestination.LowStockAlerts("i1"),
            route("LOW_STOCK", "itemId" to "i1", "currentStock" to "9", "minimumStock" to "10")
        )
        assertEquals(
            PushDestination.CollaboratorAttendance("a1", "m1"),
            route("OVERTIME_RECORDED", "attendanceId" to "a1", "membershipId" to "m1", "overtimeMinutes" to "45")
        )
        assertEquals(PushDestination.MyAttendance("a1"), route("JUSTIFICATION_REVIEWED", "attendanceId" to "a1", "verdict" to "APPROVED"))
        assertEquals(PushDestination.ClockIn("s1"), route("SHIFT_REMINDER", "shiftId" to "s1", "startTime" to "08:00:00"))
    }

    @Test
    fun `tipo desconocido o sin ids se ignora sin fallar`() {
        assertNull(route("NEW_TYPE_FROM_BACKEND", *task))
        assertNull(route("TASK_ASSIGNED", "workOrderId" to "wo1"))
        assertNull(PushRouter.route(emptyMap()))
    }

    @Test
    fun `cada destino va a su canal`() {
        assertEquals(PushChannel.TASKS, PushRouter.channelFor(PushDestination.WorkOrderDetail("wo1")))
        assertEquals(PushChannel.INVENTORY, PushRouter.channelFor(PushDestination.LowStockAlerts("i1")))
        assertEquals(PushChannel.ATTENDANCE, PushRouter.channelFor(PushDestination.ClockIn("s1")))
    }
}
