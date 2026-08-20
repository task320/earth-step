package io.github.task320.earthstep.core.domain.measurement.model

/**
 * 位置の1サンプル(P2-1)。
 *
 * Android の `Location` をそのまま持ち回すと計算層がプラットフォームへ依存するため、
 * データ層で必要な値だけを写し取ったこの型へ変換してから計算層へ渡す。
 *
 * @param accuracyMeters 水平精度(m)。小さいほど正確。
 * @param speedMetersPerSecond 端末が報告する速度。取得できない場合は null。
 * @param timestampMillis 測位時刻(epoch millis)。
 * @param isMock モック位置プロバイダ由来かどうか(仕様1.5 / P2-6)。
 */
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
    val timestampMillis: Long,
    val isMock: Boolean = false,
)
