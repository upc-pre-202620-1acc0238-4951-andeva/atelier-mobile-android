package com.andeva.atelier.mobile.features.cashier.infrastructure.di

import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.CashierService
import com.andeva.atelier.mobile.features.cashier.infrastructure.remote.HandoverService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CashierApiModule {
    @Provides
    @Singleton
    fun provideCashierService(retrofit: Retrofit): CashierService = retrofit.create(CashierService::class.java)

    @Provides
    @Singleton
    fun provideHandoverService(retrofit: Retrofit): HandoverService = retrofit.create(HandoverService::class.java)
}
