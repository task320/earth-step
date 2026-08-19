package io.github.task320.earthstep.core.domain.milestone

/**
 * 100マイルストーンの1件(仕様3.2)。
 *
 * バッジ・ランクは廃止されたため(仕様3.3)、保持するのは番号・名称・距離・大台フラグ・説明のみ。
 *
 * @param index 1〜100。距離の昇順に並ぶ。
 * @param distanceMeters 到達に必要な累計距離(m)。
 * @param name 実在物の名称。
 * @param isMajor 大台演出(仕様3.4)の対象かどうか。
 * @param description 大台演出で表示する一言説明。未執筆のものは null(docs/TASKS.md 要確認事項1)。
 */
data class Milestone(
    val index: Int,
    val distanceMeters: Long,
    val name: String,
    val isMajor: Boolean = false,
    val description: String? = null,
)
