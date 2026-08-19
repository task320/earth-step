package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.model.DailyDistance
import io.github.task320.earthstep.core.domain.model.LapRecord
import io.github.task320.earthstep.core.domain.model.LifetimeStats
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** メモリ上だけで完結する [ProgressRepository]。ViewModel のテストで使う。 */
class FakeProgressRepository(initialStats: LifetimeStats = LifetimeStats.INITIAL) : ProgressRepository {

    private val statsState = MutableStateFlow(initialStats)
    private val dailyState = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())
    private val lapState = MutableStateFlow<List<LapRecord>>(emptyList())

    /** 当日として扱う日付。 */
    var today: LocalDate = LocalDate.of(2026, 8, 19)

    override val lifetimeStats: Flow<LifetimeStats> = statsState

    override val todayDistanceMeters: Flow<Long> = dailyState.map { it[today] ?: 0L }

    override val lapRecords: Flow<List<LapRecord>> = lapState

    override fun distanceMetersOn(date: LocalDate): Flow<Long> = dailyState.map { it[date] ?: 0L }

    override fun dailyDistances(from: LocalDate, to: LocalDate): Flow<List<DailyDistance>> = dailyState.map { logs ->
        logs.filterKeys { it >= from && it <= to }
            .toSortedMap()
            .map { (date, meters) -> DailyDistance(date, meters) }
    }

    override suspend fun addDistance(meters: Long, at: Instant): Long {
        if (meters <= 0L) return statsState.value.totalDistanceMeters
        dailyState.value = dailyState.value.toMutableMap().apply {
            this[today] = (this[today] ?: 0L) + meters
        }
        statsState.value = statsState.value.copy(
            totalDistanceMeters = statsState.value.totalDistanceMeters + meters,
        )
        return statsState.value.totalDistanceMeters
    }

    override suspend fun setCurrentLap(lap: Int) {
        statsState.value = statsState.value.copy(currentLap = lap)
    }

    override suspend fun recordLapCompleted(lapNumber: Int, completedAt: Instant) {
        if (lapState.value.none { it.lapNumber == lapNumber }) {
            lapState.value = lapState.value + LapRecord(lapNumber, completedAt)
        }
    }

    override suspend fun setStrideLengthCm(strideLengthCm: Double) {
        statsState.value = statsState.value.copy(strideLengthCm = strideLengthCm)
    }

    override suspend fun recalculateTotalFromDailyLogs(): Long {
        val total = dailyState.value.values.sum()
        statsState.value = statsState.value.copy(totalDistanceMeters = total)
        return total
    }
}
