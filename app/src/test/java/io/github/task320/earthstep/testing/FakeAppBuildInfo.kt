package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.common.AppBuildInfo

class FakeAppBuildInfo(override val versionName: String = "0.0.0-test", override val isDebug: Boolean = true) :
    AppBuildInfo
