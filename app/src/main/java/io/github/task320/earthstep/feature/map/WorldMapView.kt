package io.github.task320.earthstep.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.github.task320.earthstep.core.designsystem.pixel.pixelCanvas
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.core.domain.progress.LapSkin

/**
 * 2Dドット世界地図(P5-4 / P5-5)。
 *
 * 描く順は 海 → 陸 → トレイル → 現在地。あとから描くものが必ず上に来るので、
 * 陸の上を通るトレイルも欠けない。
 *
 * トレイルは階段状のドットラインで、滑らかな曲線にはしない(仕様5.1)。
 * 赤道上を東へ進む方式なので、いまは水平の直線になる。
 */
@Composable
fun WorldMapView(
    lapRatio: Float,
    lapSkin: LapSkin,
    modifier: Modifier = Modifier,
    oceanColor: Color = PixelPalette.SkyDeep,
    landColor: Color = PixelPalette.GreenDark,
    coastColor: Color = PixelPalette.Green,
) {
    val trail = WorldMapTrail.of(lapRatio)
    val trailColor = lapSkin.trailColor()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(WorldMapDots.COLUMNS.toFloat() / WorldMapDots.ROWS),
    ) {
        val dotSize = size.width / WorldMapDots.COLUMNS
        pixelCanvas(dotSize = dotSize) {
            rect(0, 0, WorldMapDots.COLUMNS, WorldMapDots.ROWS, oceanColor)

            for (row in 0 until WorldMapDots.ROWS) {
                for (column in 0 until WorldMapDots.COLUMNS) {
                    if (!WorldMapDots.isLand(column, row)) continue
                    // 海に接するドットだけ明るくして、輪郭を1ドットぶん立てる。
                    val onCoast = !WorldMapDots.isLand(column - 1, row) ||
                        !WorldMapDots.isLand(column + 1, row) ||
                        !WorldMapDots.isLand(column, row - 1) ||
                        !WorldMapDots.isLand(column, row + 1)
                    dot(column, row, if (onCoast) coastColor else landColor)
                }
            }

            trail.trailColumns.forEach { column ->
                dot(column, WorldMapDots.EQUATOR_ROW, trailColor)
            }

            // 現在地は3x3のドットで、トレイルより1段明るく置く。
            rect(
                column = (trail.headColumn - 1).coerceAtLeast(0),
                row = WorldMapDots.EQUATOR_ROW - 1,
                widthDots = HEAD_SIZE_DOTS,
                heightDots = HEAD_SIZE_DOTS,
                color = PixelPalette.Bone,
            )
        }
    }
}

private const val HEAD_SIZE_DOTS = 3

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun WorldMapViewPreview() {
    EarthStepTheme {
        WorldMapView(lapRatio = 0.42f, lapSkin = LapSkin.LAP_1)
    }
}
