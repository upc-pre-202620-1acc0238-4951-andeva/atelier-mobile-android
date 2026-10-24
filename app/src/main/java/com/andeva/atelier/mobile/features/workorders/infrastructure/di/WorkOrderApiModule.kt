package com.andeva.atelier.mobile.features.workorders.infrastructure.di

import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.ServiceCatalogService
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkOrderApiModule {
    @Provides
    @Singleton
    fun provideWorkOrderService(retrofit: Retrofit): WorkOrderService = retrofit.create(WorkOrderService::class.java)

    @Provides
    @Singleton
    fun provideServiceCatalogService(retrofit: Retrofit): ServiceCatalogService = retrofit.create(ServiceCatalogService::class.java)
}
