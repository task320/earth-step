package io.github.task320.earthstep.feature.measurement

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 端末の再起動後に計測を再開する(P3-5)。
 *
 * 歩数センサーのカウンタは再起動で0へ戻るが、その補正は
 * [io.github.task320.earthstep.core.domain.measurement.step.StepCounterTracker] が
 * 永続化した前回値と比べて行うため、ここでは何もしなくてよい。
 *
 * 再開するのは「ユーザーがオンボーディングを終えていて、計測を有効にしている」場合だけ。
 * 一度もアプリを開いていない端末で勝手に常駐を始めない。
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var measurementController: MeasurementController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob()).launch {
            try {
                val enabled = settingsRepository.measurementEnabled.first()
                val onboarded = settingsRepository.onboardingCompleted.first()
                if (enabled && onboarded) {
                    val started = measurementController.start()
                    Timber.i("measurement restart after boot: started=%b", started)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
