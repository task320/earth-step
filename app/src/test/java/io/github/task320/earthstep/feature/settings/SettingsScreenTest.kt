package io.github.task320.earthstep.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** P8-7: 設定画面(P5-14 / P7-2 / P7-3)の表示と操作。 */
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val allPermissions = PermissionRequirements.requiredOn(sdkInt = 33)

    @Test
    fun `歩幅の較正値を出す`() {
        setContent(SettingsUiState(strideLengthCm = 71.4))

        composeRule.onNodeWithText(string(R.string.settings_stride_value, 71.4))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `権限が揃っていれば許可と出す`() {
        setContent(
            SettingsUiState(
                permissionState = PermissionState(
                    granted = allPermissions,
                    required = allPermissions,
                    batteryOptimizationIgnored = true,
                ),
            ),
        )

        composeRule.onNodeWithText(string(R.string.settings_permission_location))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.settings_permission_denied)).assertCountEquals(0)
    }

    @Test
    fun `権限が欠けていれば未許可と出す`() {
        setContent(
            SettingsUiState(
                permissionState = PermissionState(
                    granted = setOf(AppPermission.FINE_LOCATION),
                    required = allPermissions,
                ),
            ),
        )

        // 位置情報だけ許可した状態では、背景位置・活動検知・通知・バッテリー最適化の4つが残る。
        composeRule.onAllNodesWithText(string(R.string.settings_permission_denied)).assertCountEquals(4)
        composeRule.onAllNodesWithText(string(R.string.settings_permission_granted)).assertCountEquals(1)
    }

    @Test
    fun `書き出しを押すと保存先の選択へ進む`() {
        var exported = false
        setContent(onExport = { exported = true })

        composeRule.onNodeWithText(string(R.string.settings_backup_export))
            .performScrollTo()
            .performClick()

        assertThat(exported).isTrue()
    }

    @Test
    fun `読み込みを押すとファイルの選択へ進む`() {
        var imported = false
        setContent(onImport = { imported = true })

        composeRule.onNodeWithText(string(R.string.settings_backup_import))
            .performScrollTo()
            .performClick()

        assertThat(imported).isTrue()
    }

    @Test
    fun `取り込みで増えた距離を出す`() {
        // 同じファイルを二度読んでも増えないことが目に見えるようにする(P7-3)。
        setContent(backupMessage = BackupMessage.Imported(addedMeters = 5_400L))

        composeRule.onNodeWithText(
            string(R.string.settings_backup_imported, DistanceFormatter.formatDistance(5_400L)),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `新しい形式のファイルはその旨を出す`() {
        setContent(backupMessage = BackupMessage.ImportUnsupportedVersion(version = 2))

        composeRule.onNodeWithText(string(R.string.settings_backup_import_unsupported, 2))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `リセットは確認してから実行する`() {
        var reset = false
        setContent(onReset = { reset = true })

        composeRule.onNodeWithText(string(R.string.settings_reset_action))
            .performScrollTo()
            .performClick()

        // 押した時点ではまだ消さない。
        assertThat(reset).isFalse()
        composeRule.onNodeWithText(string(R.string.settings_reset_confirm_title)).assertIsDisplayed()
    }

    @Test
    fun `確認ダイアログで消すと実行される`() {
        var reset = false
        setContent(onReset = { reset = true })

        composeRule.onNodeWithText(string(R.string.settings_reset_action))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(string(R.string.settings_reset_confirm_action)).performClick()

        assertThat(reset).isTrue()
    }

    @Test
    fun `確認ダイアログをやめると消さない`() {
        var reset = false
        setContent(onReset = { reset = true })

        composeRule.onNodeWithText(string(R.string.settings_reset_action))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(string(R.string.settings_reset_cancel)).performClick()

        assertThat(reset).isFalse()
        composeRule.onNodeWithText(string(R.string.settings_reset_confirm_title)).assertDoesNotExist()
    }

    @Test
    fun `クラウド同期が未実装であることを伝える`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.settings_sync_pending))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setContent(
        uiState: SettingsUiState = SettingsUiState(),
        backupMessage: BackupMessage? = null,
        onExport: () -> Unit = {},
        onImport: () -> Unit = {},
        onReset: () -> Unit = {},
    ) {
        composeRule.setContent {
            EarthStepTheme {
                SettingsScreen(
                    uiState = uiState,
                    backupMessage = backupMessage,
                    onMeasurementEnabledChange = {},
                    onOpenAppSettings = {},
                    onOpenBatterySettings = {},
                    onExport = onExport,
                    onImport = onImport,
                    onReset = onReset,
                )
            }
        }
    }

    private fun string(resId: Int, vararg args: Any): String = composeRule.activity.getString(resId, *args)
}
