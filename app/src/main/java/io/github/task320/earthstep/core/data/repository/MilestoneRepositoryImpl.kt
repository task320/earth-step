package io.github.task320.earthstep.core.data.repository

import io.github.task320.earthstep.core.common.di.IoDispatcher
import io.github.task320.earthstep.core.data.local.dao.MilestoneAchievementDao
import io.github.task320.earthstep.core.data.local.entity.MilestoneAchievementEntity
import io.github.task320.earthstep.core.domain.milestone.Milestone
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.model.MilestoneAchievement
import io.github.task320.earthstep.core.domain.repository.MilestoneRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class MilestoneRepositoryImpl @Inject constructor(
    private val milestoneAchievementDao: MilestoneAchievementDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MilestoneRepository {

    override val catalog: List<Milestone> = MilestoneCatalog.milestones

    override val achievements: Flow<List<MilestoneAchievement>> =
        milestoneAchievementDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override val achievedIndexes: Flow<Set<Int>> =
        milestoneAchievementDao.observeAchievedIndexes().map { it.toSet() }

    override suspend fun recordAchievement(milestoneIndex: Int, achievedAt: Instant, distanceMeters: Long): Boolean =
        withContext(ioDispatcher) {
            requireNotNull(MilestoneCatalog.byIndex(milestoneIndex)) {
                "unknown milestone index: $milestoneIndex"
            }
            val rowId = milestoneAchievementDao.insertIgnore(
                MilestoneAchievementEntity(
                    milestoneIndex = milestoneIndex,
                    achievedAt = achievedAt.toEpochMilli(),
                    distanceMAtAchievement = distanceMeters,
                ),
            )
            rowId != IGNORED_ROW_ID
        }

    private companion object {
        /** `OnConflictStrategy.IGNORE` で挿入がスキップされたときの戻り値。 */
        const val IGNORED_ROW_ID = -1L
    }
}

private fun MilestoneAchievementEntity.toDomain(): MilestoneAchievement = MilestoneAchievement(
    milestoneIndex = milestoneIndex,
    achievedAt = Instant.ofEpochMilli(achievedAt),
    distanceMetersAtAchievement = distanceMAtAchievement,
)
