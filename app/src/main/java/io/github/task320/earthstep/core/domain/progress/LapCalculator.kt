package io.github.task320.earthstep.core.domain.progress

/**
 * 生涯累計距離のうち、いま何周目のどこにいるか(仕様4.1)。
 *
 * @param lapNumber 現在の周回。1周目は 1。
 * @param distanceInLapMeters 今の周に入ってから進んだ距離(m)。
 * @param completedLaps 走破し終えた周の数。
 */
data class LapProgress(val lapNumber: Int, val distanceInLapMeters: Long, val completedLaps: Int) {
    /** 今の周の進捗(0.0〜1.0)。 */
    val ratio: Float get() = distanceInLapMeters.toFloat() / Earth.CIRCUMFERENCE_METERS

    /** 今の周の残り距離(m)。 */
    val remainingInLapMeters: Long get() = Earth.CIRCUMFERENCE_METERS - distanceInLapMeters
}

/**
 * 周回の計算(P4-5 / P4-6)。
 *
 * 周回は累計距離から一意に決まるため、状態を持たない純粋関数として書く。
 * 「加算のたびにカウンタを+1する」持ち方をすると、加算の取りこぼしや二重処理で
 * 累計距離と周回数がずれてしまう。累計距離を唯一の真実にしておけば、
 * 何度計算し直しても同じ答えになる。
 */
object LapCalculator {

    /** 累計 [totalMeters] の時点の周回状態。 */
    fun progressOf(totalMeters: Long): LapProgress {
        require(totalMeters >= 0) { "distance must not be negative: $totalMeters" }
        val completed = (totalMeters / Earth.CIRCUMFERENCE_METERS).toInt()
        return LapProgress(
            lapNumber = completed + 1,
            distanceInLapMeters = totalMeters % Earth.CIRCUMFERENCE_METERS,
            completedLaps = completed,
        )
    }

    /** 走破し終えた周の数。 */
    fun completedLaps(totalMeters: Long): Int = progressOf(totalMeters).completedLaps

    /**
     * [previousMeters] から [currentMeters] までの間に走破した周の番号。
     * 1回の更新で2周以上進む場合もすべて返す(P4-5)。
     */
    fun lapsCompletedBetween(previousMeters: Long, currentMeters: Long): List<Int> {
        if (currentMeters <= previousMeters) return emptyList()
        val from = completedLaps(previousMeters)
        val to = completedLaps(currentMeters)
        // 完了周数が n から m へ増えたなら、走破したのは n+1 周目から m 周目まで。
        return ((from + 1)..to).toList()
    }

    /**
     * [previousMeters] から [currentMeters] までの間に到達した周内マーカー(P4-6)。
     * 複数周をまたぐ場合は、通過した周ぶんすべてを距離順に返す。
     */
    fun markersReachedBetween(previousMeters: Long, currentMeters: Long): List<ReachedLapMarker> {
        if (currentMeters <= previousMeters) return emptyList()
        val firstLapIndex = completedLaps(previousMeters)
        val lastLapIndex = completedLaps(currentMeters)

        return (firstLapIndex..lastLapIndex).flatMap { lapIndex ->
            val lapStart = lapIndex * Earth.CIRCUMFERENCE_METERS
            LapMarker.entries.map { marker ->
                ReachedLapMarker(
                    marker = marker,
                    lapNumber = lapIndex + 1,
                    totalDistanceMeters = lapStart + marker.distanceInLapMeters,
                )
            }
        }.filter { it.totalDistanceMeters in (previousMeters + 1)..currentMeters }
            .sortedBy { it.totalDistanceMeters }
    }

    /** [totalMeters] の次に来る周内マーカー。今の周にもう無ければ null。 */
    fun nextMarkerAfter(totalMeters: Long): LapMarker? {
        val distanceInLap = progressOf(totalMeters).distanceInLapMeters
        return LapMarker.entries.firstOrNull { it.distanceInLapMeters > distanceInLap }
    }
}

/**
 * 到達した周内マーカー。
 *
 * @param totalDistanceMeters 到達した時点の生涯累計距離(m)。
 */
data class ReachedLapMarker(val marker: LapMarker, val lapNumber: Int, val totalDistanceMeters: Long)
