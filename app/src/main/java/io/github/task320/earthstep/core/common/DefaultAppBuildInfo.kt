package io.github.task320.earthstep.core.common

import io.github.task320.earthstep.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAppBuildInfo @Inject constructor() : AppBuildInfo {
    override val versionName: String = BuildConfig.VERSION_NAME
    override val isDebug: Boolean = BuildConfig.DEBUG
}
