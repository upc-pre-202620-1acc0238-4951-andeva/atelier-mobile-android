package com.andeva.atelier.mobile.features.attendance.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Asistencia propia (US06–US10). Offline-first: red → Room → UI; marcaciones y descargos sin red van a la cola. */
interface AttendanceRepository {
    fun observeToday(): Flow<TodayAttendance>

    /** Últimos 30 días, del más reciente al más antiguo. */
    fun observeHistory(): Flow<List<Attendance>>

    fun observeSchedule(): Flow<WeeklySchedule?>

    /** Perfil laboral, sedes, turno y asistencias. Falla solo si no se pudo leer la asistencia. */
    suspend fun refresh(): Result<Unit>

    suspend fun findBranch(branchId: String): Branch?

    suspend fun clockIn(branchId: String, shiftId: String, location: DeviceLocation): Result<SubmitOutcome>

    suspend fun clockOut(): Result<SubmitOutcome>

    suspend fun submitJustification(id: AttendanceId, reason: String): Result<SubmitOutcome>

    /** Asistencia diaria de la sede (`hr:attendance:audit_all`). */
    suspend fun getBranchDailyAttendance(date: LocalDate? = null): Result<List<BranchAttendanceItem>>

    /** Aprobar o rechazar justificación (`hr:justifications:approve`). */
    suspend fun reviewJustification(id: AttendanceId, approve: Boolean): Result<Attendance>
}
