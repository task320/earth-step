package io.github.task320.earthstep.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 仕様6.1 `milestone_achievement`。
 * 主キーがマイルストーン番号そのものなので、二重記録はDBレベルで弾かれる(P4-3)。
 */
@Entity(tableName = "milestone_achievement")
data class MilestoneAchievementEntity(
    @PrimaryKey
    @ColumnInfo(name = "milestone_index")
    val milestoneIndex: Int,
    /** 達成日時(epoch millis)。 */
    @ColumnInfo(name = "achieved_at")
    val achievedAt: Long,
    @ColumnInfo(name = "distance_m_at_achievement")
    val distanceMAtAchievement: Long,
)
