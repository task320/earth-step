package io.github.task320.earthstep.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import io.github.task320.earthstep.core.designsystem.pixel.pixelCanvas
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette

/**
 * ドットで刻んだ進捗バー(P5-1 / P5-3)。
 *
 * 連続した帯ではなく、1ドットずつ埋まっていくブロックとして描く。
 * 滑らかに伸びるバーはドット絵の画面から浮くうえ、
 * 「あと何ドットで次のマイルストーン」という見え方の方がゲームらしい。
 */
@Composable
fun PixelProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    filledColor: Color = PixelPalette.Green,
    trackColor: Color = PixelPalette.Shadow,
    borderColor: Color = PixelPalette.Outline,
) {
    val dotSizePx = with(LocalDensity.current) { PixelDimens.Unit.toPx() }
    val clamped = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(PixelDimens.ProgressHeight),
    ) {
        pixelCanvas(dotSize = dotSizePx) {
            if (columns < MIN_COLUMNS || rows < MIN_ROWS) return@pixelCanvas

            // 外周1ドットを枠線にし、内側を軌道として塗り分ける。
            rect(0, 0, columns, rows, borderColor)
            val innerColumns = columns - BORDER_DOTS * 2
            val innerRows = rows - BORDER_DOTS * 2
            rect(BORDER_DOTS, BORDER_DOTS, innerColumns, innerRows, trackColor)

            // 端数のドットは切り捨てる。半端に塗ると1ドットの大きさが揃わない。
            val filled = (innerColumns * clamped).toInt()
            rect(BORDER_DOTS, BORDER_DOTS, filled, innerRows, filledColor)
        }
    }
}

private const val BORDER_DOTS = 1
private const val MIN_COLUMNS = 4
private const val MIN_ROWS = 3
