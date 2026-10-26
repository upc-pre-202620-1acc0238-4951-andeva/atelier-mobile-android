package com.andeva.atelier.mobile.features.team.infrastructure.di

import com.andeva.atelier.mobile.features.team.domain.TeamRepository
import com.andeva.atelier.mobile.features.team.infrastructure.repository.TeamRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface TeamRepositoryModule {
    @Binds
    fun provideTeamRepository(impl: TeamRepositoryImpl): TeamRepository
}
