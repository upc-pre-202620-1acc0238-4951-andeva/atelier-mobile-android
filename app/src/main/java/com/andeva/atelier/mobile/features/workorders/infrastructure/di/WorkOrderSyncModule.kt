package com.andeva.atelier.mobile.features.workorders.infrastructure.di

import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.features.workorders.infrastructure.sync.AttachIntakeImageHandler
import com.andeva.atelier.mobile.features.workorders.infrastructure.sync.SubmitProposalHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/** Handlers de la cola de órdenes (docs/06-offline-sync.md §3). `UPLOAD_FILE` lo registra el núcleo. */
@Module
@InstallIn(SingletonComponent::class)
interface WorkOrderSyncModule {
    @Binds
    @IntoMap
    @StringKey(AttachIntakeImageHandler.TYPE)
    fun bindAttachIntakeImageHandler(handler: AttachIntakeImageHandler): OperationHandler

    @Binds
    @IntoMap
    @StringKey(SubmitProposalHandler.TYPE)
    fun bindSubmitProposalHandler(handler: SubmitProposalHandler): OperationHandler
}
