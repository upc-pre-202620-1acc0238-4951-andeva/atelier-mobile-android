package com.andeva.atelier.mobile.features.cashier.infrastructure.di

import com.andeva.atelier.mobile.features.cashier.domain.CashierRepository
import com.andeva.atelier.mobile.features.cashier.infrastructure.repository.CashierRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface CashierRepositoryModule {
    @Binds
    fun provideCashierRepository(impl: CashierRepositoryImpl): CashierRepository
}
