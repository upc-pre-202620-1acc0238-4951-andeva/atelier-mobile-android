package com.andeva.atelier.mobile.features.bays.infrastructure.di

import com.andeva.atelier.mobile.features.bays.domain.BayRepository
import com.andeva.atelier.mobile.features.bays.infrastructure.repository.BayRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface BayRepositoryModule {
    @Binds
    fun provideBayRepository(impl: BayRepositoryImpl): BayRepository
}
