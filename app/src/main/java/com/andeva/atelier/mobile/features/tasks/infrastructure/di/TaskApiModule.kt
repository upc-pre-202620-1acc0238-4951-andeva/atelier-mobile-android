package com.andeva.atelier.mobile.features.tasks.infrastructure.di

import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.TaskService
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
}
