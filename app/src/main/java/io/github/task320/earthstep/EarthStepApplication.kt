package io.github.task320.earthstep

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.task320.earthstep.core.common.log.ReleaseLogTree
import io.github.task320.earthstep.core.common.sound.SoundEffects
import io.github.task320.earthstep.core.data.work.DriveSyncScheduler
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class EarthStepApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseLogTree())
        SoundEffects.init(this)
        DriveSyncScheduler.schedule(this)
    }
}
