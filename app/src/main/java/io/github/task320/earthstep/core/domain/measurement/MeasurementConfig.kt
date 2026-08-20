package io.github.task320.earthstep.core.domain.measurement

/**
 * 計測エンジンの閾値をまとめたもの(仕様1.3〜1.5)。
 *
 * 実地検証(P8-1)で調整する余地があるため、定数を直接埋め込まずここへ集約する。
 * 単体テストでは短い時間窓を指定して待ち時間を減らす。
 */
data class MeasurementConfig(
    /** これより粗い位置は距離計算から除外する(仕様1.5 / P2-3)。 */
    val maxAccuracyMeters: Float = 30f,
    /** これより粗い位置が続いたら歩数フォールバックへ切り替える(仕様1.3 / P2-15)。 */
    val fallbackAccuracyMeters: Float = 50f,
    /** 区間速度の上限。これを超える区間は加算しない(仕様1.5 / P2-4)。 */
    val maxSegmentSpeedKmh: Double = 35.0,
    /** 停止とみなす速度の上限(仕様1.4 / P2-7)。 */
    val stopSpeedKmh: Double = 1.5,
    /** 停止確定までの継続時間(仕様1.4)。 */
    val stopHoldMillis: Long = 2_500L,
    /** 再開確定までの継続時間(仕様1.4 / P2-9)。 */
    val resumeHoldMillis: Long = 2_500L,
    /** 停止確定時に取り消す距離の時間窓(仕様1.4 / P2-8)。 */
    val retroactiveWindowMillis: Long = 3_000L,
    /**
     * 直前の位置を起点として保持し続ける上限。
     * これを超える欠測は歩数フォールバック(P2-15)の担当なので、GPS側では加算しない。
     */
    val maxSegmentGapMillis: Long = 20_000L,
    /** 位置更新が途絶えたら歩数フォールバックへ切り替えるまでの時間(仕様1.3 / P2-15)。 */
    val fallbackGapMillis: Long = 17_500L,
    /**
     * 歩数モードからGPSモードへ戻すために、良好な測位が続くべき時間(仕様1.3 / P2-15)。
     * 瞬間的に1点だけ精度が良くなっても戻らないようにするためのヒステリシス。
     */
    val gpsRecoveryHoldMillis: Long = 5_000L,
    /** 1日の距離のソフト上限(仕様1.5 / P2-10)。 */
    val dailyCapMeters: Long = 100_000L,
    /** 自己較正で採用する歩幅の下限(仕様1.3 / P2-14)。 */
    val minStrideLengthCm: Double = 30.0,
    /** 自己較正で採用する歩幅の上限。 */
    val maxStrideLengthCm: Double = 100.0,
    /** 歩幅の移動平均で新しい実測値に与える重み(0〜1)。 */
    val strideSmoothingFactor: Double = 0.2,
    /** 歩幅の較正に必要な最小歩数。少なすぎるサンプルは誤差が大きい。 */
    val minStepsForCalibration: Long = 20L,
    /** 直線が続くときの位置更新間隔(仕様1.2 / P2-20)。 */
    val locationIntervalStraightMillis: Long = 10_000L,
    /** 方向転換が多いときの位置更新間隔。 */
    val locationIntervalTurningMillis: Long = 5_000L,
    /** `ON_FOOT` をXP対象とみなす信頼度の下限(仕様1.1 / P2-18)。 */
    val minOnFootConfidence: Int = 50,
    /** 方向転換とみなす角度(度)。 */
    val turnAngleDegrees: Double = 30.0,
) {
    val maxSegmentSpeedMps: Double get() = maxSegmentSpeedKmh / SECONDS_PER_HOUR_OVER_KM
    val stopSpeedMps: Double get() = stopSpeedKmh / SECONDS_PER_HOUR_OVER_KM

    private companion object {
        /** km/h を m/s へ換算する除数。 */
        const val SECONDS_PER_HOUR_OVER_KM = 3.6
    }
}
