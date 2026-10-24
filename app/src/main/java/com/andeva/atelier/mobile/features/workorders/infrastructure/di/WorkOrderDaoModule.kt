package com.andeva.atelier.mobile.features.workorders.infrastructure.di

import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WorkOrderDaoModule {
    @Provides
    fun provideWorkOrderDao(db: AppDatabase): WorkOrderDao = db.workOrderDao()
}
