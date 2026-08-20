package io.github.task320.earthstep.core.domain.measurement

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.MovementState
import io.github.task320.earthstep.core.domain.measurement.model.SegmentRejection
import io.github.task320.earthstep.testing.LocationTrack
import org.junit.Test

/** P2-3〜P2-9: フィルタと停止判定の単体検証。 */
class DistanceAccumulatorTest {

    private val config = MeasurementConfig()

    @Test
    fun `最初のサンプルは起点になるだけで距離は増えない`() {
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack().mark().build()

        val update = accumulator.onLocation(samples.first())

        assertThat(update.rejection).isEqualTo(SegmentRejection.NO_ANCHOR)
        assertThat(update.addedMeters).isEqualTo(0.0)
    }

    @Test
    fun `直線を歩いた距離が積み上がる`() {
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack().mark().walkEast(times = 10, eastMeters = 7.0, intervalMillis = 5_000).build()

        val total = accumulator.totalMeters(samples)

        assertThat(total).isWithin(TOLERANCE_METERS).of(70.0)
    }

    @Test
    fun `精度が30mを超える点は距離計算から外れるが直線の距離は失われない`() {
        // P2-3: 途中の1点だけ精度が粗い。起点は進めないため、良好な次の点まで直接測られる。
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack()
            .mark()
            .wait(5_000).move(eastMeters = 7.0).mark()
            .wait(5_000).move(eastMeters = 7.0).mark(accuracyMeters = 45f)
            .wait(5_000).move(eastMeters = 7.0).mark()
            .build()

        val total = accumulator.totalMeters(samples)

        assertThat(total).isWithin(TOLERANCE_METERS).of(21.0)
    }

    @Test
    fun `モック位置の区間は距離にならない`() {
        // P2-6: モック点の前後どちらの区間も加算しない。
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack()
            .mark()
            .wait(5_000).move(eastMeters = 7.0).mark()
            .wait(5_000).move(eastMeters = 7.0).mark(isMock = true)
            .wait(5_000).move(eastMeters = 7.0).mark()
            .wait(5_000).move(eastMeters = 7.0).mark()
            .build()

        val total = accumulator.totalMeters(samples)

        assertThat(total).isWithin(TOLERANCE_METERS).of(14.0)
    }

    @Test
    fun `区間速度が上限を超えたら加算しない`() {
        // P2-4: 5秒で500m = 360km/h。
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack()
            .mark()
            .wait(5_000).move(eastMeters = 500.0).mark()
            .build()

        accumulator.onLocation(samples[0])
        val update = accumulator.onLocation(samples[1])

        assertThat(update.rejection).isEqualTo(SegmentRejection.SPEED_TOO_HIGH)
        assertThat(update.addedMeters).isEqualTo(0.0)
    }

    @Test
    fun `外れ値を挟んだ3点はAとCで直接計算される`() {
        // P2-5: A→B と B→C はどちらも速すぎるが A→C は正常。B を外れ値として捨てる。
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack()
            .mark()
            .wait(5_000).move(eastMeters = 2_000.0).mark()
            .wait(5_000).move(eastMeters = -1_993.0).mark()
            .build()

        val total = accumulator.totalMeters(samples)

        // A→C は 10秒で 7m。B は距離に一切寄与しない。
        assertThat(total).isWithin(TOLERANCE_METERS).of(7.0)
    }

    @Test
    fun `長時間の欠測をまたぐ区間は加算しない`() {
        // P2-15: 欠測中の距離は歩数フォールバックの担当。GPSで直線を引かない。
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack()
            .mark()
            .wait(5_000).move(eastMeters = 7.0).mark()
            .wait(90_000).move(eastMeters = 120.0).mark()
            .wait(5_000).move(eastMeters = 7.0).mark()
            .build()

        val total = accumulator.totalMeters(samples)

        assertThat(total).isWithin(TOLERANCE_METERS).of(14.0)
    }

    @Test
    fun `時刻が戻ったサンプルは無視する`() {
        val accumulator = DistanceAccumulator(config)
        val first = LocationTrack().mark().build().first()
        val stale = first.copy(timestampMillis = first.timestampMillis - 1_000L)

        accumulator.onLocation(first)
        val update = accumulator.onLocation(stale)

        assertThat(update.rejection).isEqualTo(SegmentRejection.NON_MONOTONIC_TIME)
    }

