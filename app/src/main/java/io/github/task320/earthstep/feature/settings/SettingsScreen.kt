package io.github.task320.earthstep.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.component.PixelButton
import io.github.task320.earthstep.core.designsystem.component.PixelPanel
import io.github.task320.earthstep.core.designsystem.component.PixelTextButton
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.feature.permission.PermissionIntents
import io.github.task320.earthstep.ui.OnLifecycleResume

/**
 * 設定(P5-14)。
 *
 * 権限の状態・バッテリー最適化・歩幅の較正値・リセットを扱う。
 * エクスポート/インポートと同期は P7 で追加する。
 */
@Composable
fun SettingsRoute(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    OnLifecycleResume { viewModel.refreshPermissions() }

    SettingsScreen(
        uiState = uiState,
        onMeasurementEnabledChange = viewModel::setMeasurementEnabled,
        onOpenAppSettings = { PermissionIntents.openAppSettings(context) },
        onOpenBatterySettings = { PermissionIntents.requestIgnoreBatteryOptimizations(context) },
        onReset = viewModel::reset,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onMeasurementEnabledChange: (Boolean) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingReset by remember { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixelDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(PixelDimens.SpaceMedium),
        ) {
            PixelPanel {
                Text(
                    text = stringResource(R.string.settings_measurement_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.settings_measurement_enabled),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = uiState.measurementEnabled,
                        onCheckedChange = onMeasurementEnabledChange,
                    )
                }
                Text(
                    text = stringResource(R.string.settings_measurement_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelPalette.Mist,
                )
            }

            PermissionPanel(
                permissionState = uiState.permissionState,
                onOpenAppSettings = onOpenAppSettings,
                onOpenBatterySettings = onOpenBatterySettings,
            )

            PixelPanel {
                Text(
                    text = stringResource(R.string.settings_stride_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.settings_stride_value, uiState.strideLengthCm),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.settings_stride_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelPalette.Mist,
                )
            }

            PixelPanel(borderColor = PixelPalette.Rose) {
                Text(
                    text = stringResource(R.string.settings_reset_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = PixelPalette.Rose,
                )
                Text(
                    text = stringResource(
                        R.string.settings_reset_note,
                        DistanceFormatter.formatDistance(uiState.totalDistanceMeters),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelPalette.Mist,
                )
                PixelTextButton(
                    text = stringResource(R.string.settings_reset_action),
                    onClick = { confirmingReset = true },
                )
            }

            Text(
                text = stringResource(R.string.settings_backup_pending),
                style = MaterialTheme.typography.labelSmall,
                color = PixelPalette.Mist,
            )
        }
    }

    if (confirmingReset) {
        ResetConfirmDialog(
            onConfirm = {
                confirmingReset = false
                onReset()
            },
            onDismiss = { confirmingReset = false },
        )
    }
}

@Composable
private fun PermissionPanel(
    permissionState: PermissionState,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PixelPanel(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_permission_title),
            style = MaterialTheme.typography.titleSmall,
        )
        PermissionRow(R.string.settings_permission_location, permissionState.canMeasure)
        PermissionRow(
            R.string.settings_permission_background_location,
            permissionState.canMeasureInBackground,
        )
        PermissionRow(
            R.string.settings_permission_activity,
            permissionState.isGranted(AppPermission.ACTIVITY_RECOGNITION) ||
                AppPermission.ACTIVITY_RECOGNITION !in permissionState.required,
        )
        PermissionRow(R.string.settings_permission_notification, permissionState.canNotify)
        PermissionRow(
            R.string.settings_permission_battery,
            permissionState.batteryOptimizationIgnored,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PixelDimens.SpaceSmall),
        ) {
            PixelTextButton(
                text = stringResource(R.string.home_permission_open_settings),
                onClick = onOpenAppSettings,
            )
            PixelTextButton(
                text = stringResource(R.string.settings_permission_battery_action),
                onClick = onOpenBatterySettings,
            )
        }
    }
}

@Composable
private fun PermissionRow(labelRes: Int, granted: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodySmall)
        Text(
            text = stringResource(
                if (granted) R.string.settings_permission_granted else R.string.settings_permission_denied,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) PixelPalette.Green else PixelPalette.Rose,
        )
    }
}

@Composable
private fun ResetConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_reset_confirm_title)) },
        text = { Text(text = stringResource(R.string.settings_reset_confirm_body)) },
        confirmButton = {
            PixelButton(
                text = stringResource(R.string.settings_reset_action),
                onClick = onConfirm,
                containerColor = PixelPalette.Rose,
            )
        },
        dismissButton = {
            PixelTextButton(text = stringResource(R.string.settings_reset_cancel), onClick = onDismiss)
        },
        containerColor = PixelPalette.Deep,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun SettingsScreenPreview() {
    EarthStepTheme {
        SettingsScreen(
            uiState = SettingsUiState(strideLengthCm = 71.4, totalDistanceMeters = 123_456L),
            onMeasurementEnabledChange = {},
            onOpenAppSettings = {},
            onOpenBatterySettings = {},
            onReset = {},
        )
    }
}
