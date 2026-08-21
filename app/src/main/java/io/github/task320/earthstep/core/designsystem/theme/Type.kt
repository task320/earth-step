package io.github.task320.earthstep.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import io.github.task320.earthstep.R

/**
 * 文字まわりのトークン(P5-1 / P6-1)。
 *
 * ドットフォントは PixelMplus12(M+ FONT LICENSE、JIS第1・第2水準の漢字を収録)。
 * Press Start 2P はラテン文字しか持たず日本語本文に使えないため、本文・見出しとも
 * これ1本に統一している(仕様ドキュメント H-1 の判断)。差し替え点は [pixelFontFamily] の1か所。
 *
 * 行間を広めに取っているのは、ドットフォントが字面いっぱいに描かれて詰まって見えるため。
 *
 * **Boldは同梱していない**。配布元の `PixelMplus12-Bold.ttf` は "m" など一部のラテン文字の
 * グリフ(輪郭データ)が壊れており、実機で潰れた塗りつぶしとして表示されることを確認した
 * (`cmap` は正しく解決するが `glyf` の輪郭が本来より少ない/不正)。Regular 1本だけを登録し、
 * Bold要求時は Compose の合成太字([FontSynthesis])に任せる。
 */
internal val pixelFontFamily = FontFamily(
    Font(R.font.pixelmplus12_regular, weight = FontWeight.Normal),
)

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
