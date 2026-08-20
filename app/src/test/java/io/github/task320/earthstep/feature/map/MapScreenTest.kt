package io.github.task320.earthstep.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.progress.Earth
import io.github.task320.earthstep.core.domain.progress.LapMarker
import io.github.task320.earthstep.core.domain.progress.ProgressSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** P8-7: マップ画面(P5-4 / P5-5)の表示。 */
@RunWith(RobolectricTestRunner::class)
class MapScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `周回数と周内の進捗を出す`() {
        setContent(ProgressSummary.of(totalDistanceMeters = 8_000_000L, todayDistanceMeters = 0L))

        composeRule.onNodeWithText(string(R.string.home_lap_label, 1)).assertIsDisplayed()
        composeRule.onNodeWithText(
            string(
                R.string.home_lap_progress_label,
                DistanceFormatter.formatDistance(8_000_000L),
                DistanceFormatter.formatDistance(Earth.CIRCUMFERENCE_METERS),
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun `次の周内マーカーまでの残りを出す`() {
        // 25% は 10,018,750m。8,000,000m 地点なら残り 2,018,750m。
        setContent(ProgressSummary.of(totalDistanceMeters = 8_000_000L, todayDistanceMeters = 0L))

        composeRule.onNodeWithText(
            string(
                R.string.map_next_marker,
                25,
                DistanceFormatter.formatDistance(LapMarker.QUARTER.distanceInLapMeters - 8_000_000L),
            ),
        ).assertIsDisplayed()
    }

    @Test
    fun `周の終盤ではマーカーの案内を出さない`() {
        // 75% を過ぎると、その周に残るマーカーは無い。
        setContent(
            ProgressSummary.of(
                totalDistanceMeters = LapMarker.THREE_QUARTERS.distanceInLapMeters + 1_000L,
                todayDistanceMeters = 0L,
            ),
        )

        composeRule.onNodeWithText(string(R.string.map_next_marker, 25, "")).assertDoesNotExist()
    }

    @Test
    fun `2周目でも地図と進捗を出す`() {
        setContent(ProgressSummary.of(Earth.CIRCUMFERENCE_METERS + 5_000L, 0L))

        composeRule.onNodeWithText(string(R.string.home_lap_label, 2)).assertIsDisplayed()
    }

    private fun setContent(progress: ProgressSummary) {
        composeRule.setContent {
            EarthStepTheme { MapScreen(progress = progress) }
        }
    }

    private fun string(resId: Int, vararg args: Any): String = composeRule.activity.getString(resId, *args)
}
