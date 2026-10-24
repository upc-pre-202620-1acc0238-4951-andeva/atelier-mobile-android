package com.andeva.atelier.mobile.features.inventory.infrastructure.di

import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.infrastructure.repository.InventoryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface InventoryRepositoryModule {
    @Binds
    fun provideInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository
}
