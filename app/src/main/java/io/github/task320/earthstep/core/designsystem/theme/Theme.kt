package io.github.task320.earthstep.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EarthStepColorScheme = darkColorScheme(
    primary = PixelGreen,
    onPrimary = PixelInk,
    secondary = PixelSky,
    onSecondary = PixelInk,
    background = PixelNight,
    onBackground = PixelBone,
    surface = PixelDeep,
    onSurface = PixelBone,
)

/**
 * アプリ共通テーマ。
 *
 * ドット絵のトーンを固定したいため、ライトテーマ・ダイナミックカラーは持たず常に暗色固定。
 * ドットフォント(Press Start 2P 等)への差し替えと Typography の定義は P5-1 で行う。
 */
@Composable
fun EarthStepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EarthStepColorScheme,
        content = content,
    )
}
