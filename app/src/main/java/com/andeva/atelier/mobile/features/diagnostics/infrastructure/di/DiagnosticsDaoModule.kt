package com.andeva.atelier.mobile.features.diagnostics.infrastructure.di

import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.PredictiveAlertDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.TelemetryReadingDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsDaoModule {
    @Provides
    fun provideTelemetryReadingDao(db: AppDatabase): TelemetryReadingDao = db.telemetryReadingDao()

    @Provides
    fun provideVehicleFaultDao(db: AppDatabase): VehicleFaultDao = db.vehicleFaultDao()

    @Provides
    fun providePredictiveAlertDao(db: AppDatabase): PredictiveAlertDao = db.predictiveAlertDao()
}
