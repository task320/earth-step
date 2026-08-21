package io.github.task320.earthstep.core.designsystem.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.floor

/**
 * ドット単位で描くための補助(P5-2)。
 *
 * Canvas は連続座標で描けてしまうため、何も考えずに描くとドットの境界が
 * 小数座標に落ちてアンチエイリアスでにじむ。ここを通して必ず
 * 「1ドット = 整数個のピクセル」に丸めてから描く。
 */
class PixelCanvas(private val scope: DrawScope, val dotSize: Float, private val origin: Offset = Offset.Zero) {

    /** 横方向に何ドット置けるか。 */
    val columns: Int get() = floor(scope.size.width / dotSize).toInt()

    /** 縦方向に何ドット置けるか。 */
    val rows: Int get() = floor(scope.size.height / dotSize).toInt()

    /** ドット座標 [column], [row] を1つ塗る。 */
    fun dot(column: Int, row: Int, color: Color) {
        scope.drawRect(
            color = color,
            topLeft = origin + Offset(column * dotSize, row * dotSize),
            size = Size(dotSize, dotSize),
        )
    }

    /** ドット座標の矩形を塗る。 */
    fun rect(column: Int, row: Int, widthDots: Int, heightDots: Int, color: Color) {
        if (widthDots <= 0 || heightDots <= 0) return
        scope.drawRect(
            color = color,
            topLeft = origin + Offset(column * dotSize, row * dotSize),
            size = Size(widthDots * dotSize, heightDots * dotSize),
        )
    }
}

/**
 * 1ドットの大きさを決めて [block] を実行する。
 *
 * @param dotSize 1ドットあたりのピクセル数。整数に丸めてから使うので、
 *   拡大率が半端になってドットの大きさがばらつくことがない。
 * @param gridColumns [gridRows] 描く総ドット数が分かっている場合に渡す(例: 世界地図の144x72)。
 *   `dotSize` を整数へ丸めた分だけ描画サイズがキャンバスよりわずかに小さくなるため、
 *   渡しておくと余白を左右・上下に均等配分して中央寄せする。省略時は左上起点のまま(既定挙動)。
 */
fun DrawScope.pixelCanvas(
    dotSize: Float,
    gridColumns: Int? = null,
    gridRows: Int? = null,
    block: PixelCanvas.() -> Unit,
) {
    val snapped = floor(dotSize).coerceAtLeast(1f)
    val offsetX = gridColumns?.let { ((size.width - it * snapped) / 2f).coerceAtLeast(0f) } ?: 0f
    val offsetY = gridRows?.let { ((size.height - it * snapped) / 2f).coerceAtLeast(0f) } ?: 0f
    PixelCanvas(this, snapped, Offset(offsetX, offsetY)).block()
}
