package com.andeva.atelier.mobile.features.team.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.employee.EmployeeService
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceDto
import com.andeva.atelier.mobile.features.team.domain.PendingJustifications
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import com.andeva.atelier.mobile.features.team.domain.TeamRepository
import com.andeva.atelier.mobile.features.team.infrastructure.remote.TeamAttendanceService
import com.andeva.atelier.mobile.features.team.infrastructure.remote.toTeamAttendanceOrNull
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Asistencia de la sede y revisión de descargos, solo en línea (docs/api/hr.md). Un ítem que no se puede mapear
 * hace fallar la lectura que lo trae: ocultar un descargo pendiente sería peor que mostrar un error.
 */
class TeamRepositoryImpl @Inject constructor(
    private val teamAttendanceService: TeamAttendanceService,
    private val employeeService: EmployeeService,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : TeamRepository {

    override suspend fun getBranchDaily(date: LocalDate): Result<List<TeamAttendance>> {
        val branchId = branchIdOrFailure().getOrElse { return Result.failure(it) }
        return coroutineScope {
            val titles = async { jobTitles(branchId) }
            val day = readDay(branchId, date)
            val byMembership = titles.await()
            day.map { items -> items.sortedWith(RECENT_FIRST).map { it.withTitle(byMembership) } }
        }
    }

    override suspend fun getPendingJustifications(days: Int): Result<PendingJustifications> {
        if (days < 1) return Result.failure(IllegalArgumentException("days debe ser al menos 1"))
        val branchId = branchIdOrFailure().getOrElse { return Result.failure(it) }
        val today = BusinessTime.limaDate(clock.instant())
        val dates = (0 until days).map { today.minusDays(it.toLong()) } // del más reciente al más viejo
        val (results, byMembership) = coroutineScope {
            val titles = async { jobTitles(branchId) }
            val perDay = dates.map { date -> async { readDay(branchId, date) } }.awaitAll()
            perDay to titles.await()
        }
        val failures = dates.zip(results).filter { it.second.isFailure }
        if (failures.size == dates.size) return Result.failure(failures.first().second.exceptionOrNull()!!)
        val items = results.flatMap { it.getOrNull().orEmpty().filter { item -> item.awaitsReview }.sortedWith(RECENT_FIRST) }
        return Result.success(
            PendingJustifications(items.map { it.withTitle(byMembership) }, failures.map { it.first })
        )
    }

    /** El `history` cubre los últimos 30 días de Lima (hoy−29 … hoy); el push no trae la fecha del registro. */
    override suspend fun getCollaboratorAttendance(membershipId: String, attendanceId: String): Result<TeamAttendance?> {
        val today = BusinessTime.limaDate(clock.instant())
        return safely {
            apiCall(
                gson,
                { teamAttendanceService.history(membershipId, today.minusDays(HISTORY_DAYS - 1).toString(), today.toString()) }
            ) { it }
        }
            .mapCatching { dtos -> dtos.toTeam().firstOrNull { it.id == attendanceId } }
    }

    override suspend fun approveJustification(id: String): Result<TeamAttendance> =
        safely { apiCall(gson, { teamAttendanceService.approveJustification(id) }) { it } }.mapCatching { it.toTeamOrThrow() }

    override suspend fun rejectJustification(id: String): Result<TeamAttendance> =
        safely { apiCall(gson, { teamAttendanceService.rejectJustification(id) }) { it } }.mapCatching { it.toTeamOrThrow() }

    private suspend fun branchIdOrFailure(): Result<String> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        val branchId = session.branchId ?: return Result.failure(MissingBranchException())
        return Result.success(branchId)
    }

    private suspend fun readDay(branchId: String, date: LocalDate): Result<List<TeamAttendance>> =
        safely { apiCall(gson, { teamAttendanceService.branchDaily(branchId, date.toString()) }) { it } }
            .mapCatching { it.toTeam() }

    /** `membershipId → jobTitle`. Es decoración: sin `iam:members:read` no se pide, y si falla queda vacío. */
    private suspend fun jobTitles(branchId: String): Map<String, String> {
        val session = sessionManager.getSession() ?: return emptyMap()
        if (!session.has(Permission.MEMBERS_READ)) return emptyMap()
        return safely { apiCall(gson, { employeeService.getByBranch(branchId) }) { profiles ->
            profiles.mapNotNull { profile ->
                val member = profile.membershipId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = profile.jobTitle?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                member to title
            }.toMap()
        } }.getOrDefault(emptyMap())
    }

    /** Una respuesta que Gson no puede leer (RuntimeException) llega como fallo, no como excepción. */
    private suspend fun <T> safely(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun TeamAttendance.withTitle(titles: Map<String, String>) = copy(jobTitle = titles[membershipId])

    private fun List<AttendanceDto>.toTeam(): List<TeamAttendance> = map { it.toTeamOrThrow() }

    private fun AttendanceDto.toTeamOrThrow(): TeamAttendance =
        toTeamAttendanceOrNull() ?: throw IllegalStateException("Registro de asistencia inválido en la respuesta del servidor (id=$id)")

    private companion object {
        const val HISTORY_DAYS = 30L

        /** Entrada más reciente primero; los registros sin entrada (faltas) al final. */
        val RECENT_FIRST: Comparator<TeamAttendance> =
            compareBy<TeamAttendance, Instant?>(nullsFirst()) { it.clockIn }.reversed()
    }
}
