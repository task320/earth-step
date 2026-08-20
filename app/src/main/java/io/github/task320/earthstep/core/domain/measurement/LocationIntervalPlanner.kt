package io.github.task320.earthstep.core.domain.measurement

import io.github.task320.earthstep.core.domain.measurement.model.LocationSample

/**
 * 位置更新間隔の適応ロジック(仕様1.2 / P2-20)。
 *
 * 直線が続く区間は間隔を空けても距離の誤差が出にくい。逆に方向転換が多い区間で間隔を空けると、
 * 曲がり角がショートカットされて距離が短く出る。そこで直近の方位変化を見て間隔を寄せる。
 *
 * 判定材料は方位だけなので Android 非依存にでき、擬似ログで挙動を確認できる。
 */
class LocationIntervalPlanner(private val config: MeasurementConfig) {

    private var previousSample: LocationSample? = null
    private var previousBearingDegrees: Double? = null
    private var recentTurns = ArrayDeque<Boolean>()

    /** 現在推奨される位置更新間隔(ミリ秒)。 */
    var intervalMillis: Long = config.locationIntervalStraightMillis
        private set

    /**
     * サンプルを1件与えて間隔を更新する。
     * @return 間隔が変わったら新しい値、変わらなければ null。
     */
    fun onLocation(sample: LocationSample): Long? {
        val previous = previousSample
        previousSample = sample
        if (previous == null) return null

        // ほぼ同じ場所での方位は数値的に暴れるため、一定以上動いた区間だけを見る。
        if (Geo.distanceMeters(previous, sample) < MIN_SEGMENT_METERS) return null

        val bearing = Geo.bearingDegrees(previous, sample)
        val lastBearing = previousBearingDegrees
        previousBearingDegrees = bearing
        if (lastBearing == null) return null

        val turned = Geo.bearingDifferenceDegrees(lastBearing, bearing) >= config.turnAngleDegrees
        recentTurns.addLast(turned)
        while (recentTurns.size > WINDOW_SIZE) {
            recentTurns.removeFirst()
        }
        if (recentTurns.size < WINDOW_SIZE) return null

        val turning = recentTurns.count { it } >= TURN_THRESHOLD
        val next = if (turning) {
            config.locationIntervalTurningMillis
        } else {
            config.locationIntervalStraightMillis
        }
        if (next == intervalMillis) return null
        intervalMillis = next
        return next
    }

    fun reset() {
        previousSample = null
        previousBearingDegrees = null
        recentTurns = ArrayDeque()
        intervalMillis = config.locationIntervalStraightMillis
    }

    private companion object {
        /** 方位を計算する最小の区間長(m)。これ未満はGPSの揺れと区別できない。 */
        const val MIN_SEGMENT_METERS = 5.0

        /** 直近何区間を見るか。 */
        const val WINDOW_SIZE = 4

        /** 窓の中でいくつ曲がっていたら「カーブが多い」とみなすか。 */
        const val TURN_THRESHOLD = 2
    }
}
