package com.andeva.atelier.mobile.features.attendance.infrastructure.di

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.attendance.infrastructure.location.FusedLocationProvider
import com.andeva.atelier.mobile.features.attendance.infrastructure.repository.AttendanceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * `SingletonComponent` y no `ViewModelComponent`: la cola refresca la asistencia desde el `Worker`
 * (excepción de docs/05-android-architecture.md §5.3).
 */
@Module
@InstallIn(SingletonComponent::class)
interface AttendanceRepositoryModule {
    @Binds
    fun provideAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds
    fun provideLocationProvider(impl: FusedLocationProvider): LocationProvider
}
