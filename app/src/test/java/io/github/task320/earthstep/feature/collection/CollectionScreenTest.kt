package io.github.task320.earthstep.feature.collection

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** P8-7: 達成記録(P5-6)の表示と詳細の開閉。 */
@RunWith(RobolectricTestRunner::class)
class CollectionScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val achievedAt = Instant.parse("2026-08-20T03:00:00Z")

    @Test
    fun `達成件数を見出しに出す`() {
        setContent(achievedCount = 2)

        composeRule.onNodeWithText(string(R.string.collection_progress, 2, ROW_COUNT)).assertIsDisplayed()
    }

    @Test
    fun `未到達のマイルストーンも名称を伏せずに出す`() {
        // 次に何を目指すのか分からないと距離を積む動機にならない。
        setContent(achievedCount = 0)

        val third = MilestoneCatalog.byIndex(3)!!
        composeRule.onNodeWithText(third.name).assertIsDisplayed()
    }

    @Test
    fun `各行に距離を出す`() {
        setContent(achievedCount = 0)

        val first = MilestoneCatalog.byIndex(1)!!
        composeRule.onNodeWithText(DistanceFormatter.formatDistance(first.distanceMeters))
            .assertIsDisplayed()
    }

    @Test
    fun `詳細は最初は閉じている`() {
        setContent(achievedCount = 1)

        composeRule.onNodeWithText(string(R.string.collection_not_achieved)).assertDoesNotExist()
    }

    @Test
    fun `行をタップすると到達日が開く`() {
        setContent(achievedCount = 1)

        composeRule.onNodeWithText(MilestoneCatalog.byIndex(1)!!.name).performClick()

        composeRule.onNodeWithText(string(R.string.collection_achieved_at, formatDate(achievedAt)))
            .assertIsDisplayed()
    }

    @Test
    fun `未到達の行をタップすると未到達と出る`() {
        setContent(achievedCount = 0)

        composeRule.onNodeWithText(MilestoneCatalog.byIndex(1)!!.name).performClick()

        composeRule.onNodeWithText(string(R.string.collection_not_achieved)).assertIsDisplayed()
    }

    @Test
    fun `もう一度タップすると詳細が閉じる`() {
        setContent(achievedCount = 0)
        val name = MilestoneCatalog.byIndex(1)!!.name

        composeRule.onNodeWithText(name).performClick()
        composeRule.onNodeWithText(name).performClick()

        composeRule.onNodeWithText(string(R.string.collection_not_achieved)).assertDoesNotExist()
    }

    @Test
    fun `詳細は同時にひとつだけ開く`() {
        // 100件のどこを見ていたか見失わないよう、開くのは1件だけにする。
        setContent(achievedCount = 1)

        composeRule.onNodeWithText(MilestoneCatalog.byIndex(1)!!.name).performClick()
        composeRule.onNodeWithText(MilestoneCatalog.byIndex(2)!!.name).performClick()

        composeRule.onNodeWithText(string(R.string.collection_achieved_at, formatDate(achievedAt)))
            .assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.collection_not_achieved)).assertIsDisplayed()
    }

    /**
     * @param achievedCount 先頭から何件を到達済みにするか。
     *   一覧は距離の昇順なので、先頭から埋まるのが実際の状態に合う。
     */
    private fun setContent(achievedCount: Int) {
        val rows = MilestoneCatalog.milestones.take(ROW_COUNT).mapIndexed { index, milestone ->
            MilestoneRow(
                milestone = milestone,
                achievedAt = achievedAt.takeIf { index < achievedCount },
            )
        }
        composeRule.setContent {
            EarthStepTheme {
                CollectionScreen(uiState = CollectionUiState(rows = rows, achievedCount = achievedCount))
            }
        }
    }

    private fun formatDate(instant: Instant): String =
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").format(instant.atZone(ZoneId.systemDefault()))

    private fun string(resId: Int, vararg args: Any): String = composeRule.activity.getString(resId, *args)

    private companion object {
        /** 画面に収まる件数だけ用意する。100件すべては表示の検証に要らない。 */
        const val ROW_COUNT = 5
    }
}
