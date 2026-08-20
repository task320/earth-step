package io.github.task320.earthstep.core.domain.measurement

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

/** P2-10: 1日100kmのソフト上限。 */
class DailySoftCapTest {

    private val today = LocalDate.of(2026, 8, 20)
    private val tomorrow = today.plusDays(1)

    @Test
    fun `上限までは全量が通る`() {
        val cap = DailySoftCap(capMeters = 100_000L)

        assertThat(cap.allow(today, 30_000L)).isEqualTo(30_000L)
        assertThat(cap.allow(today, 40_000L)).isEqualTo(40_000L)
        assertThat(cap.todayMeters).isEqualTo(70_000L)
        assertThat(cap.lastDiscardedMeters).isEqualTo(0L)
    }

    @Test
    fun `上限をまたぐ分は切り捨てられ超過量が残る`() {
        val cap = DailySoftCap(capMeters = 100_000L)
        cap.allow(today, 95_000L)

        assertThat(cap.allow(today, 8_000L)).isEqualTo(5_000L)
        assertThat(cap.lastDiscardedMeters).isEqualTo(3_000L)
        assertThat(cap.isExhausted(today)).isTrue()
    }

    @Test
    fun `上限に達した後は0しか通らない`() {
        val cap = DailySoftCap(capMeters = 100_000L)
        cap.allow(today, 100_000L)

        assertThat(cap.allow(today, 1_000L)).isEqualTo(0L)
        assertThat(cap.lastDiscardedMeters).isEqualTo(1_000L)
    }

    @Test
    fun `日付が変わるとカウンタがリセットされる`() {
        val cap = DailySoftCap(capMeters = 100_000L)
        cap.allow(today, 100_000L)

        assertThat(cap.allow(tomorrow, 20_000L)).isEqualTo(20_000L)
        assertThat(cap.todayMeters).isEqualTo(20_000L)
        assertThat(cap.isExhausted(tomorrow)).isFalse()
    }

    @Test
    fun `起動時に記録済みの距離を引き継げる`() {
        val cap = DailySoftCap(capMeters = 100_000L)
        cap.restore(today, alreadyAccumulatedMeters = 99_000L)

        assertThat(cap.allow(today, 5_000L)).isEqualTo(1_000L)
    }

    @Test
    fun `0以下の距離は通さない`() {
        val cap = DailySoftCap(capMeters = 100_000L)

        assertThat(cap.allow(today, 0L)).isEqualTo(0L)
        assertThat(cap.allow(today, -5L)).isEqualTo(0L)
        assertThat(cap.todayMeters).isEqualTo(0L)
    }
}
