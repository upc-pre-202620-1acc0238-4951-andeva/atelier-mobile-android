package com.andeva.atelier.mobile.features.attendance.infrastructure.di

import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.ClockInHandler
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.ClockOutHandler
import com.andeva.atelier.mobile.features.attendance.infrastructure.sync.JustificationHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/** Handlers de la cola de asistencia (docs/06-offline-sync.md §3, "Cómo registra cada feature su tipo"). */
@Module
@InstallIn(SingletonComponent::class)
interface AttendanceSyncModule {
    @Binds
    @IntoMap
    @StringKey(ClockInHandler.TYPE)
    fun bindClockInHandler(handler: ClockInHandler): OperationHandler

    @Binds
    @IntoMap
    @StringKey(ClockOutHandler.TYPE)
    fun bindClockOutHandler(handler: ClockOutHandler): OperationHandler

    @Binds
    @IntoMap
    @StringKey(JustificationHandler.TYPE)
    fun bindJustificationHandler(handler: JustificationHandler): OperationHandler
}
