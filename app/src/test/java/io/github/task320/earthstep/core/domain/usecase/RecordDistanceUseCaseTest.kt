package io.github.task320.earthstep.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.progress.Earth
import io.github.task320.earthstep.core.domain.progress.LapMarker
import io.github.task320.earthstep.core.domain.progress.ProgressEvent
import io.github.task320.earthstep.testing.FakeMilestoneRepository
import io.github.task320.earthstep.testing.FakeProgressRepository
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * P4-9: 距離を段階的に注入し、達成順・複数同時達成・周回繰り上げ・マーカー発火を検証する。
 */
class RecordDistanceUseCaseTest {

    private val progressRepository = FakeProgressRepository()
    private val milestoneRepository = FakeMilestoneRepository()
    private val recordDistance = RecordDistanceUseCase(progressRepository, milestoneRepository)

    private val at = Instant.parse("2026-08-20T03:00:00Z")
    private val lap = Earth.CIRCUMFERENCE_METERS

    @Test
    fun `0以下の距離では何も起きない`() = runTest {
        assertThat(recordDistance(0L, at)).isEmpty()
        assertThat(recordDistance(-10L, at)).isEmpty()
        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(0L)
    }

    @Test
    fun `マイルストーンに届かない距離では達成が起きない`() {
        runTest {
            val events = recordDistance(299L, at)

            assertThat(events).isEmpty()
            assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(299L)
        }
    }

    @Test
    fun `閾値ちょうどで達成する`() = runTest {
        val events = recordDistance(300L, at)

        assertThat(events).hasSize(1)
        val achieved = events.single() as ProgressEvent.MilestoneAchieved
        assertThat(achieved.milestone.index).isEqualTo(1)
        assertThat(milestoneRepository.recordedIndexes()).containsExactly(1)
    }

    @Test
    fun `段階的に注入すると達成が距離の順に起きる`() = runTest {
        val achievedIndexes = mutableListOf<Int>()
        listOf(300L, 33L, 208L, 12L).forEach { meters ->
            recordDistance(meters, at)
                .filterIsInstance<ProgressEvent.MilestoneAchieved>()
                .forEach { achievedIndexes += it.milestone.index }
        }

        // 300m → #1、333m → #2、541m → #3、553m → #4。
        assertThat(achievedIndexes).containsExactly(1, 2, 3, 4).inOrder()
    }

    @Test
    fun `1回の更新で複数のマイルストーンを同時に達成する`() = runTest {
        // P4-2: 828m まで一気に進むと #1〜#10 がまとめて達成される。
        val events = recordDistance(828L, at)

        val achieved = events.filterIsInstance<ProgressEvent.MilestoneAchieved>()
        assertThat(achieved.map { it.milestone.index }).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).inOrder()
        assertThat(milestoneRepository.recordedIndexes()).hasSize(10)
    }

    @Test
    fun `同じマイルストーンは二度記録されない`() = runTest {
        // P4-3: 記録済みの分は演出も通知も出さない。
        recordDistance(300L, at)

        val events = recordDistance(20L, at)

        assertThat(events.filterIsInstance<ProgressEvent.MilestoneAchieved>()).isEmpty()
        assertThat(milestoneRepository.recordedIndexes()).containsExactly(1)
    }

    @Test
    fun `記録が漏れていたマイルストーンは次の更新で拾い直す`() {
        // プロセスが落ちて判定を1回飛ばしても、累計距離に届いていれば次で補完される。
        runTest {
            progressRepository.addDistance(1_000L, at)

            val events = recordDistance(1L, at)

            assertThat(events.filterIsInstance<ProgressEvent.MilestoneAchieved>().map { it.milestone.index })
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).inOrder()
        }
    }

    @Test
    fun `周内マーカーの到達が通知される`() = runTest {
        val events = recordDistance(LapMarker.QUARTER.distanceInLapMeters, at)

        val markers = events.filterIsInstance<ProgressEvent.LapMarkerReached>()
        assertThat(markers).hasSize(1)
        assertThat(markers.single().marker).isEqualTo(LapMarker.QUARTER)
        assertThat(markers.single().lapNumber).isEqualTo(1)
    }

    @Test
    fun `1周を走破すると周回が記録され周回数が繰り上がる`() = runTest {
        val events = recordDistance(lap, at)

        val completed = events.filterIsInstance<ProgressEvent.LapCompleted>()
        assertThat(completed.map { it.lapNumber }).containsExactly(1)
        assertThat(progressRepository.lifetimeStats.first().currentLap).isEqualTo(2)
        assertThat(progressRepository.lapRecords.first().map { it.lapNumber }).containsExactly(1)
    }

    @Test
    fun `1回の更新で2周以上進んでも取りこぼさない`() = runTest {
        // P4-5: 現実には起きないが、インポートや補正で一度に大きく増える場合に備える。
        val events = recordDistance(lap * 3, at)

        val completed = events.filterIsInstance<ProgressEvent.LapCompleted>()
        assertThat(completed.map { it.lapNumber }).containsExactly(1, 2, 3).inOrder()
        assertThat(progressRepository.lifetimeStats.first().currentLap).isEqualTo(4)
        assertThat(progressRepository.lapRecords.first().map { it.lapNumber })
            .containsExactly(1, 2, 3).inOrder()

        // 3周ぶんのマーカーがすべて出る。
        assertThat(events.filterIsInstance<ProgressEvent.LapMarkerReached>()).hasSize(9)
    }

    @Test
    fun `出来事は距離の昇順に並ぶ`() = runTest {
        val events = recordDistance(lap, at)

        assertThat(events.map { it.totalDistanceMeters }).isInOrder()
        // 最後に来るのは1周の走破。
        assertThat(events.last()).isInstanceOf(ProgressEvent.LapCompleted::class.java)
    }

    @Test
    fun `1周走破の更新では100個すべてが達成される`() = runTest {
        val events = recordDistance(lap, at)

        val achieved = events.filterIsInstance<ProgressEvent.MilestoneAchieved>()
        assertThat(achieved).hasSize(MilestoneCatalog.SIZE)
        assertThat(achieved.last().milestone.isMajor).isTrue()
    }

    @Test
    fun `2周目では達成が起きず周回とマーカーだけになる`() = runTest {
        // P4-7: 2周目以降はマイルストーンを再提示しない。
        recordDistance(lap, at)

        val events = recordDistance(lap, at)

        assertThat(events.filterIsInstance<ProgressEvent.MilestoneAchieved>()).isEmpty()
        assertThat(events.filterIsInstance<ProgressEvent.LapCompleted>().map { it.lapNumber })
            .containsExactly(2)
        assertThat(events.filterIsInstance<ProgressEvent.LapMarkerReached>().map { it.lapNumber })
            .containsExactly(2, 2, 2)
    }

    @Test
    fun `周回の走破とマーカーは大台として扱う`() = runTest {
        val events = recordDistance(lap, at)

        assertThat(events.filterIsInstance<ProgressEvent.LapCompleted>().all { it.isMajor }).isTrue()
        assertThat(events.filterIsInstance<ProgressEvent.LapMarkerReached>().all { it.isMajor }).isTrue()
    }
}
