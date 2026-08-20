package io.github.task320.earthstep.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette

/**
 * ドットUIの枠(P5-1)。
 *
 * 角丸を使わないのがドット絵のUIらしさの要。Material の既定は角丸なので、
 * [RectangleShape] を明示して直角のまま出す。
 * 枠線の太さは1ドット([PixelDimens.BorderWidth])に揃える。
 */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelPalette.Deep,
    borderColor: Color = PixelPalette.Outline,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(PixelDimens.SpaceMedium),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PixelDimens.SpaceSmall),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor, shape = RectangleShape)
            .border(width = PixelDimens.BorderWidth, color = borderColor, shape = RectangleShape)
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
