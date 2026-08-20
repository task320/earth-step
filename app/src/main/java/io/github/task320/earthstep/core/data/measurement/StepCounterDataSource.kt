package io.github.task320.earthstep.core.data.measurement

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.task320.earthstep.core.domain.measurement.model.StepSample
import io.github.task320.earthstep.core.domain.measurement.source.StepDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import timber.log.Timber

/**
 * `TYPE_STEP_COUNTER` を購読する(仕様1.2 / P2-12)。
 *
 * このセンサーは専用の低消費電力ハードウェアで動くため常時ONにしてよい。
 * 値は「端末起動からの累積歩数」で、再起動で0へ戻る。増分への変換と再起動の補正は
 * [io.github.task320.earthstep.core.domain.measurement.step.StepCounterTracker] が行う。
 *
 * センサー非搭載の端末では [isAvailable] が false になり、[steps] は何も流さない。
 * その場合は距離がGPSのみに依存する(屋内では加算されない)。
 */
@Singleton
class StepCounterDataSource @Inject constructor(@ApplicationContext context: Context) : StepDataSource {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val stepCounter: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override val isAvailable: Boolean get() = stepCounter != null

    override val steps: Flow<StepSample>
        get() {
            val sensor = stepCounter
            val manager = sensorManager
            if (sensor == null || manager == null) {
                Timber.i("step counter is not available on this device")
                return emptyFlow()
            }
            return callbackFlow {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val cumulative = event.values.firstOrNull()?.toLong() ?: return
                        trySend(
                            StepSample(
                                cumulativeSteps = cumulative,
                                timestampMillis = event.timestamp.toEpochMillis(),
                            ),
                        )
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                awaitClose { manager.unregisterListener(listener) }
            }
        }

    /**
     * センサーイベントのタイムスタンプは「起動からの経過ナノ秒」なので、
     * 位置サンプルと突き合わせられるように epoch millis へ直す。
     */
    private fun Long.toEpochMillis(): Long {
        val bootedAt = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        return bootedAt + this / NANOS_PER_MILLI
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
