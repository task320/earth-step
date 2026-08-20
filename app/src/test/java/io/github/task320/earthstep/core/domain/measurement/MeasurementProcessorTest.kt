package io.github.task320.earthstep.core.domain.measurement

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.measurement.model.ActivityUpdate
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.MeasurementMode
import io.github.task320.earthstep.core.domain.measurement.model.SegmentRejection
import io.github.task320.earthstep.core.domain.measurement.model.StepSample
import io.github.task320.earthstep.core.domain.measurement.model.UserActivity
import io.github.task320.earthstep.testing.LocationTrack
import org.junit.Test

/**
 * P2-17 / P2-18 / P2-21: 入力を束ねた状態機械の検証。
 *
 * とくに「GPS欠測 → 歩数 → GPS復帰」で距離が二重加算も欠落もしないこと(P2-17)を見る。
 */
class MeasurementProcessorTest {

    private val config = MeasurementConfig()

    @Test
    fun `GPSモードでは歩数は距離にならない`() {
        val processor = processor()
        val samples = walk(times = 4, intervalMillis = 5_000)

        var total = 0.0
        samples.forEachIndexed { index, sample ->
            total += processor.process(steps(20L * (index + 1), sample.timestampMillis)).confirmedMeters
            total += processor.process(MeasurementInput.Location(sample)).confirmedMeters
        }
        total += processor.flush().confirmedMeters

        // 歩数(80歩 = 56m相当)ではなく、GPSで測った 4区間 x 7m = 28m が積まれる。
        assertThat(total).isWithin(TOLERANCE_METERS).of(28.0)
        assertThat(processor.mode).isEqualTo(MeasurementMode.GPS)
    }

    @Test
    fun `GPSが途絶えると歩数モードへ移り歩数で距離が積まれる`() {
        val processor = processor()
        processor.start(0L)

        // 20秒以上なにも届かない → 歩数モードへ。
        processor.process(MeasurementInput.Tick(21_000L))
        assertThat(processor.mode).isEqualTo(MeasurementMode.STEPS)

        // 歩数センサーの基準値を入れてから 100 歩ぶん歩く。
        processor.process(steps(0L, 21_000L))
        val outcome = processor.process(steps(100L, 26_000L))

        // 既定歩幅 70cm × 100歩 = 70m。
        assertThat(outcome.confirmedMeters).isWithin(TOLERANCE_METERS).of(70.0)
    }

    @Test
    fun `GPS欠測から復帰しても距離が二重加算されない`() {
        // P2-17: GPS → 欠測(歩数) → GPS復帰 の一連の流れ。
        val processor = processor()
        processor.start(0L)
        processor.process(steps(0L, 0L))

        // 1) GPSで 7m x 3 = 21m 歩く。
        var total = 0.0
        val before = LocationTrack(startTimestampMillis = 0L)
            .mark()
            .walkEast(times = 3, eastMeters = 7.0, intervalMillis = 5_000, speedMps = 1.4f)
            .build()
        var steps = 0L
        before.forEach { sample ->
            steps += 10L
            total += processor.process(steps(steps, sample.timestampMillis)).confirmedMeters
            total += processor.process(MeasurementInput.Location(sample)).confirmedMeters
        }
        val gpsEndTimestamp = before.last().timestampMillis

        // 2) 30秒欠測。その間に 50 歩(= 35m)歩く。
        processor.process(MeasurementInput.Tick(gpsEndTimestamp + 21_000L))
        assertThat(processor.mode).isEqualTo(MeasurementMode.STEPS)
        steps += 50L
        total += processor.process(steps(steps, gpsEndTimestamp + 25_000L)).confirmedMeters

        // 3) GPSが復帰。欠測中に進んだ 200m ぶん離れた場所から再開する。
        val after = LocationTrack(
            startLongitude = 139.7454 + 0.002,
            startTimestampMillis = gpsEndTimestamp + 30_000L,
        )
            .mark(speedMps = 1.4f)
            .walkEast(times = 4, eastMeters = 7.0, intervalMillis = 5_000, speedMps = 1.4f)
            .build()
        after.forEach { sample ->
            steps += 10L
            total += processor.process(steps(steps, sample.timestampMillis)).confirmedMeters
            total += processor.process(MeasurementInput.Location(sample)).confirmedMeters
        }
        total += processor.flush().confirmedMeters

        assertThat(processor.mode).isEqualTo(MeasurementMode.GPS)
        // GPS 21m + 歩数 35m + 復帰後 28m = 84m。
        // 欠測をまたぐ 200m の直線も、歩数と重なる区間も入っていない。
        assertThat(total).isWithin(TOLERANCE_METERS).of(84.0)
    }

