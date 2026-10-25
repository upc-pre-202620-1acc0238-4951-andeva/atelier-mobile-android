package com.andeva.atelier.mobile.features.bays.infrastructure.di

import com.andeva.atelier.mobile.features.bays.infrastructure.remote.WorkBayService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BayApiModule {
    @Provides
    @Singleton
    fun provideWorkBayService(retrofit: Retrofit): WorkBayService = retrofit.create(WorkBayService::class.java)
}
