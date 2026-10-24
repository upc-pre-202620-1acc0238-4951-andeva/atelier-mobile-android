package com.andeva.atelier.mobile.features.tasks.infrastructure.di

import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object TaskDaoModule {
    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
}
