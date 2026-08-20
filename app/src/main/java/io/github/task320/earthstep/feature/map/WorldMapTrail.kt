package io.github.task320.earthstep.feature.map

import androidx.compose.ui.graphics.Color
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.core.domain.progress.LapSkin

/**
 * トレイルの描画に必要な値(P5-4 / P5-5)。
 *
 * 現在地は「赤道上を東へ進む単純方式」(仕様5.1 の決定)で決まる。
 * 周内進捗の割合をそのまま地図のX座標へ写像し、Yは赤道に固定する。
 *
 * @param headColumn 現在地のドット列。
 * @param trailColumns 出発点から現在地までの列。右端を越えたぶんは左端から続く。
 * @param wrapped 今の周ですでに地図の右端を越えているか。
 */
data class WorldMapTrail(val headColumn: Int, val trailColumns: List<Int>, val wrapped: Boolean) {
    companion object {

        /**
         * 周内の進捗 [lapRatio](0.0〜1.0)からトレイルを組み立てる。
         *
         * 地図の左端を周の開始地点とする。実在のルートをなぞらないので、
         * 出発点をどこに置いても等価。左端にすると右端でのラップが素直に噛み合う。
         */
        fun of(lapRatio: Float, columns: Int = WorldMapDots.COLUMNS): WorldMapTrail {
            require(columns > 0) { "columns must be positive: $columns" }
            val clamped = lapRatio.coerceIn(0f, 1f)

            // 1.0 のとき列数ぴったりになるので、最後の列に収める。
            val head = (clamped * columns).toInt().coerceAtMost(columns - 1)
            return WorldMapTrail(
                headColumn = head,
                trailColumns = (0..head).toList(),
                wrapped = false,
            )
        }
    }
}

/**
 * 周回ごとのトレイル色(仕様4.3 / P4-8)。
 *
 * 8段階のスキンに限定パレットの色を割り当てる。
 * 地図の海(青)と陸(緑)から浮く色を選び、どの周でもトレイルが埋もれないようにする。
 */
fun LapSkin.trailColor(): Color = when (this) {
    LapSkin.LAP_1 -> PixelPalette.Gold
    LapSkin.LAP_2 -> PixelPalette.Cyan
    LapSkin.LAP_3 -> PixelPalette.Coral
    LapSkin.LAP_5 -> PixelPalette.Violet
    LapSkin.LAP_10 -> PixelPalette.Lime
    LapSkin.LAP_20 -> PixelPalette.Amber
    LapSkin.LAP_50 -> PixelPalette.Bone
    LapSkin.LAP_100 -> PixelPalette.Rose
}
