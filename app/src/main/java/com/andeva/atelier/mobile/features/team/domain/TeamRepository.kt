package com.andeva.atelier.mobile.features.team.domain

import java.time.LocalDate

/** Descargos por revisar; [failedDates] son los días cuya consulta falló (resultado parcial). */
data class PendingJustifications(
    val items: List<TeamAttendance>,
    val failedDates: List<LocalDate>
)

/** Asistencia de la sede y revisión de descargos. Solo en línea. */
interface TeamRepository {
    suspend fun getBranchDaily(date: LocalDate): Result<List<TeamAttendance>>

    suspend fun getPendingJustifications(days: Int = 7): Result<PendingJustifications>

    suspend fun getCollaboratorAttendance(membershipId: String, attendanceId: String): Result<TeamAttendance?>

    suspend fun approveJustification(id: String): Result<TeamAttendance>

    suspend fun rejectJustification(id: String): Result<TeamAttendance>
}
