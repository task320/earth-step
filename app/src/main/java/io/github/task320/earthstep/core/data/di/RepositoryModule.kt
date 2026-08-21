package io.github.task320.earthstep.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.task320.earthstep.core.common.time.AppTimeSource
import io.github.task320.earthstep.core.common.time.SystemTimeSource
import io.github.task320.earthstep.core.data.permission.AndroidPermissionChecker
import io.github.task320.earthstep.core.data.repository.BackupRepositoryImpl
import io.github.task320.earthstep.core.data.repository.CelebrationQueueRepositoryImpl
import io.github.task320.earthstep.core.data.repository.DriveSyncRepositoryImpl
import io.github.task320.earthstep.core.data.repository.DriveSyncStateRepositoryImpl
import io.github.task320.earthstep.core.data.repository.GoogleAuthRepositoryImpl
import io.github.task320.earthstep.core.data.repository.MilestoneRepositoryImpl
import io.github.task320.earthstep.core.data.repository.ProgressRepositoryImpl
import io.github.task320.earthstep.core.data.repository.SettingsRepositoryImpl
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.repository.BackupRepository
import io.github.task320.earthstep.core.domain.repository.CelebrationQueueRepository
import io.github.task320.earthstep.core.domain.repository.DriveSyncRepository
import io.github.task320.earthstep.core.domain.repository.DriveSyncStateRepository
import io.github.task320.earthstep.core.domain.repository.GoogleAuthRepository
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
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindCelebrationQueueRepository(impl: CelebrationQueueRepositoryImpl): CelebrationQueueRepository

    @Binds
    @Singleton
    abstract fun bindPermissionChecker(impl: AndroidPermissionChecker): PermissionChecker

    @Binds
    @Singleton
    abstract fun bindGoogleAuthRepository(impl: GoogleAuthRepositoryImpl): GoogleAuthRepository

    @Binds
    @Singleton
    abstract fun bindDriveSyncRepository(impl: DriveSyncRepositoryImpl): DriveSyncRepository

    @Binds
    @Singleton
    abstract fun bindDriveSyncStateRepository(impl: DriveSyncStateRepositoryImpl): DriveSyncStateRepository
}
