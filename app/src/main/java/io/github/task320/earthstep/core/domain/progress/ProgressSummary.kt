package io.github.task320.earthstep.core.domain.progress

import io.github.task320.earthstep.core.domain.milestone.Milestone
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog

/**
 * 画面に出す進捗をひとまとめにしたもの(P4-4 / P4-7)。
 *
 * 累計距離から導ける値はすべてここで計算する。累計距離を唯一の入力にしておけば、
 * 表示のどこかだけが古い、という食い違いが起きない。
 *
 * @param nextMilestone 次に到達するマイルストーン。100個すべて達成済みなら null。
 * @param remainingToNextMilestoneMeters 次のマイルストーンまでの残り(m)。達成済みなら 0。
 * @param milestoneRatio 直前のマイルストーンから次までの進捗(0.0〜1.0)。
 */
data class ProgressSummary(
    val totalDistanceMeters: Long = 0L,
    val todayDistanceMeters: Long = 0L,
    val lapProgress: LapProgress = LapCalculator.progressOf(0L),
    val achievedMilestoneCount: Int = 0,
    val nextMilestone: Milestone? = null,
    val remainingToNextMilestoneMeters: Long = 0L,
    val milestoneRatio: Float = 0f,
    val nextLapMarker: LapMarker? = null,
) {

    /** 経験値(仕様2.1)。 */
    val xp: Long get() = Xp.fromMeters(totalDistanceMeters)

    /** 今の周回の見た目(仕様4.3)。 */
    val lapSkin: LapSkin get() = LapSkin.forLap(lapProgress.lapNumber)

    /**
     * 100マイルストーンの一覧を出すか(仕様4.1 / P4-7)。
     *
     * 2周目以降は再提示せず、周回カウンターと周内マーカーだけのシンプルな見せ方に切り替える。
     * 判定を「2周目に入ったか」ではなく「未達成が残っているか」にしているのは、
     * 100個目に到達した瞬間と1周を走破した瞬間が同じ距離だから。
     * どちらの表現でも同じ結果になるが、達成状況を直接見る方が意図が読める。
     */
    val showsMilestoneList: Boolean get() = achievedMilestoneCount < MilestoneCatalog.SIZE

    companion object {

        /** 累計距離と当日距離から表示用の値を組み立てる。 */
        fun of(totalDistanceMeters: Long, todayDistanceMeters: Long): ProgressSummary {
            val next = MilestoneCatalog.nextAfter(totalDistanceMeters)
            val achievedCount = MilestoneCatalog.achievedCount(totalDistanceMeters)
            val previousDistance = MilestoneCatalog.byIndex(achievedCount)?.distanceMeters ?: 0L

            return ProgressSummary(
                totalDistanceMeters = totalDistanceMeters,
                todayDistanceMeters = todayDistanceMeters,
                lapProgress = LapCalculator.progressOf(totalDistanceMeters),
                achievedMilestoneCount = achievedCount,
                nextMilestone = next,
                remainingToNextMilestoneMeters = next?.let { it.distanceMeters - totalDistanceMeters } ?: 0L,
                milestoneRatio = milestoneRatio(totalDistanceMeters, previousDistance, next),
                nextLapMarker = LapCalculator.nextMarkerAfter(totalDistanceMeters),
            )
        }

        /**
         * 直前のマイルストーンから次までの進捗。
         * 累計距離をそのまま次の距離で割ると、序盤の間隔が狭い区間で
         * バーがほとんど動かないように見えるため、区間の中での割合にする。
         */
        private fun milestoneRatio(totalMeters: Long, previousMeters: Long, next: Milestone?): Float {
            if (next == null) return 1f
            val span = next.distanceMeters - previousMeters
            if (span <= 0L) return 1f
            return ((totalMeters - previousMeters).toFloat() / span).coerceIn(0f, 1f)
        }
    }
}
