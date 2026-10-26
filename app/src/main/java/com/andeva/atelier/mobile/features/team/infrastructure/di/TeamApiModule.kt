package com.andeva.atelier.mobile.features.team.infrastructure.di

import com.andeva.atelier.mobile.features.team.infrastructure.remote.TeamAttendanceService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TeamApiModule {
    @Provides
    @Singleton
    fun provideTeamAttendanceService(retrofit: Retrofit): TeamAttendanceService =
        retrofit.create(TeamAttendanceService::class.java)
}
