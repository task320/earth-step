package io.github.task320.earthstep.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.core.domain.progress.Earth
import io.github.task320.earthstep.core.domain.progress.ProgressSummary
import io.github.task320.earthstep.feature.permission.PermissionIntents
import io.github.task320.earthstep.ui.OnLifecycleResume

@Composable
fun HomeRoute(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 設定画面から戻ってきたときに警告表示を最新にする(P3-9)。
    OnLifecycleResume { viewModel.refreshPermissions() }

    HomeScreen(
        uiState = uiState,
        onOpenSettings = { PermissionIntents.openAppSettings(context) },
        modifier = modifier,
    )
}

/**
 * ホーム画面。
 * 現時点では進捗の数値と権限の警告のみ。作り込みは P5-3 で行う。
 */
@Composable
fun HomeScreen(uiState: HomeUiState, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
            )
            val progress = uiState.progress
            Text(
                text = stringResource(R.string.home_total_distance_label) + ": " +
                    DistanceFormatter.formatDistance(progress.totalDistanceMeters),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.home_xp_label) + ": " +
                    DistanceFormatter.formatXp(progress.xp),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_today_distance_label) + ": " +
                    DistanceFormatter.formatDistance(progress.todayDistanceMeters),
                style = MaterialTheme.typography.titleMedium,
            )

            // 2周目以降はマイルストーンを再提示せず、周回の進捗だけを見せる(仕様4.1 / P4-7)。
            if (progress.showsMilestoneList) {
                NextMilestone(progress = progress)
            } else {
                LapProgressText(progress = progress)
            }

            PermissionWarnings(
                permissionState = uiState.permissionState,
                onOpenSettings = onOpenSettings,
            )

            Text(
                text = stringResource(R.string.home_version_label, uiState.versionName),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** 次のマイルストーンまでの残り(P4-4)。 */
@Composable
private fun NextMilestone(progress: ProgressSummary, modifier: Modifier = Modifier) {
    val next = progress.nextMilestone
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (next == null) {
                stringResource(R.string.home_all_milestones_achieved)
            } else {
                stringResource(
                    R.string.home_next_milestone_label,
                    next.name,
                    DistanceFormatter.formatDistance(progress.remainingToNextMilestoneMeters),
                )
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { progress.milestoneRatio },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 周回カウンターと周内の進捗(仕様4.1 / P4-7)。 */
@Composable
private fun LapProgressText(progress: ProgressSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_lap_label, progress.lapProgress.lapNumber),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                R.string.home_lap_progress_label,
                DistanceFormatter.formatDistance(progress.lapProgress.distanceInLapMeters),
                DistanceFormatter.formatDistance(Earth.CIRCUMFERENCE_METERS),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { progress.lapProgress.ratio },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 権限が欠けている状態を常時表示し、再取得の導線を出す(P3-9)。 */
@Composable
private fun PermissionWarnings(
    permissionState: PermissionState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val warnings = HomeWarning.from(permissionState)
    if (warnings.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_permission_warning_title),
                style = MaterialTheme.typography.titleSmall,
            )
            warnings.forEach { warning ->
                Text(
                    text = "・" + stringResource(warning.messageRes),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text(text = stringResource(R.string.home_permission_open_settings))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    EarthStepTheme {
        HomeScreen(
            uiState = HomeUiState(
                progress = ProgressSummary.of(totalDistanceMeters = 12_345L, todayDistanceMeters = 2_460L),
                permissionState = PermissionState(
                    granted = setOf(AppPermission.FINE_LOCATION),
                    required = setOf(AppPermission.FINE_LOCATION, AppPermission.BACKGROUND_LOCATION),
                ),
                versionName = "0.1.0",
            ),
            onOpenSettings = {},
        )
    }
}
