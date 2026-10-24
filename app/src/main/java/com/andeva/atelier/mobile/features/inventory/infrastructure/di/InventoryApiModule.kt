package com.andeva.atelier.mobile.features.inventory.infrastructure.di

import com.andeva.atelier.mobile.features.inventory.infrastructure.remote.InventoryService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InventoryApiModule {
    @Provides
    @Singleton
    fun provideInventoryService(retrofit: Retrofit): InventoryService = retrofit.create(InventoryService::class.java)
}
