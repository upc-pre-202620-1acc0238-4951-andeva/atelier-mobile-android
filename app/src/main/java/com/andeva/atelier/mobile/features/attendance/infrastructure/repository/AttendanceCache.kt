package com.andeva.atelier.mobile.features.attendance.infrastructure.repository

import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.AttendanceDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.AttendanceEntity
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceDto
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceService
import com.google.gson.Gson
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Escrituras en la caché `attendances` que comparten el repositorio y los handlers de la cola. */
class AttendanceCache @Inject constructor(
    private val service: AttendanceService,
    private val dao: AttendanceDao,
    private val gson: Gson,
    private val clock: Clock
) {
    /** Guarda la `AttendanceResource` que devolvió una escritura. */
    suspend fun save(dto: AttendanceDto, membershipId: String): AttendanceEntity? =
        dto.toEntity(membershipId, clock.millis())?.also { dao.upsert(it) }

    /** Recarga los días de Lima [from]..[to] con `history` y devuelve lo que trajo. */
    suspend fun refreshRange(membershipId: String, from: LocalDate, to: LocalDate): Result<List<AttendanceEntity>> =
        apiCall(gson, { service.history(membershipId, from.toString(), to.toString()) }) { it }
            .mapCatching { dtos ->
                val now = clock.millis()
                val rows = dtos.mapNotNull { it.toEntity(membershipId, now) }
                dao.replaceRange(
                    membershipId,
                    BusinessTime.startOfDay(from).toEpochMilli(),
                    BusinessTime.startOfDay(to.plusDays(1)).toEpochMilli(),
                    rows
                )
                rows
            }

    suspend fun refreshDay(membershipId: String, date: LocalDate) = refreshRange(membershipId, date, date)
}
