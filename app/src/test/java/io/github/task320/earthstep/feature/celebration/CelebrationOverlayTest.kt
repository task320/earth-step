package io.github.task320.earthstep.feature.celebration

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.progress.LapMarker
import io.github.task320.earthstep.core.domain.progress.ProgressSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** P8-7: 達成演出(P5-7 / P5-9 / P5-10 / P5-11)の表示と操作。 */
@RunWith(RobolectricTestRunner::class)
class CelebrationOverlayTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `マイルストーン到達の見出しに名称を出す`() {
        val milestone = MilestoneCatalog.byIndex(23)!!
        setContent(PendingCelebration.Milestone(23, milestone.distanceMeters))

        composeRule.onNodeWithText(string(R.string.celebration_milestone_title, milestone.name))
            .assertIsDisplayed()
    }

    @Test
    fun `累計距離と次までの距離を出す`() {
        // P5-10: 名称 / ここまでの累計距離 / 次までの距離。
        val progress = ProgressSummary.of(totalDistanceMeters = 300L, todayDistanceMeters = 300L)
        setContent(PendingCelebration.Milestone(1, 300L), progress = progress)

        composeRule.onNodeWithText(
            string(R.string.celebration_total_distance, DistanceFormatter.formatDistance(300L)),
        ).assertIsDisplayed()

        val next = MilestoneCatalog.byIndex(2)!!
        composeRule.onNodeWithText(
            string(
                R.string.celebration_next_distance,
                next.name,
                DistanceFormatter.formatDistance(next.distanceMeters - 300L),
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun `25パーセントの周内マーカーは豆知識を添える`() {
        // 仕様4.2: 赤道から北極点までの距離に相当するという豆知識。
        setContent(
            PendingCelebration.Marker(LapMarker.QUARTER, lapNumber = 1, totalDistanceMeters = 10_018_750L),
        )

        composeRule.onNodeWithText(string(R.string.celebration_marker_title, "25%")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.celebration_marker_quarter_description))
            .assertIsDisplayed()
    }

    @Test
    fun `50パーセントは対蹠地の説明を出す`() {
        setContent(PendingCelebration.Marker(LapMarker.HALF, 1, 20_037_500L))

        composeRule.onNodeWithText(string(R.string.celebration_marker_half_description)).assertIsDisplayed()
    }

    @Test
    fun `75パーセントは説明を持たず数字だけで見せる`() {
        // 仕様4.2: この距離帯は数字のインパクトのみで見せる。
        setContent(PendingCelebration.Marker(LapMarker.THREE_QUARTERS, 1, 30_056_250L))

        composeRule.onNodeWithText(string(R.string.celebration_marker_title, "75%")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.celebration_marker_quarter_description)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.celebration_marker_half_description)).assertDoesNotExist()
    }

    @Test
    fun `周回の走破は周回数を見出しに出す`() {
        setContent(PendingCelebration.Lap(lapNumber = 2, totalDistanceMeters = 80_150_000L))

        composeRule.onNodeWithText(string(R.string.celebration_lap_title, "2")).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.celebration_lap_description)).assertIsDisplayed()
    }

    @Test
    fun `閉じると次へ進む`() {
        var dismissed = false
        setContent(PendingCelebration.Milestone(1, 300L), onDismiss = { dismissed = true })

        composeRule.onNodeWithText(string(R.string.celebration_dismiss)).performClick()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun `残りが無ければまとめて閉じるは出さない`() {
        setContent(PendingCelebration.Milestone(1, 300L), remaining = 0)

        composeRule.onNodeWithText(string(R.string.celebration_skip_all)).assertDoesNotExist()
    }

    @Test
    fun `残りがあれば件数とまとめて閉じるを出す`() {
        setContent(PendingCelebration.Milestone(1, 300L), remaining = 3)

        composeRule.onNodeWithText(string(R.string.celebration_remaining, 3)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.celebration_skip_all)).assertIsDisplayed()
    }

    @Test
    fun `まとめて閉じるとキューを空にする`() {
        var skipped = false
        setContent(PendingCelebration.Milestone(1, 300L), remaining = 3, onSkipAll = { skipped = true })

        composeRule.onNodeWithText(string(R.string.celebration_skip_all)).performClick()

        assertThat(skipped).isTrue()
    }

    private fun setContent(
        celebration: PendingCelebration,
        progress: ProgressSummary = ProgressSummary.of(300L, 300L),
        remaining: Int = 0,
        onDismiss: () -> Unit = {},
        onSkipAll: () -> Unit = {},
    ) {
        composeRule.setContent {
            EarthStepTheme {
                CelebrationContentView(
                    celebration = celebration,
                    progress = progress,
                    remaining = remaining,
                    onDismiss = onDismiss,
                    onSkipAll = onSkipAll,
                )
            }
        }
    }

    private fun string(resId: Int, vararg args: Any): String = composeRule.activity.getString(resId, *args)
}
