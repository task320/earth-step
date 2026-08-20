package io.github.task320.earthstep.core.domain.measurement.step

import io.github.task320.earthstep.core.domain.measurement.MeasurementConfig
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.MeasurementMode

/**
 * GPSモードと歩数モードの切り替え(仕様1.3 / P2-15・P2-16)。
 *
 * 切り替えの条件はどちらの向きにもヒステリシスを持たせる。
 * 地下街の入口などで数秒おきに往復すると、切り替えの端点で距離の取りこぼしや
 * 二重計上が起きやすいため。
 *
 * ## 二重計上を防ぐ仕組み
 * 距離を作るのは常にどちらか一方のモードだけ。
 * - GPSモード中: 歩数は停止判定と歩幅較正にしか使わない
 * - 歩数モード中: 位置は復帰判定にしか使わない
 *
 * モードが変わった瞬間に、呼び出し側は距離計算の起点(GPSのアンカー、歩数の起点)を
 * どちらも捨てる。こうすると切り替えの瞬間をまたぐ区間はどちらのモードでも加算されない。
 */
class FallbackController(private val config: MeasurementConfig) {

    var mode: MeasurementMode = MeasurementMode.GPS
        private set

    private var lastUsableLocationMillis: Long? = null
    private var goodLocationSinceMillis: Long? = null

    /** 位置サンプルを与え、モードが変わったら新しいモードを返す。 */
    fun onLocation(sample: LocationSample): MeasurementMode? {
        val usable = !sample.isMock && sample.accuracyMeters <= config.fallbackAccuracyMeters
        if (usable) {
            lastUsableLocationMillis = sample.timestampMillis
        }
        return when (mode) {
            MeasurementMode.GPS -> evaluateGpsMode(sample.timestampMillis)
            MeasurementMode.STEPS -> evaluateStepsMode(sample)
        }
    }

    /**
     * 位置が届かないまま時間が経ったことを伝える。
     * 測位が完全に途絶えた場合、[onLocation] は呼ばれないため、こちらで切り替えを判断する。
     */
    fun onElapsed(nowMillis: Long): MeasurementMode? =
        if (mode == MeasurementMode.GPS) evaluateGpsMode(nowMillis) else null

    /** 計測開始時に基準時刻を入れる。 */
    fun start(nowMillis: Long) {
        mode = MeasurementMode.GPS
        lastUsableLocationMillis = nowMillis
        goodLocationSinceMillis = null
    }

    private fun evaluateGpsMode(nowMillis: Long): MeasurementMode? {
        val since = lastUsableLocationMillis ?: nowMillis.also { lastUsableLocationMillis = it }
        if (nowMillis - since < config.fallbackGapMillis) return null

        mode = MeasurementMode.STEPS
        goodLocationSinceMillis = null
        return MeasurementMode.STEPS
    }

    private fun evaluateStepsMode(sample: LocationSample): MeasurementMode? {
        val good = !sample.isMock && sample.accuracyMeters <= config.maxAccuracyMeters
        if (!good) {
            goodLocationSinceMillis = null
            return null
        }
        val since = goodLocationSinceMillis
            ?: sample.timestampMillis.also { goodLocationSinceMillis = it }
        if (sample.timestampMillis - since < config.gpsRecoveryHoldMillis) return null

        mode = MeasurementMode.GPS
        goodLocationSinceMillis = null
        lastUsableLocationMillis = sample.timestampMillis
        return MeasurementMode.GPS
    }
}
