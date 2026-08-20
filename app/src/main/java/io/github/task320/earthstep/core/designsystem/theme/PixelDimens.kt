package io.github.task320.earthstep.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * 余白と線幅のトークン(P5-1)。
 *
 * すべて [PixelDimens.Unit] の整数倍にする。ドット絵のUIは
 * 「1ドットの大きさ」を揃えないと、枠線の太さや余白がばらついて素材が浮いて見える。
 */
// 余白はすべて Unit の整数倍。倍率そのものが値なので定数へ切り出しても意味を持たない。
@Suppress("MagicNumber")
object PixelDimens {

    /** 1ドットぶんの大きさ。すべての寸法の基準。 */
    val Unit = 4.dp

    val BorderWidth = Unit
    val SpaceSmall = Unit * 2
    val SpaceMedium = Unit * 3
    val SpaceLarge = Unit * 6
    val ScreenPadding = Unit * 4

    /** 進捗バーの高さ。 */
    val ProgressHeight = Unit * 4
}
