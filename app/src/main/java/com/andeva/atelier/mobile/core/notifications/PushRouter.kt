package com.andeva.atelier.mobile.core.notifications

/** Pantalla que abre cada push (docs/04-mobile-integration.md §2). Las features la convierten en su ruta. */
sealed interface PushDestination {
    data class TaskDetail(val workOrderId: String, val taskId: String) : PushDestination
    data class TaskSuspensionReview(val workOrderId: String, val taskId: String) : PushDestination
    data class TaskProducts(val workOrderId: String, val taskId: String) : PushDestination
    data class WorkOrderDetail(val workOrderId: String) : PushDestination
    data class LowStockAlerts(val itemId: String) : PushDestination
    data class CollaboratorAttendance(val attendanceId: String, val membershipId: String) : PushDestination
    data class MyAttendance(val attendanceId: String) : PushDestination
    data class ClockIn(val shiftId: String) : PushDestination
}

enum class PushChannel { TASKS, ATTENDANCE, INVENTORY }

object PushRouter {
    const val KEY_TYPE = "type"

    /**
     * `data.type` + ids → destino. Un tipo desconocido o sin sus ids devuelve null sin fallar:
     * el backend puede agregar tipos nuevos.
     */
    fun route(data: Map<String, String?>): PushDestination? {
        fun id(key: String) = data[key]?.takeIf { it.isNotBlank() }
        val workOrderId = id("workOrderId")
        val taskId = id("taskId")
        return when (data[KEY_TYPE]) {
            "TASK_ASSIGNED", "TASK_PAUSED", "TASK_RESUMED" ->
                if (workOrderId != null && taskId != null) PushDestination.TaskDetail(workOrderId, taskId) else null
            "TASK_SUSPENSION_REQUESTED" ->
                if (workOrderId != null && taskId != null) PushDestination.TaskSuspensionReview(workOrderId, taskId) else null
            "TASK_PART_CHARGED" ->
                if (workOrderId != null && taskId != null) PushDestination.TaskProducts(workOrderId, taskId) else null
            "WORK_ORDER_COMPLETED" -> workOrderId?.let { PushDestination.WorkOrderDetail(it) }
            "LOW_STOCK" -> id("itemId")?.let { PushDestination.LowStockAlerts(it) }
            "OVERTIME_RECORDED" -> {
                val attendanceId = id("attendanceId")
                val membershipId = id("membershipId")
                if (attendanceId != null && membershipId != null) PushDestination.CollaboratorAttendance(attendanceId, membershipId) else null
            }
            "JUSTIFICATION_REVIEWED" -> id("attendanceId")?.let { PushDestination.MyAttendance(it) }
            "SHIFT_REMINDER" -> id("shiftId")?.let { PushDestination.ClockIn(it) }
            else -> null
        }
    }

    fun channelFor(destination: PushDestination): PushChannel = when (destination) {
        is PushDestination.TaskDetail,
        is PushDestination.TaskSuspensionReview,
        is PushDestination.TaskProducts,
        is PushDestination.WorkOrderDetail -> PushChannel.TASKS
        is PushDestination.LowStockAlerts -> PushChannel.INVENTORY
        is PushDestination.CollaboratorAttendance,
        is PushDestination.MyAttendance,
        is PushDestination.ClockIn -> PushChannel.ATTENDANCE
    }
}
