package io.github.task320.earthstep.core.data.repository

import androidx.room.withTransaction
import io.github.task320.earthstep.core.common.di.IoDispatcher
import io.github.task320.earthstep.core.common.time.AppTimeSource
import io.github.task320.earthstep.core.common.time.DayKey
import io.github.task320.earthstep.core.common.time.DayTicker
import io.github.task320.earthstep.core.data.local.EarthStepDatabase
import io.github.task320.earthstep.core.data.local.entity.LapRecordEntity
import io.github.task320.earthstep.core.data.local.entity.LifetimeStatsEntity
import io.github.task320.earthstep.core.domain.model.DailyDistance
import io.github.task320.earthstep.core.domain.model.LapRecord
import io.github.task320.earthstep.core.domain.model.LifetimeStats
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val database: EarthStepDatabase,
    private val timeSource: AppTimeSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProgressRepository {

    private val dailyLogDao = database.dailyLogDao()
    private val lifetimeStatsDao = database.lifetimeStatsDao()
    private val lapRecordDao = database.lapRecordDao()

    override val lifetimeStats: Flow<LifetimeStats> =
        lifetimeStatsDao.observe(LifetimeStatsEntity.SINGLETON_ID)
            .map { it?.toDomain() ?: LifetimeStats.INITIAL }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val todayDistanceMeters: Flow<Long> =
        DayTicker.dates(timeSource).flatMapLatest { distanceMetersOn(it) }

    override val lapRecords: Flow<List<LapRecord>> =
        lapRecordDao.observeAll().map { records -> records.map { it.toDomain() } }

    override fun distanceMetersOn(date: LocalDate): Flow<Long> = dailyLogDao.observeDistance(DayKey.of(date))
        .map { it ?: 0L }
        .distinctUntilChanged()

    override fun dailyDistances(from: LocalDate, to: LocalDate): Flow<List<DailyDistance>> =
        dailyLogDao.observeRange(DayKey.of(from), DayKey.of(to))
            .map { logs -> logs.map { DailyDistance(DayKey.parse(it.date), it.distanceM) } }

    override suspend fun addDistance(meters: Long, at: Instant): Long = withContext(ioDispatcher) {
        if (meters <= 0L) {
            return@withContext currentTotal()
        }
        val dateKey = DayKey.of(timeSource.dateOf(at))
        database.withTransaction {
            ensureStatsRow()
            val updatedAt = at.toEpochMilli()
            dailyLogDao.insertIfAbsent(date = dateKey, updatedAt = updatedAt)
            dailyLogDao.incrementDistance(date = dateKey, meters = meters, updatedAt = updatedAt)
            lifetimeStatsDao.addDistance(meters, LifetimeStatsEntity.SINGLETON_ID)
            currentTotal()
        }
    }

    override suspend fun setCurrentLap(lap: Int) = withContext(ioDispatcher) {
        require(lap >= 1) { "lap must be positive: $lap" }
        database.withTransaction {
            ensureStatsRow()
            lifetimeStatsDao.setCurrentLap(lap, LifetimeStatsEntity.SINGLETON_ID)
        }
    }

    override suspend fun recordLapCompleted(lapNumber: Int, completedAt: Instant) = withContext(ioDispatcher) {
        require(lapNumber >= 1) { "lapNumber must be positive: $lapNumber" }
        lapRecordDao.insertIgnore(
            LapRecordEntity(lapNumber = lapNumber, completedAt = completedAt.toEpochMilli()),
        )
        Unit
    }

    override suspend fun setStrideLengthCm(strideLengthCm: Double) = withContext(ioDispatcher) {
        require(strideLengthCm > 0.0) { "strideLengthCm must be positive: $strideLengthCm" }
        database.withTransaction {
            ensureStatsRow()
            lifetimeStatsDao.setStrideLengthCm(strideLengthCm, LifetimeStatsEntity.SINGLETON_ID)
        }
    }

    override suspend fun recalculateTotalFromDailyLogs(): Long = withContext(ioDispatcher) {
        database.withTransaction {
            ensureStatsRow()
            val total = dailyLogDao.sumAll()
            lifetimeStatsDao.setTotalDistance(total, LifetimeStatsEntity.SINGLETON_ID)
            total
        }
    }

    /** 単一行が無ければ既定値で作る。更新系はすべてUPDATE文なので、先に行の存在を保証する。 */
    private suspend fun ensureStatsRow() {
        lifetimeStatsDao.insertIfAbsent(LifetimeStatsEntity())
    }

    private suspend fun currentTotal(): Long =
        lifetimeStatsDao.get(LifetimeStatsEntity.SINGLETON_ID)?.totalDistanceM ?: 0L
}

private fun LifetimeStatsEntity.toDomain(): LifetimeStats = LifetimeStats(
    totalDistanceMeters = totalDistanceM,
    currentLap = currentLap,
    strideLengthCm = strideLengthCm,
)

private fun LapRecordEntity.toDomain(): LapRecord = LapRecord(
    lapNumber = lapNumber,
    completedAt = completedAt?.let(Instant::ofEpochMilli),
)
