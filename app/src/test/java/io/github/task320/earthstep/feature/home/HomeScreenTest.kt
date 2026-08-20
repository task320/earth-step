package io.github.task320.earthstep.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.core.domain.progress.Earth
import io.github.task320.earthstep.core.domain.progress.ProgressSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P8-7: ホーム画面の表示(仕様どおりの数値が並ぶこと)を自動で確かめる。
 *
 * Compose の UI テストは Robolectric 上で動かしている。実機・エミュレータが要る
 * `androidTest` に置くと CI で実行されず、壊れても気づけないため。
 * 実機での描画確認は P8-6(端末マトリクス検証)の担当。
 */
@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val allPermissions = PermissionRequirements.requiredOn(sdkInt = 33)

    @Test
    fun `累計距離とXPと当日距離と周回数が並ぶ`() {
        // P5-3 が出すと決めた数値がすべて画面にあること。
        setContent(ProgressSummary.of(totalDistanceMeters = 12_345L, todayDistanceMeters = 2_460L))

        composeRule.onNodeWithText(DistanceFormatter.formatDistance(12_345L)).assertIsDisplayed()
        composeRule.onNodeWithText(DistanceFormatter.formatXp(12_345L)).assertIsDisplayed()
        composeRule.onNodeWithText(
            string(R.string.home_today_distance_label) + " " + DistanceFormatter.formatDistance(2_460L),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.home_lap_label, 1)).assertIsDisplayed()
    }

    @Test
    fun `次のマイルストーンと残り距離を出す`() {
        // 300m の #1 まで残り 100m。
        setContent(ProgressSummary.of(totalDistanceMeters = 200L, todayDistanceMeters = 200L))

        val next = MilestoneCatalog.byIndex(1)!!
        composeRule.onNodeWithText(
            string(R.string.home_next_milestone_label, next.name, DistanceFormatter.formatDistance(100L)),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `達成件数を出す`() {
        setContent(ProgressSummary.of(totalDistanceMeters = 333L, todayDistanceMeters = 0L))

        composeRule.onNodeWithText(string(R.string.home_milestone_count, 2, MilestoneCatalog.SIZE))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `100個すべて達成した時点で周回の進捗へ切り替わる`() {
        // 100個目の距離と1周の距離は同じ(仕様3.1)。
        // 「一覧を出すが次の目標が無い」状態は起こらない。
        setContent(ProgressSummary.of(Earth.CIRCUMFERENCE_METERS, 0L))

        composeRule.onNodeWithText(string(R.string.home_lap_label, 2)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.home_all_milestones_achieved)).assertDoesNotExist()
    }

    @Test
    fun `2周目はマイルストーンではなく周回の進捗を出す`() {
        // 仕様4.1 / P4-7: 2周目以降は100マイルストーンを再提示しない。
        setContent(ProgressSummary.of(Earth.CIRCUMFERENCE_METERS + 1_000L, 0L))

        composeRule.onNodeWithText(string(R.string.home_lap_label, 2)).assertIsDisplayed()
        composeRule.onNodeWithText(
            string(
                R.string.home_lap_progress_label,
                DistanceFormatter.formatDistance(1_000L),
                DistanceFormatter.formatDistance(Earth.CIRCUMFERENCE_METERS),
            ),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.home_milestone_count, MilestoneCatalog.SIZE, MilestoneCatalog.SIZE))
            .assertDoesNotExist()
    }

    @Test
    fun `計測中かどうかを出す`() {
        setContent(measuring = true)
        composeRule.onNodeWithText(string(R.string.home_measuring)).assertIsDisplayed()
    }

    @Test
    fun `計測が止まっていることを出す`() {
        setContent(measuring = false)
        composeRule.onNodeWithText(string(R.string.home_not_measuring)).assertIsDisplayed()
    }

    @Test
    fun `権限がすべて揃っていれば警告を出さない`() {
        setContent(
            permissionState = PermissionState(
                granted = allPermissions,
                required = allPermissions,
                batteryOptimizationIgnored = true,
            ),
        )

        composeRule.onNodeWithText(string(R.string.home_permission_warning_title)).assertDoesNotExist()
    }

    @Test
    fun `位置情報が無ければ計測できない旨を警告する`() {
        // P3-9: 権限が欠けている状態を常時表示する。
        setContent(permissionState = PermissionState(required = allPermissions))

        composeRule.onNodeWithText(string(R.string.home_permission_warning_title))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("・" + string(R.string.home_permission_missing_location))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `警告から設定画面を開ける`() {
        var opened = false
        setContent(
            permissionState = PermissionState(required = allPermissions),
            onOpenSettings = { opened = true },
        )

        composeRule.onNodeWithText(string(R.string.home_permission_open_settings))
            .performScrollTo()
            .performClick()

        assertThat(opened).isTrue()
    }

    private fun setContent(
        progress: ProgressSummary = ProgressSummary.of(0L, 0L),
        measuring: Boolean = false,
        permissionState: PermissionState = PermissionState(
            granted = allPermissions,
            required = allPermissions,
            batteryOptimizationIgnored = true,
        ),
        onOpenSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            EarthStepTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        progress = progress,
                        measuring = measuring,
                        permissionState = permissionState,
                        versionName = "1.0.0",
                    ),
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }

    private fun string(resId: Int, vararg args: Any): String = if (args.isEmpty()) {
        composeRule.activity.getString(resId)
    } else {
        composeRule.activity.getString(resId, *args)
    }
}
