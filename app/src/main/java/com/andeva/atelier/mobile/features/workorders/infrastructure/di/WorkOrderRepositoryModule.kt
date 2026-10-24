package com.andeva.atelier.mobile.features.workorders.infrastructure.di

import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.infrastructure.repository.WorkOrderRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** `@Singleton`: el repositorio guarda en memoria el catálogo de servicios del taller. */
@Module
@InstallIn(SingletonComponent::class)
interface WorkOrderRepositoryModule {
    @Binds
    @Singleton
    fun provideWorkOrderRepository(impl: WorkOrderRepositoryImpl): WorkOrderRepository
}
