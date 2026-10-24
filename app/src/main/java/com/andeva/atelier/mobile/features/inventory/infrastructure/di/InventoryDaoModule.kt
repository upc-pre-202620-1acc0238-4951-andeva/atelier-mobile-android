package com.andeva.atelier.mobile.features.inventory.infrastructure.di

import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.BatchDao
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.PartDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object InventoryDaoModule {
    @Provides
    fun providePartDao(db: AppDatabase): PartDao = db.partDao()

    @Provides
    fun provideBatchDao(db: AppDatabase): BatchDao = db.batchDao()
}