    @Test
    fun `停止が確定すると直近の距離だけが取り消される`() {
        // P2-7/P2-8: 速度が落ち歩数も増えない状態が 2.5 秒続いたら停止確定。
        // 停止確定時点から 3 秒以内に積んだ距離は GPS ノイズとみなして捨て、
        // それより前に確定済みの距離は残す。
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack()
            .mark(speedMps = 1.4f)
            .wait(1_000).move(eastMeters = 1.4).mark(speedMps = 1.4f)
            .wait(1_000).move(eastMeters = 0.3).mark(speedMps = 0.1f)
            .wait(1_000).move(eastMeters = 0.3).mark(speedMps = 0.1f)
            .wait(1_000).move(eastMeters = 0.3).mark(speedMps = 0.1f)
            .wait(1_000).move(eastMeters = 0.3).mark(speedMps = 0.1f)
            .build()

        var confirmed = 0.0
        var retracted = 0.0
        samples.forEach { sample ->
            val update = accumulator.onLocation(sample, newSteps = 0L)
            confirmed += update.confirmedMeters
            retracted += update.retractedMeters
        }

        assertThat(accumulator.movementState).isEqualTo(MovementState.STOPPED)
        // 停止確定の 3 秒前までに積んだ 1.4m + 0.3m は確定済み。
        assertThat(confirmed).isWithin(TOLERANCE_METERS).of(1.7)
        // 直前 3 秒ぶんの 0.3m x 2 は取り消される。
        assertThat(retracted).isWithin(TOLERANCE_METERS).of(0.6)
        assertThat(accumulator.flush().confirmedMeters).isEqualTo(0.0)
    }

    @Test
    fun `歩数が増えていれば速度が低くても停止しない`() {
        // 仕様1.4: 速度と歩数の両条件で誤停止を防ぐ。
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack()
            .mark(speedMps = 0.1f)
            .wait(1_000).move(eastMeters = 0.5).mark(speedMps = 0.1f)
            .wait(1_000).move(eastMeters = 0.5).mark(speedMps = 0.1f)
            .wait(1_000).move(eastMeters = 0.5).mark(speedMps = 0.1f)
            .wait(1_000).move(eastMeters = 0.5).mark(speedMps = 0.1f)
            .build()

        samples.forEach { accumulator.onLocation(it, newSteps = 2L) }

        assertThat(accumulator.movementState).isEqualTo(MovementState.MOVING)
    }

    @Test
    fun `停止から再開すると距離の加算が戻る`() {
        // P2-9: 徒歩速度域と新規歩数が続いたら再開。
        val accumulator = DistanceAccumulator(config)
        val stopping = LocationTrack()
            .mark(speedMps = 0.1f)
            .wait(1_000).mark(speedMps = 0.1f)
            .wait(1_000).mark(speedMps = 0.1f)
            .wait(1_000).mark(speedMps = 0.1f)
            .build()
        stopping.forEach { accumulator.onLocation(it, newSteps = 0L) }
        assertThat(accumulator.movementState).isEqualTo(MovementState.STOPPED)

        val resuming = LocationTrack(startTimestampMillis = stopping.last().timestampMillis)
            .wait(1_000).move(eastMeters = 1.4).mark(speedMps = 1.4f)
            .wait(1_000).move(eastMeters = 1.4).mark(speedMps = 1.4f)
            .wait(1_000).move(eastMeters = 1.4).mark(speedMps = 1.4f)
            .wait(1_000).move(eastMeters = 1.4).mark(speedMps = 1.4f)
            .build()
        resuming.forEach { accumulator.onLocation(it, newSteps = 2L) }

        assertThat(accumulator.movementState).isEqualTo(MovementState.MOVING)
    }

    @Test
    fun `確定は遡及除外の窓を過ぎてから起きる`() {
        val accumulator = DistanceAccumulator(config)
        val samples = LocationTrack()
            .mark()
            .wait(1_000).move(eastMeters = 1.4).mark(speedMps = 1.4f)
            .wait(1_000).move(eastMeters = 1.4).mark(speedMps = 1.4f)
            .build()
        samples.forEach { accumulator.onLocation(it, newSteps = 2L) }

        // まだ 3 秒経っていないので確定していない。
        val beforeFlush = accumulator.onLocation(
            samples.last().copy(timestampMillis = samples.last().timestampMillis + 1_000L),
            newSteps = 2L,
        )
        assertThat(beforeFlush.confirmedMeters).isEqualTo(0.0)
        assertThat(beforeFlush.pendingMeters).isGreaterThan(0.0)

        assertThat(accumulator.flush().confirmedMeters).isWithin(TOLERANCE_METERS).of(2.8)
    }

    @Test
    fun `歩数フォールバックの距離はその場で確定する`() {
        val accumulator = DistanceAccumulator(config)

        val update = accumulator.onFallbackDistance(50.0)

        assertThat(update.confirmedMeters).isEqualTo(50.0)
        assertThat(update.pendingMeters).isEqualTo(0.0)
    }

    private fun DistanceAccumulator.totalMeters(samples: List<LocationSample>): Double {
        var total = 0.0
        samples.forEach { total += onLocation(it, newSteps = 2L).confirmedMeters }
        return total + flush().confirmedMeters
    }

    private companion object {
        const val TOLERANCE_METERS = 0.5
    }
}
