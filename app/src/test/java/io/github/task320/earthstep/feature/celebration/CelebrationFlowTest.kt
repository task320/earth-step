package io.github.task320.earthstep.feature.celebration

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.progress.LapMarker
import io.github.task320.earthstep.core.domain.usecase.ObserveProgressSummaryUseCase
import io.github.task320.earthstep.testing.FakeCelebrationQueueRepository
import io.github.task320.earthstep.testing.FakeProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P8-7: 演出フロー(P5-13)を通しで確かめる。
 *
 * バックグラウンドで貯まった演出が、起動後に1件ずつ順に再生され、
 * 閉じ切ると消えることを画面の操作で検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CelebrationFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
    fun `キューが空なら演出は出ない`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.celebration_dismiss)).assertDoesNotExist()
    }

    @Test
    fun `貯まった演出を起きた順に再生して最後に消える`() {
        // 距離の昇順に #1 → #2 → 25%マーカー の順で再生される。
        enqueue(
            PendingCelebration.Marker(LapMarker.QUARTER, lapNumber = 1, totalDistanceMeters = 10_018_750L),
            PendingCelebration.Milestone(milestoneIndex = 2, totalDistanceMeters = 333L),
            PendingCelebration.Milestone(milestoneIndex = 1, totalDistanceMeters = 300L),
        )
        setContent()

        assertHeadlineIsMilestone(1)
        composeRule.onNodeWithText(string(R.string.celebration_remaining, 2)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.celebration_dismiss)).performClick()
        assertHeadlineIsMilestone(2)

        composeRule.onNodeWithText(string(R.string.celebration_dismiss)).performClick()
        composeRule.onNodeWithText(string(R.string.celebration_marker_title, "25%")).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.celebration_dismiss)).performClick()
        composeRule.onNodeWithText(string(R.string.celebration_dismiss)).assertDoesNotExist()
    }

    @Test
    fun `まとめて閉じると残りも消える`() {
        enqueue(
            PendingCelebration.Milestone(1, 300L),
            PendingCelebration.Milestone(2, 333L),
            PendingCelebration.Milestone(3, 541L),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.celebration_skip_all)).performClick()

        composeRule.onNodeWithText(string(R.string.celebration_dismiss)).assertDoesNotExist()
        assertThat(queueRepository.current()).isEmpty()
    }

    @Test
    fun `残り1件になるとまとめて閉じるは消える`() {
        enqueue(PendingCelebration.Milestone(1, 300L), PendingCelebration.Milestone(2, 333L))
        setContent()

        composeRule.onNodeWithText(string(R.string.celebration_dismiss)).performClick()

        composeRule.onNodeWithText(string(R.string.celebration_skip_all)).assertDoesNotExist()
    }

    private fun assertHeadlineIsMilestone(index: Int) {
        val milestone = MilestoneCatalog.byIndex(index)!!
        composeRule.onNodeWithText(string(R.string.celebration_milestone_title, milestone.name))
            .assertIsDisplayed()
    }

    private fun enqueue(vararg celebrations: PendingCelebration) = runBlocking {
        queueRepository.enqueue(celebrations.toList())
    }

    private fun setContent() {
        val viewModel = CelebrationViewModel(
            celebrationQueueRepository = queueRepository,
            observeProgressSummary = ObserveProgressSummaryUseCase(progressRepository),
        )
        composeRule.setContent {
            EarthStepTheme { CelebrationOverlay(viewModel = viewModel) }
        }
    }

    private fun string(resId: Int, vararg args: Any): String = composeRule.activity.getString(resId, *args)
}
