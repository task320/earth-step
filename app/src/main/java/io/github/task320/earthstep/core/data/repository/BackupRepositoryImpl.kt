package io.github.task320.earthstep.core.data.repository

import androidx.room.withTransaction
import io.github.task320.earthstep.core.common.di.IoDispatcher
import io.github.task320.earthstep.core.common.time.AppTimeSource
import io.github.task320.earthstep.core.data.local.EarthStepDatabase
import io.github.task320.earthstep.core.data.local.entity.DailyLogEntity
import io.github.task320.earthstep.core.data.local.entity.LapRecordEntity
import io.github.task320.earthstep.core.data.local.entity.LifetimeStatsEntity
import io.github.task320.earthstep.core.data.local.entity.MilestoneAchievementEntity
import io.github.task320.earthstep.core.domain.backup.BackupDailyLog
import io.github.task320.earthstep.core.domain.backup.BackupData
import io.github.task320.earthstep.core.domain.backup.BackupLapRecord
import io.github.task320.earthstep.core.domain.backup.BackupLifetimeStats
import io.github.task320.earthstep.core.domain.backup.BackupMilestoneAchievement
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.model.LifetimeStats
import io.github.task320.earthstep.core.domain.repository.BackupRepository
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: EarthStepDatabase,
    private val timeSource: AppTimeSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {

    private val dailyLogDao = database.dailyLogDao()
    private val lifetimeStatsDao = database.lifetimeStatsDao()
    private val milestoneAchievementDao = database.milestoneAchievementDao()
    private val lapRecordDao = database.lapRecordDao()

    override suspend fun snapshot(): BackupData = withContext(ioDispatcher) {
        val stats = lifetimeStatsDao.get(LifetimeStatsEntity.SINGLETON_ID)
        BackupData(
            exportedAt = timeSource.now().toString(),
            lifetimeStats = BackupLifetimeStats(
                totalDistanceMeters = stats?.totalDistanceM ?: 0L,
                currentLap = stats?.currentLap ?: 1,
                strideLengthCm = stats?.strideLengthCm ?: LifetimeStats.DEFAULT_STRIDE_LENGTH_CM,
            ),
            dailyLog = dailyLogDao.getAll().map { entity ->
                BackupDailyLog(
                    date = entity.date,
                    distanceMeters = entity.distanceM,
                    updatedAt = Instant.ofEpochMilli(entity.updatedAt).toString(),
                )
            },
            milestoneAchievements = milestoneAchievementDao.getAll().map { entity ->
                BackupMilestoneAchievement(
                    milestoneIndex = entity.milestoneIndex,
                    achievedAt = Instant.ofEpochMilli(entity.achievedAt).toString(),
                    distanceMetersAtAchievement = entity.distanceMAtAchievement,
                )
            },
            lapRecords = lapRecordDao.getAll().map { entity ->
                BackupLapRecord(
                    lapNumber = entity.lapNumber,
                    completedAt = entity.completedAt?.let { Instant.ofEpochMilli(it).toString() },
                )
            },
        )
    }

    override suspend fun replaceAll(data: BackupData) = withContext(ioDispatcher) {
        database.withTransaction {
            dailyLogDao.deleteAll()
            milestoneAchievementDao.deleteAll()
            lapRecordDao.deleteAll()

            dailyLogDao.upsertAll(data.dailyLog.map { it.toEntity() })
            milestoneAchievementDao.insertAllIgnore(data.milestoneAchievements.map { it.toEntity() })
            lapRecordDao.insertAllIgnore(data.lapRecords.map { it.toEntity() })

            lifetimeStatsDao.insertIfAbsent(LifetimeStatsEntity())
            lifetimeStatsDao.setTotalDistance(
                data.lifetimeStats.totalDistanceMeters,
                LifetimeStatsEntity.SINGLETON_ID,
            )
            lifetimeStatsDao.setCurrentLap(
                data.lifetimeStats.currentLap,
                LifetimeStatsEntity.SINGLETON_ID,
            )
            lifetimeStatsDao.setStrideLengthCm(
                data.lifetimeStats.strideLengthCm,
                LifetimeStatsEntity.SINGLETON_ID,
            )
        }
    }

    private fun BackupDailyLog.toEntity() = DailyLogEntity(
        date = date,
        distanceM = distanceMeters,
        updatedAt = updatedAt.toEpochMillisOrZero(),
    )

    private fun BackupMilestoneAchievement.toEntity() = MilestoneAchievementEntity(
        milestoneIndex = milestoneIndex,
        achievedAt = achievedAt.toEpochMillisOrZero(),
        // 古いバックアップにはこの列が無い。閾値距離で埋めれば「その距離で達成した」ことは保てる。
        distanceMAtAchievement = distanceMetersAtAchievement
            ?: MilestoneCatalog.byIndex(milestoneIndex)?.distanceMeters
            ?: 0L,
    )

    private fun BackupLapRecord.toEntity() = LapRecordEntity(
        lapNumber = lapNumber,
        completedAt = completedAt?.toEpochMillisOrZero(),
    )

    /**
     * 読めない日時は 0 として扱う。
     * 日時が1件おかしいだけでインポート全体を失敗させるより、距離を守る方を優先する。
     */
    private fun String?.toEpochMillisOrZero(): Long {
        if (this.isNullOrBlank()) return 0L
        return try {
            Instant.parse(this).toEpochMilli()
        } catch (e: DateTimeParseException) {
            Timber.w(e, "could not parse timestamp: %s", this)
            0L
        }
    }
}
