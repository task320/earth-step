@file:Suppress("MagicNumber") // 色の 0xAARRGGBB リテラルは定数化しても可読性が上がらない

package io.github.task320.earthstep.feature.celebration

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.common.sound.SoundEffects
import io.github.task320.earthstep.core.designsystem.component.PixelButton
import io.github.task320.earthstep.core.designsystem.component.PixelPanel
import io.github.task320.earthstep.core.designsystem.component.PixelTextButton
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.progress.LapMarker
import io.github.task320.earthstep.core.domain.progress.ProgressSummary

/**
 * 達成演出(仕様3.4 / P5-7 / P5-9 / P5-10 / P5-11)。
 *
 * 演出は「通常」と「大台」の2段階。ランクを廃止したので、この2つだけを使い回す。
 * - 通常: 軽いスケールアニメーションのパネル
 * - 大台: 画面全体を覆い、四角ドットのパーティクルと一言説明を添える
 *
 * 貯まっている演出を1件ずつ順に再生し、閉じるたびに次へ進む(P5-13)。
 */
@Composable
fun CelebrationOverlay(modifier: Modifier = Modifier, viewModel: CelebrationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val celebration = uiState.current ?: return

    CelebrationContentView(
        celebration = celebration,
        progress = uiState.progress,
        remaining = uiState.remaining,
        onDismiss = viewModel::dismissCurrent,
        onSkipAll = viewModel::skipAll,
        modifier = modifier,
    )
}

@Composable
fun CelebrationContentView(
    celebration: PendingCelebration,
    progress: ProgressSummary,
    remaining: Int,
    onDismiss: () -> Unit,
    onSkipAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = CelebrationContent.of(celebration)

    // 表示のたびに小さく飛び出す。ドット絵に合わせて弾みは控えめにする。
    var shown by remember(celebration) { mutableStateOf(false) }
    LaunchedEffect(celebration) {
        shown = true
        SoundEffects.play(if (content.isMajor) SoundEffects.Sound.MAJOR else SoundEffects.Sound.MILESTONE)
    }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else INITIAL_SCALE,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "celebration-scale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (content.isMajor) MAJOR_SCRIM else NORMAL_SCRIM),
        contentAlignment = Alignment.Center,
    ) {
        if (content.isMajor) {
            PixelParticles(seed = celebration.totalDistanceMeters.toInt())
        }

        PixelPanel(
            modifier = Modifier
                .padding(PixelDimens.ScreenPadding)
                .scale(scale),
            backgroundColor = PixelPalette.Deep,
            borderColor = if (content.isMajor) PixelPalette.Gold else PixelPalette.Outline,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PixelDimens.SpaceMedium),
        ) {
            Text(
                text = headline(celebration, content),
                style = MaterialTheme.typography.titleMedium,
                color = if (content.isMajor) PixelPalette.Gold else PixelPalette.Bone,
                textAlign = TextAlign.Center,
            )
            (content.descriptionText ?: content.descriptionRes?.let { stringResource(it) })?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelPalette.Mist,
                    textAlign = TextAlign.Center,
                )
            }

            // P5-10: 名称 / ここまでの累計距離 / 次までの距離。
            Text(
                text = stringResource(
                    R.string.celebration_total_distance,
                    DistanceFormatter.formatDistance(progress.totalDistanceMeters),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = progress.nextMilestone?.let { next ->
                    stringResource(
                        R.string.celebration_next_distance,
                        next.name,
                        DistanceFormatter.formatDistance(progress.remainingToNextMilestoneMeters),
                    )
                } ?: stringResource(R.string.home_all_milestones_achieved),
                style = MaterialTheme.typography.bodySmall,
                color = PixelPalette.Mist,
                textAlign = TextAlign.Center,
            )

            PixelButton(
                text = stringResource(R.string.celebration_dismiss),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (content.isMajor) PixelPalette.Gold else PixelPalette.Green,
            )
            if (remaining > 0) {
                Text(
                    text = stringResource(R.string.celebration_remaining, remaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelPalette.Mist,
                )
                PixelTextButton(text = stringResource(R.string.celebration_skip_all), onClick = onSkipAll)
            }
        }
    }
}

@Composable
private fun headline(celebration: PendingCelebration, content: CelebrationContent): String = when (celebration) {
    is PendingCelebration.Marker -> stringResource(
        content.titleRes,
        CelebrationContent.markerLabel(celebration.marker),
    )

    else -> stringResource(content.titleRes, content.name)
}

private val NORMAL_SCRIM = Color(0xCC0B0E1A)
private val MAJOR_SCRIM = Color(0xF20B0E1A)
private const val INITIAL_SCALE = 0.85f

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun CelebrationMajorPreview() {
    EarthStepTheme {
        CelebrationContentView(
            celebration = PendingCelebration.Marker(
                LapMarker.QUARTER,
                lapNumber = 1,
                totalDistanceMeters = 10_018_750L,
            ),
            progress = ProgressSummary.of(10_018_750L, 4_200L),
            remaining = 2,
            onDismiss = {},
            onSkipAll = {},
        )
    }
}
