package com.andeva.atelier.mobile.features.tasks.infrastructure.di

import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.infrastructure.repository.TaskRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** `SingletonComponent` como `attendance`: lo comparten la UI y la cola (docs/05-android-architecture.md §5.3). */
@Module
@InstallIn(SingletonComponent::class)
interface TaskRepositoryModule {
    @Binds
    fun provideTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
