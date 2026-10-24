package com.andeva.atelier.mobile.features.tasks.infrastructure.di

import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.features.tasks.infrastructure.sync.AttachTaskEvidenceHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/** Handlers de la cola de tareas (docs/06-offline-sync.md §3). `UPLOAD_FILE` lo registra el núcleo. */
@Module
@InstallIn(SingletonComponent::class)
interface TaskSyncModule {
    @Binds
    @IntoMap
    @StringKey(AttachTaskEvidenceHandler.TYPE)
    fun bindAttachTaskEvidenceHandler(handler: AttachTaskEvidenceHandler): OperationHandler
}
