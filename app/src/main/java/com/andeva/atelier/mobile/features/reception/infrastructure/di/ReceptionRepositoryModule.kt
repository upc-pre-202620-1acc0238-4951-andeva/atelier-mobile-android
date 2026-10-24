package com.andeva.atelier.mobile.features.reception.infrastructure.di

import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.infrastructure.repository.ReceptionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ReceptionRepositoryModule {
    @Binds
    fun provideReceptionRepository(impl: ReceptionRepositoryImpl): ReceptionRepository
}
