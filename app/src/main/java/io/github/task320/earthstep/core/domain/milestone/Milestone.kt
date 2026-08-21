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
 * @param description 実在物についての一言説明。達成記録画面での詳細表示と大台演出で使う。
 *   名称には「世界一」「日本一」等の順位表現を含めない(将来抜かれて実情と食い違うため)。
 *   説明文中で順位に触れる場合、人工物の記録(将来抜かれうるもの)には「2026年現在」のように
 *   時点を明示する。自然地形や国境など普遍的な事実は明示なしで書いてよい。
 */
data class Milestone(
    val index: Int,
    val distanceMeters: Long,
    val name: String,
    val isMajor: Boolean = false,
    val description: String? = null,
)
