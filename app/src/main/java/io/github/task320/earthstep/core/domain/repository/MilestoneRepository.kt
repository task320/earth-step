package io.github.task320.earthstep.core.domain.repository

import io.github.task320.earthstep.core.domain.milestone.Milestone
import io.github.task320.earthstep.core.domain.model.MilestoneAchievement
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * マイルストーンのマスタ参照と達成記録(P1-7)。
 *
 * 達成判定そのもの(複数同時達成の検出など)は P4-2 のユースケース側に置く。
 * ここは「マスタを引く」「達成を冪等に記録する」までを担う。
 */
interface MilestoneRepository {

    /** 100件のマスタ(距離の昇順)。 */
    val catalog: List<Milestone>

    /** 達成記録を番号の昇順で返す。 */
    val achievements: Flow<List<MilestoneAchievement>>

    /** 達成済みの番号の集合。 */
    val achievedIndexes: Flow<Set<Int>>

    /**
     * 達成を記録する。既に同じ番号が記録済みなら何もしない(P4-3 の冪等化をDBのPKで担保)。
     *
     * @return 新規に記録されたら true、既に記録済みなら false。
     */
    suspend fun recordAchievement(milestoneIndex: Int, achievedAt: Instant, distanceMeters: Long): Boolean
}
