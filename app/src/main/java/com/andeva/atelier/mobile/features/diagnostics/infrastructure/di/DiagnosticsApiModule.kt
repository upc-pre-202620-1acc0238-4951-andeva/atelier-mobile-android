package com.andeva.atelier.mobile.features.diagnostics.infrastructure.di

import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.IotService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsApiModule {
    @Provides
    @Singleton
    fun provideIotService(retrofit: Retrofit): IotService = retrofit.create(IotService::class.java)
}
