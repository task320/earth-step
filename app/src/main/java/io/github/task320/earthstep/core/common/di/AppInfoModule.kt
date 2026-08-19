package io.github.task320.earthstep.core.common.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.task320.earthstep.core.common.AppBuildInfo
import io.github.task320.earthstep.core.common.DefaultAppBuildInfo
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppInfoModule {

    @Binds
    @Singleton
    abstract fun bindAppBuildInfo(impl: DefaultAppBuildInfo): AppBuildInfo
}
