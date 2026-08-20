package io.github.task320.earthstep.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.component.PixelPanel
import io.github.task320.earthstep.core.designsystem.component.PixelProgressBar
import io.github.task320.earthstep.core.designsystem.component.PixelTextButton
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.core.domain.milestone.Milestone
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.core.domain.progress.Earth
import io.github.task320.earthstep.core.domain.progress.ProgressSummary
import io.github.task320.earthstep.feature.map.WorldMapView
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
 * ホーム画面(P5-3)。
 *
 * 出す数字は 累計距離・XP・当日距離・周回数・次のマイルストーンまでの距離 と進捗バー。
 * 一番大きく出すのは累計距離。このゲームで積み上がるのはそれだけで、
 * XPは同じ値の別表現でしかないため、同じ大きさで2つ並べると視線が散る。
 */
@Composable
fun HomeScreen(uiState: HomeUiState, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val progress = uiState.progress

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixelDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(PixelDimens.SpaceMedium),
        ) {
            TotalDistancePanel(progress = progress, measuring = uiState.measuring)

            WorldMapView(lapRatio = progress.lapProgress.ratio, lapSkin = progress.lapSkin)

            // 2周目以降はマイルストーンを再提示せず、周回の進捗だけを見せる(仕様4.1 / P4-7)。
            // 100個目に到達した時点で1周を走破しているため、
            // 「一覧を出すが次の目標が無い」状態は起こらない。
            val next = progress.nextMilestone
            if (progress.showsMilestoneList && next != null) {
                NextMilestonePanel(progress = progress, next = next)
            } else {
                LapProgressPanel(progress = progress)
            }

            PermissionWarnings(
                permissionState = uiState.permissionState,
                onOpenSettings = onOpenSettings,
            )

            Text(
                text = stringResource(R.string.home_version_label, uiState.versionName),
                style = MaterialTheme.typography.labelSmall,
                color = PixelPalette.Mist,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TotalDistancePanel(progress: ProgressSummary, measuring: Boolean, modifier: Modifier = Modifier) {
    PixelPanel(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.home_total_distance_label),
            style = MaterialTheme.typography.labelMedium,
            color = PixelPalette.Mist,
        )
        Text(
            text = DistanceFormatter.formatDistance(progress.totalDistanceMeters),
            style = MaterialTheme.typography.displaySmall,
            color = PixelPalette.Gold,
        )
        Text(
            text = DistanceFormatter.formatXp(progress.xp),
            style = MaterialTheme.typography.bodyMedium,
            color = PixelPalette.Mist,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.home_today_distance_label) + " " +
                    DistanceFormatter.formatDistance(progress.todayDistanceMeters),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.home_lap_label, progress.lapProgress.lapNumber),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = stringResource(
                if (measuring) R.string.home_measuring else R.string.home_not_measuring,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = if (measuring) PixelPalette.Green else PixelPalette.Mist,
        )
    }
}

/** 次のマイルストーンまでの残りと進捗バー(P4-4 / P5-3)。 */
@Composable
private fun NextMilestonePanel(progress: ProgressSummary, next: Milestone, modifier: Modifier = Modifier) {
    PixelPanel(modifier = modifier) {
        Text(
            text = stringResource(
                R.string.home_next_milestone_label,
                next.name,
                DistanceFormatter.formatDistance(progress.remainingToNextMilestoneMeters),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        PixelProgressBar(progress = progress.milestoneRatio)
        Text(
            text = stringResource(
                R.string.home_milestone_count,
                progress.achievedMilestoneCount,
                io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog.SIZE,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = PixelPalette.Mist,
        )
    }
}

/** 周回カウンターと周内の進捗(仕様4.1 / P4-7)。 */
@Composable
private fun LapProgressPanel(progress: ProgressSummary, modifier: Modifier = Modifier) {
    PixelPanel(modifier = modifier) {
        Text(
            text = stringResource(
                R.string.home_lap_progress_label,
                DistanceFormatter.formatDistance(progress.lapProgress.distanceInLapMeters),
                DistanceFormatter.formatDistance(Earth.CIRCUMFERENCE_METERS),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        PixelProgressBar(progress = progress.lapProgress.ratio)
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

    PixelPanel(modifier = modifier, borderColor = PixelPalette.Rose) {
        Text(
            text = stringResource(R.string.home_permission_warning_title),
            style = MaterialTheme.typography.titleSmall,
            color = PixelPalette.Rose,
        )
        warnings.forEach { warning ->
            Text(
                text = "・" + stringResource(warning.messageRes),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        PixelTextButton(
            text = stringResource(R.string.home_permission_open_settings),
            onClick = onOpenSettings,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun HomeScreenPreview() {
    EarthStepTheme {
        HomeScreen(
            uiState = HomeUiState(
                progress = ProgressSummary.of(totalDistanceMeters = 12_345L, todayDistanceMeters = 2_460L),
                measuring = true,
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
