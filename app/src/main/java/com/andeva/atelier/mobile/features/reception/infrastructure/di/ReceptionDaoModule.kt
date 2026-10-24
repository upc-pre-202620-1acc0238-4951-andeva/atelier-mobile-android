package com.andeva.atelier.mobile.features.reception.infrastructure.di

import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ReceptionDaoModule {
    @Provides
    fun provideVehicleDao(db: AppDatabase): VehicleDao = db.vehicleDao()
}
