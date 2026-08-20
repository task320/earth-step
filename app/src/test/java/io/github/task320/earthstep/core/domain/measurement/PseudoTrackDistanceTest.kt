package io.github.task320.earthstep.core.domain.measurement

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.testing.PseudoLocationLog
import org.junit.Test

/**
 * P2-11: 実走行を模した擬似ログを流し、期待距離との誤差を検証する。
 *
 * ログは `src/test/resources/measurement/` に置いてある。真値(実際に歩いた距離)は
 * ログを生成した時点で分かっているため、各ケースの定数として持つ。
 *
 * 誤差の許容幅はケースごとに変える。直線やジャンプのように理屈上ぴったり出るものは狭く、
 * ジッターのように原理的に上振れするものは広く取る。
 */
class PseudoTrackDistanceTest {

    private val config = MeasurementConfig()

    @Test
    fun `直線walkは真値どおりに積み上がる`() {
        val measured = measure("straight_walk.csv")

        assertThat(measured).isWithin(EXACT_TOLERANCE_METERS).of(700.0)
    }

    @Test
    fun `カーブを含む経路も真値どおりに積み上がる`() {
        // 一辺105mの正方形。方向転換で距離が失われないことを確認する。
        val measured = measure("curved_walk.csv")

        assertThat(measured).isWithin(EXACT_TOLERANCE_METERS).of(420.0)
    }

    @Test
    fun `ジッターのある経路は上振れするが許容範囲に収まる`() {
        // 真値 560m に ±4m のノイズ。ノイズは必ず距離を増やす方向へ効くため、
        // 「真値を下回らない」ことと「増えすぎない」ことの両方を見る。
        val measured = measure("jitter_walk.csv")

        assertThat(measured).isAtLeast(560.0)
        assertThat(measured).isAtMost(560.0 * JITTER_UPPER_RATIO)
    }

    @Test
    fun `GPSジャンプの点は距離に寄与しない`() {
        // 真値 280m。途中に 2km 離れた外れ値が1点あるが、3点整合性チェックで捨てられる。
        val measured = measure("gps_jump.csv")

        assertThat(measured).isWithin(EXACT_TOLERANCE_METERS).of(280.0)
    }

    @Test
    fun `トンネルの欠測区間はGPSでは加算されない`() {
        // 欠測中に歩いた 120m は歩数フォールバックの担当。GPSは直線で埋めない。
        val measured = measure("tunnel_gap.csv")

        assertThat(measured).isWithin(EXACT_TOLERANCE_METERS).of(280.0)
    }

    @Test
    fun `モック位置の区間は距離にならない`() {
        // 全 40 区間のうち、モック点に触れる 11 区間(15〜25番目)が除外される。
        val measured = measure("mock_location.csv")

        assertThat(measured).isWithin(EXACT_TOLERANCE_METERS).of(203.0)
    }

    @Test
    fun `低精度点を挟んでも直線の距離は失われない`() {
        // 3点に1点 accuracy=45m。起点を進めないため、良好な点どうしで測り直される。
        val measured = measure("low_accuracy.csv")

        assertThat(measured).isWithin(EXACT_TOLERANCE_METERS).of(420.0)
    }

    private fun measure(fileName: String): Double {
        val accumulator = DistanceAccumulator(config)
        var total = 0.0
        PseudoLocationLog.load(fileName).forEach { sample: LocationSample ->
            // 擬似ログは歩き続けている前提なので、常に歩数が増えているものとして流す。
            total += accumulator.onLocation(sample, newSteps = STEPS_PER_SAMPLE).confirmedMeters
        }
        return total + accumulator.flush().confirmedMeters
    }

    private companion object {
        const val EXACT_TOLERANCE_METERS = 1.0
        const val JITTER_UPPER_RATIO = 1.6
        const val STEPS_PER_SAMPLE = 6L
    }
}
