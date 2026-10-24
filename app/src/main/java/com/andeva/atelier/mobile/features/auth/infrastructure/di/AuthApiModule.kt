package com.andeva.atelier.mobile.features.auth.infrastructure.di

import com.andeva.atelier.mobile.features.auth.infrastructure.remote.AuthService
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.EmployeeService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthApiModule {
    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService = retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideEmployeeService(retrofit: Retrofit): EmployeeService = retrofit.create(EmployeeService::class.java)
}
