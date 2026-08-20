package io.github.task320.earthstep.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val earthStepColorScheme = darkColorScheme(
    primary = PixelPalette.Green,
    onPrimary = PixelPalette.Night,
    primaryContainer = PixelPalette.GreenDark,
    onPrimaryContainer = PixelPalette.Bone,
    secondary = PixelPalette.Sky,
    onSecondary = PixelPalette.Bone,
    secondaryContainer = PixelPalette.SkyDeep,
    onSecondaryContainer = PixelPalette.Bone,
    tertiary = PixelPalette.Gold,
    onTertiary = PixelPalette.Night,
    background = PixelPalette.Night,
    onBackground = PixelPalette.Bone,
    surface = PixelPalette.Deep,
    onSurface = PixelPalette.Bone,
    surfaceVariant = PixelPalette.Shadow,
    onSurfaceVariant = PixelPalette.Mist,
    outline = PixelPalette.Outline,
    error = PixelPalette.Rose,
    onError = PixelPalette.Night,
    errorContainer = PixelPalette.Shadow,
    onErrorContainer = PixelPalette.Rose,
)

/**
 * アプリ共通テーマ(P5-1)。
 *
 * ドット絵のトーンを固定したいため、ライトテーマ・ダイナミックカラーは持たず常に暗色固定。
 * 端末の壁紙に合わせて色が変わると、限定パレットで作った素材と噛み合わなくなる。
 */
@Composable
fun EarthStepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = earthStepColorScheme,
        typography = earthStepTypography,
        content = content,
    )
}
