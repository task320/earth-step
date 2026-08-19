package io.github.task320.earthstep.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.data.local.EarthStepDatabase
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** P1-2 / P1-7: 達成記録の永続化と冪等性(P4-3 の土台)。 */
@RunWith(RobolectricTestRunner::class)
class MilestoneRepositoryImplTest {

    private lateinit var database: EarthStepDatabase
    private lateinit var repository: MilestoneRepositoryImpl

    private val achievedAt = Instant.parse("2026-08-19T03:00:00Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EarthStepDatabase::class.java,
        )
            .setQueryExecutor(Dispatchers.IO.asExecutor())
            .setTransactionExecutor(Dispatchers.IO.asExecutor())
            .build()
        repository = MilestoneRepositoryImpl(database.milestoneAchievementDao(), Dispatchers.IO)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `マスタは100件をそのまま公開する`() {
        assertThat(repository.catalog).isEqualTo(MilestoneCatalog.milestones)
    }

    @Test
    fun `達成の記録は初回だけ成功し二度目は無視される`() = runBlocking<Unit> {
        assertThat(repository.recordAchievement(1, achievedAt, 300L)).isTrue()
        assertThat(repository.recordAchievement(1, achievedAt.plusSeconds(60), 900L)).isFalse()

        val achievements = repository.achievements.first()
        assertThat(achievements).hasSize(1)
        assertThat(achievements.single().achievedAt).isEqualTo(achievedAt)
        assertThat(achievements.single().distanceMetersAtAchievement).isEqualTo(300L)
    }

    @Test
    fun `達成済みの番号を昇順で流す`() = runBlocking<Unit> {
        repository.achievedIndexes.test {
            assertThat(awaitItem()).isEmpty()

            repository.recordAchievement(2, achievedAt, 333L)
            repository.recordAchievement(1, achievedAt, 300L)

            // Room の無効化通知はまとめられることがあるため、期待する集合に達するまで読み進める。
            var latest = awaitItem()
            while (latest != setOf(1, 2)) {
                latest = awaitItem()
            }
            assertThat(latest).containsExactly(1, 2).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `複数の達成を記録すると番号順に並ぶ`() = runBlocking<Unit> {
        repository.recordAchievement(3, achievedAt, 600L)
        repository.recordAchievement(1, achievedAt, 600L)
        repository.recordAchievement(2, achievedAt, 600L)

        assertThat(repository.achievements.first().map { it.milestoneIndex })
            .containsExactly(1, 2, 3).inOrder()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `マスタに無い番号は記録できない`() = runBlocking<Unit> {
        repository.recordAchievement(MilestoneCatalog.SIZE + 1, achievedAt, 1L)
    }
}
