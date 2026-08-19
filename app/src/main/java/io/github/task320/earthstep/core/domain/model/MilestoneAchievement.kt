package io.github.task320.earthstep.core.domain.model

import java.time.Instant

/**
 * マイルストーン達成記録(仕様6.1 `milestone_achievement`)。
 *
 * @param milestoneIndex 1〜100。[io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog] の番号。
 * @param achievedAt 達成日時。
 * @param distanceMetersAtAchievement 達成時点の累計距離(m)。閾値をどれだけ超えて達成したかを保持する。
 */
data class MilestoneAchievement(
    val milestoneIndex: Int,
    val achievedAt: Instant,
    val distanceMetersAtAchievement: Long,
)
