package com.andeva.atelier.mobile.features.bays.infrastructure.di

import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BayDaoModule {
    @Provides
    fun provideBayDao(db: AppDatabase): BayDao = db.bayDao()
}
