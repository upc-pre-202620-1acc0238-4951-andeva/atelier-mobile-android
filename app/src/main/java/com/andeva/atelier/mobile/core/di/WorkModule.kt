package com.andeva.atelier.mobile.core.di

import com.andeva.atelier.mobile.core.data.sync.OperationHandler
import com.andeva.atelier.mobile.core.data.sync.UploadFileHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.Multibinds
import dagger.multibindings.StringKey

/** Handlers de la cola por `type`. Cada feature agrega los suyos con `@Binds @IntoMap @StringKey`. */
@Module
@InstallIn(SingletonComponent::class)
interface WorkModule {
    @Multibinds
    fun operationHandlers(): Map<String, OperationHandler>

    @Binds
    @IntoMap
    @StringKey(UploadFileHandler.TYPE)
    fun bindUploadFileHandler(handler: UploadFileHandler): OperationHandler
}
