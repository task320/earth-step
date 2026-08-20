package io.github.task320.earthstep.feature.celebration

import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.progress.LapMarker

/**
 * 演出で表示する文言(仕様3.4 / 4.2 / P5-10 / P5-11)。
 *
 * 文字列リソースIDと差し込む値だけを持ち、実際の解決は Composable 側で行う。
 * こうしておくと文言の組み立てを Android 無しでテストできる。
 *
 * @param titleRes 見出し。
 * @param name マイルストーン名など、見出しに差し込む語。
 * @param descriptionRes 大台演出で出す一言説明。無ければ null。
 */
data class CelebrationContent(val titleRes: Int, val name: String, val descriptionRes: Int?, val isMajor: Boolean) {
    companion object {

        /**
         * 演出1件ぶんの文言を決める。
         *
         * マイルストーンの一言説明(仕様3.4)は未執筆のため、
         * 現状 [descriptionRes] が埋まるのは周内マーカーだけ。
         * 文面が決まったら `Milestone.description` から引くように変える。
         */
        fun of(celebration: PendingCelebration): CelebrationContent = when (celebration) {
            is PendingCelebration.Milestone -> {
                val milestone = MilestoneCatalog.byIndex(celebration.milestoneIndex)
                CelebrationContent(
                    titleRes = R.string.celebration_milestone_title,
                    name = milestone?.name.orEmpty(),
                    descriptionRes = null,
                    isMajor = milestone?.isMajor == true,
                )
            }

            is PendingCelebration.Marker -> CelebrationContent(
                titleRes = R.string.celebration_marker_title,
                name = "",
                descriptionRes = celebration.marker.descriptionRes(),
                isMajor = true,
            )

            is PendingCelebration.Lap -> CelebrationContent(
                titleRes = R.string.celebration_lap_title,
                name = celebration.lapNumber.toString(),
                descriptionRes = R.string.celebration_lap_description,
                isMajor = true,
            )
        }

        /** 周内マーカーの見出しに差し込む割合表示。 */
        fun markerLabel(marker: LapMarker): String = when (marker) {
            LapMarker.QUARTER -> "25%"
            LapMarker.HALF -> "50%"
            LapMarker.THREE_QUARTERS -> "75%"
        }
    }
}

/**
 * 周内マーカーの一言説明(仕様4.2)。
 * 75% は説明を持たず、数字のインパクトだけで見せる。
 */
private fun LapMarker.descriptionRes(): Int? = when (this) {
    LapMarker.QUARTER -> R.string.celebration_marker_quarter_description
    LapMarker.HALF -> R.string.celebration_marker_half_description
    LapMarker.THREE_QUARTERS -> null
}
