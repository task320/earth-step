package io.github.task320.earthstep.core.domain.progress

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P4-1: 1m = 1XP(仕様2.1)。 */
class XpTest {

    @Test
    fun `1mが1XPになる`() {
        assertThat(Xp.fromMeters(1L)).isEqualTo(1L)
        assertThat(Xp.fromMeters(1_000L)).isEqualTo(1_000L)
    }

    @Test
    fun `地球一周は40075000XP`() {
        assertThat(Xp.forFullLap).isEqualTo(40_075_000L)
    }

    @Test
    fun `距離0はXPも0`() {
        assertThat(Xp.fromMeters(0L)).isEqualTo(0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `負の距離は受け付けない`() {
        Xp.fromMeters(-1L)
    }
}
