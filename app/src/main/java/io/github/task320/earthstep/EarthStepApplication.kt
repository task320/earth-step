package io.github.task320.earthstep

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.task320.earthstep.core.common.log.ReleaseLogTree
import timber.log.Timber

@HiltAndroidApp
class EarthStepApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseLogTree())
    }
}
