package com.andeva.atelier.mobile.features.reports.infrastructure.di

import com.andeva.atelier.mobile.features.reports.domain.ReportsRepository
import com.andeva.atelier.mobile.features.reports.infrastructure.repository.ReportsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ReportRepositoryModule {
    @Binds
    fun provideReportsRepository(impl: ReportsRepositoryImpl): ReportsRepository
}
