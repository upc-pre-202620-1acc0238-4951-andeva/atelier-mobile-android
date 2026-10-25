package com.andeva.atelier.mobile.features.diagnostics.infrastructure.di

import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.RegisterFaultHandler
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.ResolveFaultHandler
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.sync.TelemetryBatchHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/** Handlers de la cola de diagnóstico (docs/06-offline-sync.md §3, "Cómo registra cada feature su tipo"). */
@Module
@InstallIn(SingletonComponent::class)
interface DiagnosticsSyncModule {
    @Binds
    @IntoMap
    @StringKey(TelemetryBatchHandler.TYPE)
    fun bindTelemetryBatchHandler(handler: TelemetryBatchHandler): OperationHandler

    @Binds
    @IntoMap
    @StringKey(RegisterFaultHandler.TYPE)
    fun bindRegisterFaultHandler(handler: RegisterFaultHandler): OperationHandler

    @Binds
    @IntoMap
    @StringKey(ResolveFaultHandler.TYPE)
    fun bindResolveFaultHandler(handler: ResolveFaultHandler): OperationHandler
}
