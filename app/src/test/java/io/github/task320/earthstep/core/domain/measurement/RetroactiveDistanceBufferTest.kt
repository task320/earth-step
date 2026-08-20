package io.github.task320.earthstep.core.domain.measurement

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P2-8: 遡及除外バッファ。 */
class RetroactiveDistanceBufferTest {

    @Test
    fun `窓を過ぎた分だけが確定する`() {
        val buffer = RetroactiveDistanceBuffer(windowMillis = 3_000L)
        buffer.add(timestampMillis = 1_000L, meters = 5.0)
        buffer.add(timestampMillis = 2_000L, meters = 5.0)
        buffer.add(timestampMillis = 4_000L, meters = 5.0)

        assertThat(buffer.confirmExpired(nowMillis = 4_500L)).isEqualTo(5.0)
        assertThat(buffer.pendingMeters).isEqualTo(10.0)
    }

    @Test
    fun `窓の中の距離は取り消せる`() {
        val buffer = RetroactiveDistanceBuffer(windowMillis = 3_000L)
        buffer.add(1_000L, 5.0)
        buffer.add(2_000L, 5.0)

        assertThat(buffer.retractAll()).isEqualTo(10.0)
        assertThat(buffer.pendingMeters).isEqualTo(0.0)
        assertThat(buffer.confirmExpired(nowMillis = 10_000L)).isEqualTo(0.0)
    }

    @Test
    fun `確定済みの距離は取り消せない`() {
        val buffer = RetroactiveDistanceBuffer(windowMillis = 3_000L)
        buffer.add(1_000L, 5.0)
        buffer.add(6_000L, 5.0)
        assertThat(buffer.confirmExpired(nowMillis = 6_000L)).isEqualTo(5.0)

        assertThat(buffer.retractAll()).isEqualTo(5.0)
    }

    @Test
    fun `flushは残りをすべて確定させる`() {
        val buffer = RetroactiveDistanceBuffer(windowMillis = 3_000L)
        buffer.add(1_000L, 5.0)
        buffer.add(2_000L, 7.0)

        assertThat(buffer.flush()).isEqualTo(12.0)
        assertThat(buffer.pendingMeters).isEqualTo(0.0)
    }

    @Test
    fun `0以下の距離は積まない`() {
        val buffer = RetroactiveDistanceBuffer(windowMillis = 3_000L)

        buffer.add(1_000L, 0.0)
        buffer.add(1_000L, -3.0)

        assertThat(buffer.pendingMeters).isEqualTo(0.0)
    }
}
