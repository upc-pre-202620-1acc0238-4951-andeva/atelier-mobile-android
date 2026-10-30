package com.andeva.atelier.mobile.features.tasks.infrastructure.di

import com.andeva.atelier.mobile.features.tasks.domain.PausedElapsedStore
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.DataStorePausedElapsedStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface TaskTimerModule {
    @Binds
    fun bindPausedElapsedStore(impl: DataStorePausedElapsedStore): PausedElapsedStore
}
