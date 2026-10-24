package com.andeva.atelier.mobile.features.attendance.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.employee.EmployeeService
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.core.data.sync.PendingOperationQueue
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import com.andeva.atelier.mobile.features.attendance.domain.Branch
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.MissingAssignmentException
import com.andeva.atelier.mobile.features.attendance.domain.SubmitOutcome
import com.andeva.atelier.mobile.features.attendance.domain.TodayAttendance
import com.andeva.atelier.mobile.features.attendance.domain.WeeklySchedule
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.AttendanceDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.BranchDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.ShiftDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceService
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.BranchService
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.ClockInRequestDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.ClockOutRequestDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.JustificationPayload
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.JustifyAttendanceRequestDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.ClockInHandler
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.ClockOutHandler
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.JustificationHandler
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Offline-first (docs/06-offline-sync.md §4 y §6): la UI observa Room; las marcaciones y descargos
 * van directo con red y a la cola `pending_operations` sin ella.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceService: AttendanceService,
    private val branchService: BranchService,
    private val employeeService: EmployeeService,
    private val attendanceDao: AttendanceDao,
    private val shiftDao: ShiftDao,
    private val branchDao: BranchDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val pendingOperationDao: PendingOperationDao,
    private val queue: PendingOperationQueue,
    private val cache: AttendanceCache,
    private val sessionManager: SessionManager,
    private val connectivity: ConnectivityObserver,
    private val gson: Gson,
    private val clock: Clock
) : AttendanceRepository {

    private val membershipId: Flow<String?> = sessionManager.session.map { it?.membershipId }.distinctUntilChanged()

    private fun today(): LocalDate = BusinessTime.limaDate(clock.instant())

    override fun observeToday(): Flow<TodayAttendance> = membershipId.flatMapLatest { id ->
        if (id == null) return@flatMapLatest flowOf(TodayAttendance.NotClockedIn)
        val today = today()
        combine(
            attendanceDao.observeLatest(id),
            pendingOperationDao.observeByDedupeKey(AttendanceDedupeKeys.clockIn(id, today)),
            pendingOperationDao.observeByDedupeKey(AttendanceDedupeKeys.clockOut(id, today))
        ) { latest, clockIn, clockOut -> todayOf(latest?.toDomain(), clockIn, clockOut, today) }
    }

    override fun observeHistory(): Flow<List<Attendance>> = membershipId.flatMapLatest { id ->
        if (id == null) return@flatMapLatest flowOf(emptyList())
        val from = BusinessTime.startOfDay(today().minusDays(HISTORY_DAYS)).toEpochMilli()
        attendanceDao.observeSince(id, from).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeSchedule(): Flow<WeeklySchedule?> = sessionManager.session
        .map { it?.branchId to it?.shiftId }
        .distinctUntilChanged()
        .flatMapLatest { (branchId, shiftId) ->
            if (shiftId == null) return@flatMapLatest flowOf(null)
            val branch = branchId?.let { branchDao.observeById(it) } ?: flowOf(null)
            combine(shiftDao.observeById(shiftId), branch) { shift, branchEntity ->
                shift?.let { WeeklySchedule(branchId, shiftId, it.toDomain(), branchEntity?.toDomain()) }
            }
        }

    /**
     * Perfil laboral (sede y turno pueden haber cambiado, o el login fue sin red), sedes y turno
     * (24 h) y asistencias de los últimos 30 días (siempre). Solo llama a lo que la sesión permite.
     */
    override suspend fun refresh(): Result<Unit> {
        val session = sessionManager.getSession() ?: return Result.failure(MissingAssignmentException())
        val membershipId = session.membershipId ?: return Result.failure(MissingAssignmentException())
        val now = clock.millis()

        if (session.has(Permission.ATTENDANCE_READ_OWN)) refreshAssignment(membershipId)
        val current = sessionManager.getSession() ?: session

        if (current.has(Permission.BRANCHES_READ) && isStale(SCOPE_BRANCHES, now)) refreshBranches(now)
        val shiftId = current.shiftId
        if (shiftId != null && current.has(Permission.SHIFTS_READ) && isStale(shiftScope(shiftId), now)) refreshShift(shiftId, now)

        if (!current.has(Permission.ATTENDANCE_READ_OWN)) return Result.success(Unit)
        val today = today()
        val history = cache.refreshRange(membershipId, today.minusDays(HISTORY_DAYS), today)
        // La jornada activa puede haber empezado ayer (fuera del rango de hoy). Sin jornada: 404 o vacío, se ignora.
        apiCall(gson, { attendanceService.statusToday(membershipId) }) { it }
            .onSuccess { cache.save(it, membershipId) }
        return history.map { }
    }

    override suspend fun findBranch(branchId: String): Branch? = branchDao.findById(branchId)?.toDomain()

    override suspend fun clockIn(branchId: String, shiftId: String, location: DeviceLocation): Result<SubmitOutcome> {
        val membershipId = sessionManager.getSession()?.membershipId ?: return Result.failure(MissingAssignmentException())
        val now = clock.instant()
        val body = ClockInRequestDto(branchId, shiftId, location.latitude, location.longitude)
        return submit(
            membershipId = membershipId,
            online = { attendanceService.clockIn(body) },
            type = ClockInHandler.TYPE,
            payload = body.copy(clockInTime = now.toString()),
            dedupeKey = AttendanceDedupeKeys.clockIn(membershipId, BusinessTime.limaDate(now))
        )
    }

    override suspend fun clockOut(): Result<SubmitOutcome> {
        val membershipId = sessionManager.getSession()?.membershipId ?: return Result.failure(MissingAssignmentException())
        val now = clock.instant()
        val today = BusinessTime.limaDate(now)
        val openId = attendanceDao.findOpen(membershipId)?.id
        // El ingreso de hoy aún no llegó al backend: la salida va a la cola detrás de él (06 §6)
        val queuedClockIn = pendingOperationDao.findByDedupeKey(AttendanceDedupeKeys.clockIn(membershipId, today))
        return submit(
            membershipId = membershipId,
            online = { attendanceService.clockOut(ClockOutRequestDto(attendanceId = openId)) },
            type = ClockOutHandler.TYPE,
            payload = ClockOutRequestDto(attendanceId = openId, clockOutTime = now.toString()),
            dedupeKey = AttendanceDedupeKeys.clockOut(membershipId, today),
            dependsOn = queuedClockIn?.id
        )
    }

    override suspend fun submitJustification(id: AttendanceId, reason: String): Result<SubmitOutcome> {
        val membershipId = sessionManager.getSession()?.membershipId ?: return Result.failure(MissingAssignmentException())
        val body = JustifyAttendanceRequestDto(reason.trim())
        return submit(
            membershipId = membershipId,
            online = { attendanceService.submitJustification(id.value, body) },
            type = JustificationHandler.TYPE,
            payload = JustificationPayload(id.value, body.reason),
            dedupeKey = AttendanceDedupeKeys.justification(id.value)
        )
    }

    /**
     * Con red se envía directo y un error de negocio vuelve con el `message` del backend. Sin red, o si
     * la llamada se corta ([IOException]), se encola: el reintento es seguro por la tabla de 06 §3.
     */
    private suspend fun submit(
        membershipId: String,
        online: suspend () -> Response<AttendanceDto>,
        type: String,
        payload: Any,
        dedupeKey: String,
        dependsOn: String? = null
    ): Result<SubmitOutcome> {
        if (dependsOn == null && connectivity.isOnline.value) {
            val result = apiCall(gson, online) { it }
            if (result.exceptionOrNull() !is IOException) {
                return result.mapCatching { dto ->
                    val saved = cache.save(dto, membershipId) ?: error("AttendanceResource sin clockIn")
                    SubmitOutcome.Recorded(saved.toDomain())
                }
            }
        }
        queue.enqueue(type, payload, dedupeKey, dependsOn) ?: return Result.success(SubmitOutcome.AlreadyQueued)
        return Result.success(SubmitOutcome.Queued)
    }

    private suspend fun refreshAssignment(membershipId: String) {
        apiCall(gson, { employeeService.getByMembership(null, membershipId) }) { it }
            .onSuccess { sessionManager.updateAssignment(it.branchId, it.assignedShiftId) }
            .onFailure { error ->
                // Sin perfil laboral: no tiene sede ni turno. Otros errores (sin red, 5xx) conservan lo guardado
                if (error is ApiException && error.httpStatus == 404) sessionManager.updateAssignment(null, null)
            }
    }

    private suspend fun refreshBranches(now: Long) {
        apiCall(gson, { branchService.getBranches() }) { it }.onSuccess { dtos ->
            val rows = dtos.mapNotNull { it.toEntity(now) }
            branchDao.replaceAll(rows)
            cacheMetadataDao.upsert(CacheMetadataEntity(SCOPE_BRANCHES, now, rows.size))
        }
    }

    private suspend fun refreshShift(shiftId: String, now: Long) {
        apiCall(gson, { attendanceService.getWorkShift(shiftId) }) { it }.onSuccess { dto ->
            shiftDao.upsert(dto.toEntity(now))
            cacheMetadataDao.upsert(CacheMetadataEntity(shiftScope(shiftId), now, 1))
        }
    }

    private suspend fun isStale(scope: String, now: Long): Boolean {
        val lastSyncedAt = cacheMetadataDao.get(scope)?.lastSyncedAt ?: return true
        return now - lastSyncedAt >= REFERENCE_TTL_MILLIS
    }

    companion object {
        const val HISTORY_DAYS = 30L
        const val SCOPE_BRANCHES = "branches"
        const val REFERENCE_TTL_MILLIS = 24 * 60 * 60 * 1000L

        fun shiftScope(shiftId: String) = "shifts:$shiftId"

        /** La cola manda sobre la caché: lo encolado es más nuevo que lo último que vino del backend. */
        internal fun todayOf(
            latest: Attendance?,
            queuedClockIn: PendingOperationEntity?,
            queuedClockOut: PendingOperationEntity?,
            today: LocalDate
        ): TodayAttendance {
            val open = latest?.takeIf { it.isOpen }
            return when {
                queuedClockOut != null -> TodayAttendance.ClockOutQueued(open, Instant.ofEpochMilli(queuedClockOut.createdAt))
                open != null -> TodayAttendance.ClockedIn(open)
                queuedClockIn != null -> TodayAttendance.ClockInQueued(Instant.ofEpochMilli(queuedClockIn.createdAt))
                latest != null && BusinessTime.limaDate(latest.clockIn) == today -> TodayAttendance.Completed(latest)
                else -> TodayAttendance.NotClockedIn
            }
        }
    }
}
