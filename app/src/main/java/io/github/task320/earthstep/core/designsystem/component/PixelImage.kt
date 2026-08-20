package io.github.task320.earthstep.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource

/**
 * ドット絵をぼかさずに拡大表示する(仕様5.4 / P5-2)。
 *
 * 既定の描画はバイリニア補間で、そのまま拡大するとドットの角が丸まってぼやける。
 * [FilterQuality.None] を指定するとニアレストネイバー拡大になり、
 * 1ドットが正方形のまま大きくなる。
 *
 * ドット絵の素材を出すときは必ずこの Composable を通す。
 * `Image(painterResource(...))` を直接使うとぼやける。
 */
@Composable
fun PixelImage(
    resourceId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val bitmap = ImageBitmap.imageResource(resourceId)
    Image(
        painter = BitmapPainter(image = bitmap, filterQuality = FilterQuality.None),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
