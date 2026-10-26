package com.andeva.atelier.mobile.features.reports.infrastructure.di

import com.andeva.atelier.mobile.features.reports.infrastructure.remote.ReportService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReportApiModule {
    @Provides
    @Singleton
    fun provideReportService(retrofit: Retrofit): ReportService = retrofit.create(ReportService::class.java)
}
