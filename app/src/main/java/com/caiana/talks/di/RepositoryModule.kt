package com.caiana.talks.di

import com.caiana.talks.data.repository.StatsRepository
import com.caiana.talks.data.repository.StatsRepositoryImpl
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository
}
