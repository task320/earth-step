package io.github.task320.earthstep.core.domain.measurement.step

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.measurement.MeasurementConfig
import io.github.task320.earthstep.core.domain.measurement.model.MeasurementMode
import io.github.task320.earthstep.core.domain.measurement.model.StepSample
import io.github.task320.earthstep.testing.LocationTrack
import org.junit.Test

/** P2-12〜P2-16: 歩数フォールバックの部品ごとの検証。 */
class StepFallbackTest {

    private val config = MeasurementConfig()

    // --- P2-13 再起動リセット補正 -------------------------------------------------

    @Test
    fun `初回のサンプルは基準値になるだけで歩数は増えない`() {
        val tracker = StepCounterTracker()

        assertThat(tracker.onSample(StepSample(cumulativeSteps = 12_345L, timestampMillis = 0L))).isEqualTo(0L)
        assertThat(tracker.lastRawSteps).isEqualTo(12_345L)
    }

    @Test
    fun `累積値の差分が歩数の増分になる`() {
        val tracker = StepCounterTracker(lastRawSteps = 1_000L)

        assertThat(tracker.onSample(StepSample(1_030L, 0L))).isEqualTo(30L)
        assertThat(tracker.onSample(StepSample(1_045L, 1_000L))).isEqualTo(15L)
    }

    @Test
    fun `再起動でカウンタが戻っても負の増分にならない`() {
        // 仕様1.3: TYPE_STEP_COUNTER は端末再起動で 0 に戻る。
        val tracker = StepCounterTracker(lastRawSteps = 50_000L)

        val delta = tracker.onSample(StepSample(cumulativeSteps = 120L, timestampMillis = 0L))

        assertThat(delta).isEqualTo(120L)
        assertThat(tracker.rebootCount).isEqualTo(1)
        assertThat(tracker.lastRawSteps).isEqualTo(120L)
    }

    @Test
    fun `永続化した値から復元すると初回から増分を出せる`() {
        val tracker = StepCounterTracker()
        tracker.restore(rawSteps = 2_000L)

        assertThat(tracker.onSample(StepSample(2_050L, 0L))).isEqualTo(50L)
    }

    // --- P2-14 歩幅の自己較正 -----------------------------------------------------

    @Test
    fun `既定の歩幅は70cm`() {
        val calibrator = StrideCalibrator(config)

        assertThat(calibrator.strideLengthCm).isEqualTo(70.0)
        assertThat(calibrator.distanceMetersFor(100L)).isWithin(0.01).of(70.0)
    }

    @Test
    fun `実測歩幅へ移動平均で寄っていく`() {
        val calibrator = StrideCalibrator(config)

        // 100歩で 80m = 歩幅 80cm。
        repeat(20) { calibrator.observe(distanceMeters = 80.0, steps = 100L) }

        assertThat(calibrator.strideLengthCm).isWithin(1.0).of(80.0)
        assertThat(calibrator.sampleCount).isEqualTo(20)
    }

    @Test
    fun `1回の観測で歩幅が飛ばない`() {
        val calibrator = StrideCalibrator(config)

        calibrator.observe(distanceMeters = 90.0, steps = 100L)

        // 平滑化係数 0.2 なので 70 → 74 程度。
        assertThat(calibrator.strideLengthCm).isWithin(0.5).of(74.0)
    }

    @Test
    fun `30cm未満と100cm超の実測値は棄却する`() {
        val calibrator = StrideCalibrator(config)

        assertThat(calibrator.observe(distanceMeters = 10.0, steps = 100L)).isFalse() // 10cm
        assertThat(calibrator.observe(distanceMeters = 200.0, steps = 100L)).isFalse() // 200cm
        assertThat(calibrator.strideLengthCm).isEqualTo(70.0)
        assertThat(calibrator.rejectedCount).isEqualTo(2)
    }

    @Test
    fun `歩数が少なすぎるサンプルは較正に使わない`() {
        val calibrator = StrideCalibrator(config)

        assertThat(calibrator.observe(distanceMeters = 8.0, steps = 10L)).isFalse()
        assertThat(calibrator.strideLengthCm).isEqualTo(70.0)
    }

    @Test
    fun `復元は範囲内の値だけ受け入れる`() {
        val calibrator = StrideCalibrator(config)

        calibrator.restore(65.0)
        assertThat(calibrator.strideLengthCm).isEqualTo(65.0)

        calibrator.restore(500.0)
        assertThat(calibrator.strideLengthCm).isEqualTo(65.0)
    }

    // --- P2-15 切替判定 -----------------------------------------------------------

    @Test
    fun `位置が途絶えたら歩数モードへ切り替わる`() {
        val controller = FallbackController(config)
        controller.start(nowMillis = 0L)

        assertThat(controller.onElapsed(10_000L)).isNull()
        assertThat(controller.onElapsed(20_000L)).isEqualTo(MeasurementMode.STEPS)
        assertThat(controller.mode).isEqualTo(MeasurementMode.STEPS)
    }

    @Test
    fun `精度が50mを超える位置は測位が無いのと同じ扱いになる`() {
        val controller = FallbackController(config)
        controller.start(nowMillis = 0L)
        val samples = LocationTrack(startTimestampMillis = 0L)
            .wait(10_000).mark(accuracyMeters = 80f)
            .wait(10_000).mark(accuracyMeters = 80f)
            .build()

        assertThat(controller.onLocation(samples[0])).isNull()
        assertThat(controller.onLocation(samples[1])).isEqualTo(MeasurementMode.STEPS)
    }

    @Test
    fun `良好な測位が1点戻っただけではGPSモードへ戻らない`() {
        // 短時間の瞬断で往復しないためのヒステリシス。
        val controller = FallbackController(config)
        controller.start(nowMillis = 0L)
        controller.onElapsed(20_000L)

        val samples = LocationTrack(startTimestampMillis = 20_000L)
            .wait(1_000).mark(accuracyMeters = 5f)
            .wait(1_000).mark(accuracyMeters = 5f)
            .build()

        assertThat(controller.onLocation(samples[0])).isNull()
        assertThat(controller.onLocation(samples[1])).isNull()
        assertThat(controller.mode).isEqualTo(MeasurementMode.STEPS)
    }

    @Test
    fun `良好な測位が続けばGPSモードへ戻る`() {
        val controller = FallbackController(config)
        controller.start(nowMillis = 0L)
        controller.onElapsed(20_000L)

        val samples = LocationTrack(startTimestampMillis = 20_000L)
            .wait(1_000).mark(accuracyMeters = 5f)
            .wait(5_000).mark(accuracyMeters = 5f)
            .build()
        controller.onLocation(samples[0])

        assertThat(controller.onLocation(samples[1])).isEqualTo(MeasurementMode.GPS)
        assertThat(controller.mode).isEqualTo(MeasurementMode.GPS)
    }

    @Test
    fun `復帰の途中で精度が崩れたらやり直しになる`() {
        val controller = FallbackController(config)
        controller.start(nowMillis = 0L)
        controller.onElapsed(20_000L)

        val samples = LocationTrack(startTimestampMillis = 20_000L)
            .wait(1_000).mark(accuracyMeters = 5f)
            .wait(2_000).mark(accuracyMeters = 45f)
            .wait(3_000).mark(accuracyMeters = 5f)
            .build()
        samples.forEach { controller.onLocation(it) }

        assertThat(controller.mode).isEqualTo(MeasurementMode.STEPS)
    }
}
