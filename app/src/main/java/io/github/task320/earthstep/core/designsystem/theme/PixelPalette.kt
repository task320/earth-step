@file:Suppress("MagicNumber") // 色定数の 0xAARRGGBB リテラルは定数化しても可読性が上がらない

package io.github.task320.earthstep.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * ドット絵向けの限定パレット(P5-1)。
 *
 * 色数を絞ることで、画面ごとに色味がばらけるのを防ぐ。
 * ドット絵は面積あたりの色数が少ないほど「それらしく」見えるため、
 * グラデーションや半透明を使わず、ここに並べた色だけで構成する。
 */
object PixelPalette {

    /** 背景。夜空に近い暗色。 */
    val Night = Color(0xFF0B0E1A)

    /** パネルや一段手前の面。 */
    val Deep = Color(0xFF161B2E)

    /** パネルの内側の影。 */
    val Shadow = Color(0xFF232A45)

    /** 枠線。 */
    val Outline = Color(0xFF3C4468)

    /** 本文。純白にせず少し黄味を残すとドット絵になじむ。 */
    val Bone = Color(0xFFE8E6D9)

    /** 補助的な文字。未到達のマイルストーンにも使う。 */
    val Mist = Color(0xFF7C849C)

    /** 陸地・プライマリ。 */
    val Green = Color(0xFF3FA34D)
    val GreenDark = Color(0xFF27682F)

    /** 海。 */
    val Sky = Color(0xFF2B5F96)
    val SkyDeep = Color(0xFF17375C)

    /** 達成・強調。 */
    val Gold = Color(0xFFF2C14E)
    val Amber = Color(0xFFD98E3A)

    /** 警告。 */
    val Rose = Color(0xFFD95763)

    /** 周回のバリエーション用。 */
    val Violet = Color(0xFF8E6BC8)
    val Cyan = Color(0xFF4ECDC4)
    val Coral = Color(0xFFE87461)
    val Lime = Color(0xFFA3D948)
}
