package com.andeva.atelier.mobile.features.tasks.infrastructure.di

import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.PartSearchService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.TaskService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.VehicleLookupService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkBayLookupService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TaskApiModule {
    @Provides
    @Singleton
    fun provideTaskService(retrofit: Retrofit): TaskService = retrofit.create(TaskService::class.java)

    @Provides
    @Singleton
    fun providePartSearchService(retrofit: Retrofit): PartSearchService = retrofit.create(PartSearchService::class.java)

    @Provides
    @Singleton
    fun provideVehicleLookupService(retrofit: Retrofit): VehicleLookupService = retrofit.create(VehicleLookupService::class.java)

    @Provides
    @Singleton
    fun provideWorkBayLookupService(retrofit: Retrofit): WorkBayLookupService = retrofit.create(WorkBayLookupService::class.java)
}
