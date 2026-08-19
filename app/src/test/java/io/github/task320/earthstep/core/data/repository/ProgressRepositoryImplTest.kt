package io.github.task320.earthstep.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.data.local.EarthStepDatabase
import io.github.task320.earthstep.core.domain.model.LifetimeStats
import io.github.task320.earthstep.testing.FakeTimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P1-2 / P1-7 / P1-8: 距離の加算・参照と日付境界の扱い。
 *
 * Room の Flow と `withTransaction` は実際のスレッドプール上で動くため、
 * 仮想時間の `runTest` ではなく `runBlocking` で実行する。
 */
@RunWith(RobolectricTestRunner::class)
class ProgressRepositoryImplTest {

    private lateinit var database: EarthStepDatabase
    private lateinit var repository: ProgressRepositoryImpl

    private val timeSource = FakeTimeSource(
        current = Instant.parse("2026-08-19T03:00:00Z"), // 東京では 8/19 12:00
        zoneId = ZoneId.of("Asia/Tokyo"),
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EarthStepDatabase::class.java,
        )
            .setQueryExecutor(Dispatchers.IO.asExecutor())
            .setTransactionExecutor(Dispatchers.IO.asExecutor())
            .build()
        repository = ProgressRepositoryImpl(database, timeSource, Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `加算した距離が日別ログと累計の両方に入る`() = runBlocking<Unit> {
        val total = repository.addDistance(1_200L, timeSource.now())

        assertThat(total).isEqualTo(1_200L)
        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 19)).first()).isEqualTo(1_200L)
        assertThat(repository.lifetimeStats.first().totalDistanceMeters).isEqualTo(1_200L)
    }

    @Test
    fun `同じ日への加算は合算される`() = runBlocking<Unit> {
        repository.addDistance(500L, timeSource.now())
        repository.addDistance(700L, timeSource.now())
        val total = repository.addDistance(300L, timeSource.now())

        assertThat(total).isEqualTo(1_500L)
        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 19)).first()).isEqualTo(1_500L)
    }

    @Test
    fun `0以下の距離は無視され累計も日別ログも変わらない`() = runBlocking<Unit> {
        repository.addDistance(400L, timeSource.now())

        assertThat(repository.addDistance(0L, timeSource.now())).isEqualTo(400L)
        assertThat(repository.addDistance(-100L, timeSource.now())).isEqualTo(400L)
        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 19)).first()).isEqualTo(400L)
        assertThat(repository.lifetimeStats.first().totalDistanceMeters).isEqualTo(400L)
    }

    @Test
    fun `ローカル日付をまたぐと別の日として記録される`() = runBlocking<Unit> {
        // 東京の 8/19 23:59 と 8/20 00:01。
        repository.addDistance(1_000L, Instant.parse("2026-08-19T14:59:00Z"))
        repository.addDistance(2_000L, Instant.parse("2026-08-19T15:01:00Z"))

        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 19)).first()).isEqualTo(1_000L)
        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 20)).first()).isEqualTo(2_000L)
        assertThat(repository.lifetimeStats.first().totalDistanceMeters).isEqualTo(3_000L)
    }

    @Test
    fun `UTCの日付が変わっても東京では同じ日にまとまる`() = runBlocking<Unit> {
        // どちらも東京では 8/20(UTCでは 8/19 と 8/20 にまたがる)。
        repository.addDistance(800L, Instant.parse("2026-08-19T23:30:00Z"))
        repository.addDistance(900L, Instant.parse("2026-08-20T00:30:00Z"))

        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 20)).first()).isEqualTo(1_700L)
        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 19)).first()).isEqualTo(0L)
    }

    @Test
    fun `タイムゾーンが変わると以後の加算は新しいゾーンの日付へ入る`() = runBlocking<Unit> {
        val instant = Instant.parse("2026-08-19T15:30:00Z") // 東京 8/20 00:30 / UTC 8/19 15:30
        repository.addDistance(600L, instant)
        timeSource.zoneId = ZoneId.of("UTC")
        repository.addDistance(600L, instant)

        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 20)).first()).isEqualTo(600L)
        assertThat(repository.distanceMetersOn(LocalDate.of(2026, 8, 19)).first()).isEqualTo(600L)
    }

    @Test
    fun `当日距離のFlowは加算のたびに更新される`() = runBlocking<Unit> {
        repository.todayDistanceMeters.test {
            assertThat(awaitItem()).isEqualTo(0L)

            repository.addDistance(250L, timeSource.now())
            assertThat(awaitItem()).isEqualTo(250L)

            repository.addDistance(750L, timeSource.now())
            assertThat(awaitItem()).isEqualTo(1_000L)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `記録が無いときは初期値の統計を流す`() = runBlocking<Unit> {
        assertThat(repository.lifetimeStats.first()).isEqualTo(LifetimeStats.INITIAL)
    }

    @Test
    fun `周回数と歩幅を保存できる`() = runBlocking<Unit> {
        repository.setCurrentLap(3)
        repository.setStrideLengthCm(68.5)

        val stats = repository.lifetimeStats.first()
        assertThat(stats.currentLap).isEqualTo(3)
        assertThat(stats.strideLengthCm).isEqualTo(68.5)
    }

    @Test
    fun `同じ周回の完了記録は二重に入らない`() = runBlocking<Unit> {
        val first = Instant.parse("2026-08-19T03:00:00Z")
        repository.recordLapCompleted(1, first)
        repository.recordLapCompleted(1, first.plusSeconds(60))
        repository.recordLapCompleted(2, first.plusSeconds(120))

        val laps = repository.lapRecords.first()
        assertThat(laps.map { it.lapNumber }).containsExactly(1, 2).inOrder()
        assertThat(laps.first().completedAt).isEqualTo(first)
    }

    @Test
    fun `累計は日別ログの合計から再計算できる`() = runBlocking<Unit> {
        repository.addDistance(1_000L, Instant.parse("2026-08-19T03:00:00Z"))
        repository.addDistance(2_500L, Instant.parse("2026-08-20T03:00:00Z"))
        // 同期の競合解決で累計だけがずれた状況を作る。
        repository.setCurrentLap(1)
        database.lifetimeStatsDao().setTotalDistance(999_999L, 1)

        assertThat(repository.recalculateTotalFromDailyLogs()).isEqualTo(3_500L)
        assertThat(repository.lifetimeStats.first().totalDistanceMeters).isEqualTo(3_500L)
    }

    @Test
    fun `期間指定で日別距離を日付順に取得できる`() = runBlocking<Unit> {
        repository.addDistance(100L, Instant.parse("2026-08-18T03:00:00Z"))
        repository.addDistance(200L, Instant.parse("2026-08-19T03:00:00Z"))
        repository.addDistance(300L, Instant.parse("2026-08-20T03:00:00Z"))

        val range = repository.dailyDistances(
            from = LocalDate.of(2026, 8, 18),
            to = LocalDate.of(2026, 8, 19),
        ).first()

        assertThat(range.map { it.date }).containsExactly(
            LocalDate.of(2026, 8, 18),
            LocalDate.of(2026, 8, 19),
        ).inOrder()
        assertThat(range.map { it.distanceMeters }).containsExactly(100L, 200L).inOrder()
    }
}
