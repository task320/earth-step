package io.github.task320.earthstep.core.domain.measurement.step

import io.github.task320.earthstep.core.domain.measurement.MeasurementConfig
import io.github.task320.earthstep.core.domain.model.LifetimeStats

/**
 * 歩幅の自己較正(仕様1.3 / P2-14)。
 *
 * GPSが取れている区間で「実測歩幅 = GPS距離 ÷ 歩数」を計算し、指数移動平均で均す。
 * 身長入力を求めない代わりに、歩けば歩くほど本人の歩幅へ寄っていく。
 *
 * 異常値(30〜100cm の外)は棄却する。GPSのジャンプや歩数の取りこぼしで
 * 極端な値が出ることがあり、そのまま平均へ入れると以後の距離が壊れるため。
 */
class StrideCalibrator(
    private val config: MeasurementConfig,
    initialStrideLengthCm: Double = LifetimeStats.DEFAULT_STRIDE_LENGTH_CM,
) {

    /** 現在の歩幅(cm)。フォールバック時はこの値で歩数を距離へ換算する。 */
    var strideLengthCm: Double = initialStrideLengthCm
        private set

    /** 較正に採用したサンプル数。 */
    var sampleCount: Int = 0
        private set

    /** 異常値として棄却したサンプル数。ログ・テスト用。 */
    var rejectedCount: Int = 0
        private set

    /**
     * GPSで測れた区間の実績を1件与える。
     *
     * @return 歩幅を更新したら true、歩数不足や異常値で棄却したら false。
     */
    fun observe(distanceMeters: Double, steps: Long): Boolean {
        if (steps < config.minStepsForCalibration || distanceMeters <= 0.0) return false

        val observedCm = distanceMeters * CENTIMETERS_PER_METER / steps
        if (observedCm < config.minStrideLengthCm || observedCm > config.maxStrideLengthCm) {
            rejectedCount++
            return false
        }

        val weight = config.strideSmoothingFactor
        strideLengthCm = strideLengthCm * (1.0 - weight) + observedCm * weight
        sampleCount++
        return true
    }

    /** 歩数を距離(m)へ換算する。 */
    fun distanceMetersFor(steps: Long): Double =
        if (steps <= 0L) 0.0 else steps * strideLengthCm / CENTIMETERS_PER_METER

    /** 永続化した歩幅から状態を復元する。 */
    fun restore(strideLengthCm: Double) {
        if (strideLengthCm in config.minStrideLengthCm..config.maxStrideLengthCm) {
            this.strideLengthCm = strideLengthCm
        }
    }

    private companion object {
        const val CENTIMETERS_PER_METER = 100.0
    }
}
