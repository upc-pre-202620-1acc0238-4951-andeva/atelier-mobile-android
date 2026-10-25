package com.andeva.atelier.mobile.features.diagnostics.infrastructure.di

import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticSession
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.HealthReportRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapterProvider
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLink
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.BluetoothSppTransport
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.BondedObdAdapterProvider
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.Elm327ObdLink
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.ObdForegroundController
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.ObdSessionServiceController
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.bluetooth.ObdTransport
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository.DiagnosticsRepositoryImpl
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository.HealthReportRepositoryImpl
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.session.LiveDiagnosticSession
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * `SingletonComponent` y no `ViewModelComponent`: la sesión OBD-II dura más que una pantalla y la cola usa el
 * repositorio desde el `Worker` (excepción de docs/05-android-architecture.md §5.3).
 */
@Module
@InstallIn(SingletonComponent::class)
interface DiagnosticsRepositoryModule {
    @Binds
    fun provideDiagnosticsRepository(impl: DiagnosticsRepositoryImpl): DiagnosticsRepository

    @Binds
    fun provideHealthReportRepository(impl: HealthReportRepositoryImpl): HealthReportRepository

    @Binds
    fun provideObdTransport(impl: BluetoothSppTransport): ObdTransport

    @Binds
    fun provideObdLink(impl: Elm327ObdLink): ObdLink

    @Binds
    fun provideObdAdapterProvider(impl: BondedObdAdapterProvider): ObdAdapterProvider

    @Binds
    fun provideObdForegroundController(impl: ObdSessionServiceController): ObdForegroundController

    companion object {
        /** Una sola sesión en toda la app: es dueña del socket del ELM327. */
        @Provides
        @Singleton
        fun provideDiagnosticSession(
            link: ObdLink,
            repository: DiagnosticsRepository,
            locationProvider: LocationProvider,
            foreground: ObdForegroundController
        ): DiagnosticSession = LiveDiagnosticSession(
            link = link,
            repository = repository,
            locationProvider = locationProvider,
            foreground = foreground,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }
}
