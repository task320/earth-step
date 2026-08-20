package io.github.task320.earthstep.core.data.measurement

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.measurement.MeasurementConfig
import io.github.task320.earthstep.core.domain.measurement.model.ActivityUpdate
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.StepSample
import io.github.task320.earthstep.core.domain.measurement.model.UserActivity
import io.github.task320.earthstep.testing.FakeActivityRecognitionDataSource
import io.github.task320.earthstep.testing.FakeLocationDataSource
import io.github.task320.earthstep.testing.FakeMeasurementStateRepository
import io.github.task320.earthstep.testing.FakeProgressRepository
import io.github.task320.earthstep.testing.FakeStepDataSource
import io.github.task320.earthstep.testing.FakeTimeSource
import io.github.task320.earthstep.testing.LocationTrack
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** P2-21: 購読・永続化・日次上限の結線を検証する。 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementEngineTest {

    private val locationDataSource = FakeLocationDataSource()
    private val stepDataSource = FakeStepDataSource()
    private val activityDataSource = FakeActivityRecognitionDataSource()
    private val progressRepository = FakeProgressRepository()
    private val measurementStateRepository = FakeMeasurementStateRepository()
    private val timeSource = FakeTimeSource()

    @Test
    fun `GPSで歩いた距離が整数メートルで永続化される`() = runTest {
        val engine = engine()
        val job = launch { engine.run() }
        runCurrent()

        walk().forEach { sample ->
            locationDataSource.emit(sample)
            runCurrent()
        }
        job.cancelAndJoin()

        // 7m x 10区間。端数はメートル未満で持ち越すため 70m ちょうどになる。
        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(70L)
        assertThat(progressRepository.todayDistanceMeters.first()).isEqualTo(70L)
    }

    @Test
    fun `寝かせていた距離は計測終了時に書き切られる`() = runTest {
        val engine = engine()
        val job = launch { engine.run() }
        runCurrent()

        // 遡及除外の窓(3秒)を出ないうちに計測を終える。
        val samples = LocationTrack(startTimestampMillis = timeSource.now().toEpochMilli())
            .mark(speedMps = 1.4f)
            .walkEast(times = 2, eastMeters = 3.0, intervalMillis = 2_000, speedMps = 1.4f)
            .build()
        samples.forEach { sample ->
            locationDataSource.emit(sample)
            runCurrent()
        }
        job.cancelAndJoin()

        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(6L)
    }

    @Test
    fun `1日の上限を超えた分は加算されない`() {
        runTest {
            // すでに 99,995m 記録済みの状態から歩き始める。
            progressRepository.today = timeSource.today()
            progressRepository.addDistance(99_995L, timeSource.now())

            val engine = engine()
            val job = launch { engine.run() }
            runCurrent()

            walk().forEach { sample ->
                locationDataSource.emit(sample)
                runCurrent()
            }
            job.cancelAndJoin()

            // 上限 100,000m まで 5m だけ通る。
            assertThat(progressRepository.todayDistanceMeters.first()).isEqualTo(100_000L)
        }
    }

    @Test
    fun `乗り物と判定されている間は距離が入らない`() = runTest {
        val engine = engine()
        val job = launch { engine.run() }
        runCurrent()

        activityDataSource.emit(
            ActivityUpdate(
                activity = UserActivity.IN_VEHICLE,
                confidence = 90,
                timestampMillis = timeSource.now().toEpochMilli(),
            ),
        )
        runCurrent()
        walk().forEach { sample ->
            locationDataSource.emit(sample)
            runCurrent()
        }
        job.cancelAndJoin()

        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(0L)
    }

    @Test
    fun `歩数センサーの累積値が永続化される`() = runTest {
        val engine = engine()
        val job = launch { engine.run() }
        runCurrent()

        stepDataSource.emit(
            StepSample(cumulativeSteps = 4_200L, timestampMillis = timeSource.now().toEpochMilli()),
        )
        runCurrent()
        job.cancelAndJoin()

        assertThat(measurementStateRepository.current()).isEqualTo(4_200L)
    }

    @Test
    fun `較正した歩幅がDBへ書き戻される`() = runTest {
        val engine = engine()
        val job = launch { engine.run() }
        runCurrent()

        // 5秒ごとに 7m 進み 20 歩。実測歩幅 35cm へ寄っていく。
        var steps = 0L
        walk().forEach { sample ->
            steps += 20L
            stepDataSource.emit(StepSample(steps, sample.timestampMillis))
            runCurrent()
            locationDataSource.emit(sample)
            runCurrent()
        }
        job.cancelAndJoin()

        assertThat(progressRepository.lifetimeStats.first().strideLengthCm).isLessThan(70.0)
    }

    @Test
    fun `計測中は状態が running になり終了で戻る`() = runTest {
        val engine = engine()
        val job = launch { engine.run() }
        runCurrent()

        assertThat(engine.status.value.running).isTrue()
        assertThat(engine.status.value.stepSensorAvailable).isTrue()

        job.cancelAndJoin()
        assertThat(engine.status.value.running).isFalse()
    }

    private fun engine() = MeasurementEngine(
        locationDataSource = locationDataSource,
        stepDataSource = stepDataSource,
        activityRecognitionDataSource = activityDataSource,
        progressRepository = progressRepository,
        measurementStateRepository = measurementStateRepository,
        timeSource = timeSource,
        config = MeasurementConfig(),
    )

    // 日次上限は距離が入る日付で数えるため、擬似ログの時刻も時計に合わせる。
    private fun walk(): List<LocationSample> = LocationTrack(
        startTimestampMillis = timeSource.now().toEpochMilli(),
    )
        .mark(speedMps = 1.4f)
        .walkEast(times = 10, eastMeters = 7.0, intervalMillis = 5_000, speedMps = 1.4f)
        .build()
}
