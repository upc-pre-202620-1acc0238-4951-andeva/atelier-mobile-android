package com.andeva.atelier.mobile.features.auth.infrastructure.di

import com.andeva.atelier.mobile.features.auth.domain.AuthRepository
import com.andeva.atelier.mobile.features.auth.domain.BranchChoiceRepository
import com.andeva.atelier.mobile.features.auth.infrastructure.repository.AuthRepositoryImpl
import com.andeva.atelier.mobile.features.auth.infrastructure.repository.BranchChoiceRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
interface AuthRepositoryModule {
    @Binds
    fun provideAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    fun provideBranchChoiceRepository(impl: BranchChoiceRepositoryImpl): BranchChoiceRepository
}
