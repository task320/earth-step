package io.github.task320.earthstep.core.domain.progress

import io.github.task320.earthstep.core.domain.milestone.Milestone

/**
 * 距離が増えたことで起きた出来事(P4-2 / P4-5 / P4-6)。
 *
 * 1回の距離更新で複数同時に起きうるため、必ず距離の昇順に並べて扱う。
 * 演出(P5-7/P5-9)と通知(P5-12)はこの並び順のまま再生する。
 */
sealed interface ProgressEvent {

    /** その出来事が起きた時点の生涯累計距離(m)。 */
    val totalDistanceMeters: Long

    /** 大台演出(仕様3.4)の対象かどうか。 */
    val isMajor: Boolean

    /** マイルストーンに到達した。 */
    data class MilestoneAchieved(val milestone: Milestone, override val totalDistanceMeters: Long) : ProgressEvent {
        override val isMajor: Boolean get() = milestone.isMajor
    }

    /** 周内マーカー(25/50/75%)に到達した。3種とも大台演出を使う(仕様4.2)。 */
    data class LapMarkerReached(val marker: LapMarker, val lapNumber: Int, override val totalDistanceMeters: Long) :
        ProgressEvent {
        override val isMajor: Boolean get() = true
    }

    /** 1周を走破した。 */
    data class LapCompleted(val lapNumber: Int, override val totalDistanceMeters: Long) : ProgressEvent {
        /** 周回の完了は必ず大台として見せる。 */
        override val isMajor: Boolean get() = true
    }
}
