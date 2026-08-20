package io.github.task320.earthstep.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.component.PixelPanel
import io.github.task320.earthstep.core.designsystem.component.PixelProgressBar
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.core.domain.progress.Earth
import io.github.task320.earthstep.core.domain.progress.ProgressSummary
import io.github.task320.earthstep.feature.home.HomeViewModel

/**
 * マップ画面(P5-4 / P5-5)。
 *
 * 進捗はホームと同じ [ProgressSummary] から読む。画面ごとに別の計算をすると、
 * ホームとマップで表示がずれる余地が生まれる。
 */
@Composable
fun MapRoute(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(progress = uiState.progress, modifier = modifier)
}

@Composable
fun MapScreen(progress: ProgressSummary, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixelDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(PixelDimens.SpaceMedium, Alignment.CenterVertically),
        ) {
            WorldMapView(
                lapRatio = progress.lapProgress.ratio,
                lapSkin = progress.lapSkin,
            )

            PixelPanel {
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
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelPalette.Mist,
                )
                PixelProgressBar(
                    progress = progress.lapProgress.ratio,
                    filledColor = progress.lapSkin.trailColor(),
                )
                progress.nextLapMarker?.let { marker ->
                    Text(
                        text = stringResource(
                            R.string.map_next_marker,
                            (marker.ratio * PERCENT).toInt(),
                            DistanceFormatter.formatDistance(
                                marker.distanceInLapMeters - progress.lapProgress.distanceInLapMeters,
                            ),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private const val PERCENT = 100

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun MapScreenPreview() {
    EarthStepTheme {
        MapScreen(progress = ProgressSummary.of(totalDistanceMeters = 8_000_000L, todayDistanceMeters = 3_200L))
    }
}
