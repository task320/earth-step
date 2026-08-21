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
import io.github.task320.earthstep.core.domain.model.GoogleAccount
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    fun `未サインインならサインインボタンを出す`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_signin))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `サインインを押すと呼ばれる`() {
        var signedIn = false
        setContent(onSignIn = { signedIn = true })

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_signin))
            .performScrollTo()
            .performClick()

        assertThat(signedIn).isTrue()
    }

    @Test
    fun `サインイン済みならメールアドレスとサインアウトを出す`() {
        setContent(
            uiState = SettingsUiState(
                signedInAccount = GoogleAccount(id = "1", email = "walker@example.com", displayName = "Walker"),
            ),
        )

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_signed_in_as, "walker@example.com"))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_signout))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `サインアウトを押すと呼ばれる`() {
        var signedOut = false
        setContent(
            uiState = SettingsUiState(
                signedInAccount = GoogleAccount(id = "1", email = "walker@example.com", displayName = null),
            ),
            onSignOut = { signedOut = true },
        )

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_signout))
            .performScrollTo()
            .performClick()

        assertThat(signedOut).isTrue()
    }

    @Test
    fun `サインインに失敗したら理由を出す`() {
        setContent(signInFailed = true)

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_signin_failed))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `サインイン済みなら今すぐ同期ボタンを出す`() {
        setContent(
            uiState = SettingsUiState(
                signedInAccount = GoogleAccount(id = "1", email = "walker@example.com", displayName = null),
            ),
        )

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_action))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `今すぐ同期を押すと呼ばれる`() {
        var synced = false
        setContent(
            uiState = SettingsUiState(
                signedInAccount = GoogleAccount(id = "1", email = "walker@example.com", displayName = null),
            ),
            onSync = { synced = true },
        )

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_action))
            .performScrollTo()
            .performClick()

        assertThat(synced).isTrue()
    }

    @Test
    fun `同期で増えた距離を出す`() {
        setContent(
            uiState = SettingsUiState(
                signedInAccount = GoogleAccount(id = "1", email = "walker@example.com", displayName = null),
            ),
            driveSyncMessage = DriveSyncMessage.Synced(addedMeters = 3_200L),
        )

        composeRule.onNodeWithText(
            string(R.string.settings_cloud_sync_synced, DistanceFormatter.formatDistance(3_200L)),
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `サインイン済みで未同期ならその旨を出す`() {
        setContent(
            uiState = SettingsUiState(
                signedInAccount = GoogleAccount(id = "1", email = "walker@example.com", displayName = null),
            ),
        )

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_never_synced))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `最終同期日時を出す`() {
        val syncedAt = Instant.parse("2026-08-22T04:00:00Z")
        setContent(
            uiState = SettingsUiState(
                signedInAccount = GoogleAccount(id = "1", email = "walker@example.com", displayName = null),
                lastSyncedAt = syncedAt,
            ),
        )

        val formatted = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").format(syncedAt.atZone(ZoneId.systemDefault()))
        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_last_synced_at, formatted))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `同期に失敗したら理由を出す`() {
        setContent(
            uiState = SettingsUiState(
                signedInAccount = GoogleAccount(id = "1", email = "walker@example.com", displayName = null),
            ),
            driveSyncMessage = DriveSyncMessage.Failed,
        )

        composeRule.onNodeWithText(string(R.string.settings_cloud_sync_failed))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // このテストヘルパーはSettingsScreenの引数をそのまま横流しするだけなので、
    // 数が多くても分割するとかえって呼び出し側との対応が追いにくくなる。
    @Suppress("LongParameterList")
    private fun setContent(
        uiState: SettingsUiState = SettingsUiState(),
        backupMessage: BackupMessage? = null,
        driveSyncMessage: DriveSyncMessage? = null,
        signInFailed: Boolean = false,
        onExport: () -> Unit = {},
        onImport: () -> Unit = {},
        onReset: () -> Unit = {},
        onSignIn: () -> Unit = {},
        onSignOut: () -> Unit = {},
        onSync: () -> Unit = {},
    ) {
        composeRule.setContent {
            EarthStepTheme {
                SettingsScreen(
                    uiState = uiState,
                    backupMessage = backupMessage,
                    driveSyncMessage = driveSyncMessage,
                    signInFailed = signInFailed,
                    onMeasurementEnabledChange = {},
                    onOpenAppSettings = {},
                    onOpenBatterySettings = {},
                    onExport = onExport,
                    onImport = onImport,
                    onReset = onReset,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                    onSync = onSync,
                )
            }
        }
    }

    private fun string(resId: Int, vararg args: Any): String = composeRule.activity.getString(resId, *args)
}