    @Test
    fun `自転車と判定されている間は距離を積まない`() {
        // 仕様1.1 / P2-18。
        val processor = processor()
        processor.start(0L)
        processor.process(activity(UserActivity.ON_BICYCLE, confidence = 80, timestampMillis = 0L))

        val samples = walk(times = 5, intervalMillis = 5_000)
        var total = 0.0
        samples.forEach { total += processor.process(MeasurementInput.Location(it)).confirmedMeters }
        total += processor.flush().confirmedMeters

        assertThat(total).isEqualTo(0.0)
    }

    @Test
    fun `信頼度が低い判定では対象可否を変えない`() {
        val processor = processor()
        processor.start(0L)

        val outcome = processor.process(
            activity(UserActivity.IN_VEHICLE, confidence = 20, timestampMillis = 0L),
        )

        assertThat(outcome.rejection).isNull()
    }

    @Test
    fun `乗り物から徒歩へ戻ると再び距離を積む`() {
        val processor = processor()
        processor.start(0L)
        processor.process(activity(UserActivity.IN_VEHICLE, confidence = 90, timestampMillis = 0L))

        val ride = walk(times = 3, intervalMillis = 5_000)
        ride.forEach { processor.process(MeasurementInput.Location(it)) }

        processor.process(
            activity(UserActivity.ON_FOOT, confidence = 90, timestampMillis = ride.last().timestampMillis),
        )
        val walk = LocationTrack(startTimestampMillis = ride.last().timestampMillis)
            .mark(speedMps = 1.4f)
            .walkEast(times = 3, eastMeters = 7.0, intervalMillis = 5_000, speedMps = 1.4f)
            .build()

        var total = 0.0
        walk.forEach { total += processor.process(MeasurementInput.Location(it)).confirmedMeters }
        total += processor.flush().confirmedMeters

        assertThat(total).isWithin(TOLERANCE_METERS).of(21.0)
    }

    @Test
    fun `乗り物と判定された位置は理由つきで棄却される`() {
        val processor = processor()
        processor.start(0L)
        processor.process(activity(UserActivity.IN_VEHICLE, confidence = 90, timestampMillis = 0L))

        val outcome = processor.process(MeasurementInput.Location(walk(times = 1, intervalMillis = 5_000).first()))

        assertThat(outcome.rejection).isEqualTo(SegmentRejection.NOT_ON_FOOT)
    }

    @Test
    fun `歩幅はGPS区間の実績から較正される`() {
        // P2-14: 5秒ごとに 7m 進み 10 歩。実測歩幅は 70cm のままだが、較正が走ることを見る。
        val processor = processor(initialStrideLengthCm = 50.0)
        processor.start(0L)
        processor.process(steps(0L, 0L))

        val samples = LocationTrack(startTimestampMillis = 0L)
            .mark(speedMps = 1.4f)
            .walkEast(times = 10, eastMeters = 7.0, intervalMillis = 5_000, speedMps = 1.4f)
            .build()
        var steps = 0L
        samples.forEach { sample ->
            steps += 10L
            processor.process(steps(steps, sample.timestampMillis))
            processor.process(MeasurementInput.Location(sample))
        }

        // 50cm から 70cm 側へ寄る。
        assertThat(processor.strideLengthCm).isGreaterThan(50.0)
        assertThat(processor.strideLengthCm).isLessThan(70.0)
    }

    private fun processor(initialStrideLengthCm: Double = 70.0) = MeasurementProcessor(
        config = config,
        initialStrideLengthCm = initialStrideLengthCm,
    )

    private fun walk(times: Int, intervalMillis: Long): List<LocationSample> = LocationTrack(
        startTimestampMillis = 0L,
    )
        .mark(speedMps = 1.4f)
        .walkEast(times = times, eastMeters = 7.0, intervalMillis = intervalMillis, speedMps = 1.4f)
        .build()

    private fun steps(cumulative: Long, timestampMillis: Long) =
        MeasurementInput.Steps(StepSample(cumulative, timestampMillis))

    private fun activity(activity: UserActivity, confidence: Int, timestampMillis: Long) =
        MeasurementInput.Activity(ActivityUpdate(activity, confidence, timestampMillis))

    private companion object {
        const val TOLERANCE_METERS = 1.0
    }
}
