package io.github.task320.earthstep.feature.celebration

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.progress.LapMarker
import io.github.task320.earthstep.core.domain.usecase.ObserveProgressSummaryUseCase
import io.github.task320.earthstep.testing.FakeCelebrationQueueRepository
import io.github.task320.earthstep.testing.FakeProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** P5-13: 貯まった演出を順に再生する。 */
@OptIn(ExperimentalCoroutinesApi::class)
class CelebrationViewModelTest {

    private val queueRepository = FakeCelebrationQueueRepository()
    private val progressRepository = FakeProgressRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `キューが空なら再生するものは無い`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.current).isNull()
            assertThat(state.remaining).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `先頭から順に再生し閉じるたびに次へ進む`() = runTest {
        queueRepository.enqueue(
            listOf(
                PendingCelebration.Milestone(1, 300L),
                PendingCelebration.Milestone(2, 333L),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(expectMostRecentItem().current)
                .isEqualTo(PendingCelebration.Milestone(1, 300L))

            viewModel.dismissCurrent()
            assertThat(expectMostRecentItem().current)
                .isEqualTo(PendingCelebration.Milestone(2, 333L))

            viewModel.dismissCurrent()
            assertThat(expectMostRecentItem().current).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `残り件数を数える`() = runTest {
        queueRepository.enqueue(
            listOf(
                PendingCelebration.Milestone(1, 300L),
                PendingCelebration.Milestone(2, 333L),
                PendingCelebration.Marker(LapMarker.QUARTER, 1, 10_018_750L),
            ),
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(expectMostRecentItem().remaining).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `まとめて閉じるとキューが空になる`() = runTest {
        queueRepository.enqueue(listOf(PendingCelebration.Milestone(1, 300L)))
        val viewModel = viewModel()

        viewModel.skipAll()

        assertThat(queueRepository.current()).isEmpty()
    }

    @Test
    fun `起きた順に並べ替えて再生する`() = runTest {
        // 通知の到着順ではなく、達成した距離の順に見せる。
        queueRepository.enqueue(listOf(PendingCelebration.Milestone(3, 541L)))
        queueRepository.enqueue(listOf(PendingCelebration.Milestone(1, 300L)))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertThat(expectMostRecentItem().current)
                .isEqualTo(PendingCelebration.Milestone(1, 300L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel() = CelebrationViewModel(
        celebrationQueueRepository = queueRepository,
        observeProgressSummary = ObserveProgressSummaryUseCase(progressRepository),
    )
}
