package com.andeva.atelier.mobile.features.main.infrastructure.di

import com.andeva.atelier.mobile.features.main.domain.PendingOperationRepository
import com.andeva.atelier.mobile.features.main.infrastructure.repository.PendingOperationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
interface MainRepositoryModule {
    @Binds
    fun providePendingOperationRepository(impl: PendingOperationRepositoryImpl): PendingOperationRepository
}
