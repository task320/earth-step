package io.github.task320.earthstep.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.task320.earthstep.core.common.time.AppTimeSource
import io.github.task320.earthstep.core.common.time.SystemTimeSource
import io.github.task320.earthstep.core.data.permission.AndroidPermissionChecker
import io.github.task320.earthstep.core.data.repository.MilestoneRepositoryImpl
import io.github.task320.earthstep.core.data.repository.ProgressRepositoryImpl
import io.github.task320.earthstep.core.data.repository.SettingsRepositoryImpl
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.repository.MilestoneRepository
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindMilestoneRepository(impl: MilestoneRepositoryImpl): MilestoneRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAppTimeSource(impl: SystemTimeSource): AppTimeSource

    @Binds
    @Singleton
    abstract fun bindPermissionChecker(impl: AndroidPermissionChecker): PermissionChecker
}
