package io.github.task320.earthstep.feature.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.component.PixelPanel
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 達成記録(仕様3.3 / P5-6)。
 *
 * 100件を距離の昇順で並べる。未到達はグレーで、名称は伏せずそのまま出す。
 * 次に何を目指すのか分からないと、距離を積む動機にならないため。
 */
@Composable
fun CollectionRoute(modifier: Modifier = Modifier, viewModel: CollectionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun CollectionScreen(uiState: CollectionUiState, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<MilestoneRow?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            contentPadding = PaddingValues(PixelDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(PixelDimens.SpaceSmall),
        ) {
            item {
                Text(
                    text = stringResource(
                        R.string.collection_progress,
                        uiState.achievedCount,
                        uiState.total,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = PixelDimens.SpaceSmall),
                )
            }
            items(items = uiState.rows, key = { it.milestone.index }) { row ->
                MilestoneListItem(
                    row = row,
                    expanded = selected?.milestone?.index == row.milestone.index,
                    onClick = {
                        selected = if (selected?.milestone?.index == row.milestone.index) null else row
                    },
                )
            }
        }
    }
}

/**
 * 1件ぶんの行。タップで詳細(番号/名称/距離/説明/到達日)を開く。
 * 別画面へ遷移させないのは、100件のどこを見ていたか見失いやすいため。
 */
@Composable
private fun MilestoneListItem(
    row: MilestoneRow,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (row.achieved) PixelPalette.Bone else PixelPalette.Mist
    val accentColor = when {
        row.milestone.isMajor && row.achieved -> PixelPalette.Gold
        row.achieved -> PixelPalette.Green
        else -> PixelPalette.Outline
    }

    PixelPanel(
        modifier = modifier.clickable(onClick = onClick),
        backgroundColor = if (row.achieved) PixelPalette.Deep else PixelPalette.Night,
        borderColor = accentColor,
        contentPadding = PaddingValues(PixelDimens.SpaceSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PixelDimens.SpaceSmall),
        ) {
            Text(
                text = row.milestone.index.toString().padStart(INDEX_WIDTH, ' '),
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                modifier = Modifier.width(INDEX_COLUMN_WIDTH),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.milestone.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
                Text(
                    text = DistanceFormatter.formatDistance(row.milestone.distanceMeters),
                    style = MaterialTheme.typography.labelSmall,
                    color = PixelPalette.Mist,
                )
            }
        }

        if (expanded) {
            MilestoneDetail(row = row, contentColor = contentColor)
        }
    }
}

@Composable
private fun MilestoneDetail(row: MilestoneRow, contentColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = PixelDimens.SpaceSmall),
        verticalArrangement = Arrangement.spacedBy(PixelDimens.Unit),
    ) {
        // 一言説明は未執筆(docs 要確認事項1)。文面が入るまでは行ごと出さない。
        row.milestone.description?.let { description ->
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = contentColor)
        }
        Text(
            text = row.achievedAt?.let { stringResource(R.string.collection_achieved_at, formatDate(it)) }
                ?: stringResource(R.string.collection_not_achieved),
            style = MaterialTheme.typography.labelSmall,
            color = PixelPalette.Mist,
        )
    }
}

private fun formatDate(instant: Instant): String = DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
private const val INDEX_WIDTH = 3

@Suppress("MagicNumber") // 9ドットぶんの幅。倍率そのものが値。
private val INDEX_COLUMN_WIDTH = PixelDimens.Unit * 9

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun CollectionScreenPreview() {
    EarthStepTheme {
        CollectionScreen(
            uiState = CollectionUiState(
                rows = MilestoneCatalog.milestones.take(PREVIEW_ROWS).mapIndexed { index, milestone ->
                    MilestoneRow(
                        milestone = milestone,
                        achievedAt = if (index < 2) Instant.parse("2026-08-20T03:00:00Z") else null,
                    )
                },
                achievedCount = 2,
            ),
        )
    }
}

private const val PREVIEW_ROWS = 6
