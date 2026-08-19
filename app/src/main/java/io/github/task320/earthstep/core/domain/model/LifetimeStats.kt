package io.github.task320.earthstep.core.domain.model

/**
 * 生涯累計の統計(仕様6.1 `lifetime_stats`)。端末に1件だけ存在する。
 *
 * @param totalDistanceMeters 生涯累計距離(m)。仕様2.1より、この値がそのままXPになる。
 * @param currentLap 現在の周回数。1周目は 1。
 * @param strideLengthCm 自己較正した歩幅(cm)。GPS区間から算出する(仕様1.3)。
 */
data class LifetimeStats(val totalDistanceMeters: Long, val currentLap: Int, val strideLengthCm: Double) {
    companion object {
        /** 較正前の既定歩幅(cm)。仕様1.3/P2-14 の初期値。 */
        const val DEFAULT_STRIDE_LENGTH_CM = 70.0

        val INITIAL = LifetimeStats(
            totalDistanceMeters = 0L,
            currentLap = 1,
            strideLengthCm = DEFAULT_STRIDE_LENGTH_CM,
        )
    }
}
