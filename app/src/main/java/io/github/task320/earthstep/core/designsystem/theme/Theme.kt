package io.github.task320.earthstep.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val earthStepColorScheme = darkColorScheme(
    primary = pixelGreen,
    onPrimary = pixelInk,
    secondary = pixelSky,
    onSecondary = pixelInk,
    background = pixelNight,
    onBackground = pixelBone,
    surface = pixelDeep,
    onSurface = pixelBone,
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
        colorScheme = earthStepColorScheme,
        content = content,
    )
}
