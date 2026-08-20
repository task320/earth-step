package io.github.task320.earthstep.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * 文字まわりのトークン(P5-1)。
 *
 * ドットフォント(Press Start 2P など)は P6 で同梱する。それまでは等幅フォントで代用し、
 * 差し替え点を [pixelFontFamily] の1か所に閉じておく。
 * Press Start 2P はラテン文字しか持たないため、日本語を含む本文は
 * 同梱後も等幅フォントのままにする想定(数字と見出しだけドットフォントへ寄せる)。
 *
 * 行間を広めに取っているのは、ドットフォントが字面いっぱいに描かれて詰まって見えるため。
 */
internal val pixelFontFamily = FontFamily.Monospace

private val pixelLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun pixelTextStyle(sizeSp: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = pixelFontFamily,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * LINE_HEIGHT_RATIO).sp,
    lineHeightStyle = pixelLineHeightStyle,
)

private const val LINE_HEIGHT_RATIO = 1.5f

internal val earthStepTypography = Typography(
    displaySmall = pixelTextStyle(sizeSp = 32, weight = FontWeight.Bold),
    headlineMedium = pixelTextStyle(sizeSp = 24, weight = FontWeight.Bold),
    headlineSmall = pixelTextStyle(sizeSp = 20, weight = FontWeight.Bold),
    titleLarge = pixelTextStyle(sizeSp = 20, weight = FontWeight.Bold),
    titleMedium = pixelTextStyle(sizeSp = 16, weight = FontWeight.Bold),
    titleSmall = pixelTextStyle(sizeSp = 14, weight = FontWeight.Bold),
    bodyLarge = pixelTextStyle(sizeSp = 16),
    bodyMedium = pixelTextStyle(sizeSp = 14),
    bodySmall = pixelTextStyle(sizeSp = 12),
    labelLarge = pixelTextStyle(sizeSp = 14, weight = FontWeight.Bold),
    labelMedium = pixelTextStyle(sizeSp = 12),
    labelSmall = pixelTextStyle(sizeSp = 10),
)
