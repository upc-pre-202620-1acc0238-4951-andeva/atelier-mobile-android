package com.andeva.atelier.mobile.features.attendance.infrastructure.di

import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.AttendanceDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.BranchDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.ShiftDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AttendanceDaoModule {
    @Provides
    fun provideAttendanceDao(db: AppDatabase): AttendanceDao = db.attendanceDao()

    @Provides
    fun provideShiftDao(db: AppDatabase): ShiftDao = db.shiftDao()

    @Provides
    fun provideBranchDao(db: AppDatabase): BranchDao = db.branchDao()
}
