package io.github.task320.earthstep.core.designsystem.component

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import io.github.task320.earthstep.core.common.sound.SoundEffects
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette

/** 角丸を持たないボタン(P5-1)。 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = PixelPalette.Green,
    contentColor: Color = PixelPalette.Night,
) {
    Button(
        onClick = {
            SoundEffects.play(SoundEffects.Sound.TAP)
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** 補助操作用。枠を持たず文字だけで見せる。 */
@Composable
fun PixelTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = {
            SoundEffects.play(SoundEffects.Sound.TAP)
            onClick()
        },
        modifier = modifier,
        shape = RectangleShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = PixelPalette.Mist,
        )
    }
}
