package io.github.task320.earthstep.feature.celebration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import io.github.task320.earthstep.core.designsystem.pixel.pixelCanvas
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import kotlin.random.Random

/**
 * 大台演出の四角ドットパーティクル(仕様5.3 / P5-9)。
 *
 * 円や星ではなく粗い正方形のドットで舞わせる。円形の粒を混ぜると
 * ドット絵で統一した画面から浮くため。
 *
 * 粒の初期位置と速度は演出のたびに固定の乱数から作る。毎フレーム乱数を引くと
 * 粒がちらついて舞っているように見えない。
 */
@Composable
fun PixelParticles(modifier: Modifier = Modifier, particleCount: Int = DEFAULT_PARTICLE_COUNT, seed: Int = 0) {
    val particles = remember(seed, particleCount) { createParticles(particleCount, seed) }
    val transition = rememberInfiniteTransition(label = "particles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val dotSizePx = with(LocalDensity.current) { PixelDimens.Unit.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        pixelCanvas(dotSize = dotSizePx * PARTICLE_DOTS) {
            if (columns <= 0 || rows <= 0) return@pixelCanvas
            particles.forEach { particle ->
                // 位置は 0.0〜1.0 で持ち、はみ出したぶんは上へ巻き戻す。
                val y = (particle.startY + phase * particle.speed) % 1f
                dot(
                    column = (particle.startX * columns).toInt().coerceIn(0, columns - 1),
                    row = (y * rows).toInt().coerceIn(0, rows - 1),
                    color = particle.color,
                )
            }
        }
    }
}

private data class Particle(val startX: Float, val startY: Float, val speed: Float, val color: Color)

private fun createParticles(count: Int, seed: Int): List<Particle> {
    val random = Random(seed)
    val colors = listOf(PixelPalette.Gold, PixelPalette.Bone, PixelPalette.Cyan, PixelPalette.Amber)
    return List(count) {
        Particle(
            startX = random.nextFloat(),
            startY = random.nextFloat(),
            speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED),
            color = colors[random.nextInt(colors.size)],
        )
    }
}

private const val DEFAULT_PARTICLE_COUNT = 48
private const val CYCLE_MILLIS = 2_400
private const val PARTICLE_DOTS = 2f
private const val MIN_SPEED = 0.4f
private const val MAX_SPEED = 1.4f
