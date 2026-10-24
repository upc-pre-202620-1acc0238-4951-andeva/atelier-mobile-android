package com.andeva.atelier.mobile.features.attendance.infrastructure.di

import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceService
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.BranchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AttendanceApiModule {
    @Provides
    @Singleton
    fun provideAttendanceService(retrofit: Retrofit): AttendanceService = retrofit.create(AttendanceService::class.java)

    @Provides
    @Singleton
    fun provideBranchService(retrofit: Retrofit): BranchService = retrofit.create(BranchService::class.java)
}
