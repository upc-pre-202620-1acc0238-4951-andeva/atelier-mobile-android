package com.andeva.atelier.mobile.features.cashier.infrastructure.di

import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.VoucherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CashierDaoModule {
    @Provides
    fun provideVoucherDao(db: AppDatabase): VoucherDao = db.voucherDao()
}
