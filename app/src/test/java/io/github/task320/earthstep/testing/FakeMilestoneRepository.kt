package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.milestone.Milestone
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.model.MilestoneAchievement
import io.github.task320.earthstep.core.domain.repository.MilestoneRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** メモリ上だけで完結する [MilestoneRepository]。主キーによる冪等性も再現する。 */
class FakeMilestoneRepository : MilestoneRepository {

    private val state = MutableStateFlow<Map<Int, MilestoneAchievement>>(emptyMap())

    override val catalog: List<Milestone> = MilestoneCatalog.milestones

    override val achievements: Flow<List<MilestoneAchievement>> =
        state.map { it.values.sortedBy(MilestoneAchievement::milestoneIndex) }

    override val achievedIndexes: Flow<Set<Int>> = state.map { it.keys.toSet() }

    override suspend fun recordAchievement(milestoneIndex: Int, achievedAt: Instant, distanceMeters: Long): Boolean {
        requireNotNull(MilestoneCatalog.byIndex(milestoneIndex)) {
            "unknown milestone index: $milestoneIndex"
        }
        if (state.value.containsKey(milestoneIndex)) return false
        state.value = state.value + (
            milestoneIndex to MilestoneAchievement(
                milestoneIndex = milestoneIndex,
                achievedAt = achievedAt,
                distanceMetersAtAchievement = distanceMeters,
            )
            )
        return true
    }

    fun recordedIndexes(): List<Int> = state.value.keys.sorted()
}
