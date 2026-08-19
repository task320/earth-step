package io.github.task320.earthstep.core.common.time

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.testing.FakeTimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** P1-8: 日付をまたいだら新しい日付を流す。 */
class DayTickerTest {

    @Test
    fun `ローカルの深夜0時をまたぐと次の日付を流す`() = runTest {
        // 東京の 2026-08-19 23:59:59。
        val timeSource = FakeTimeSource(
            current = Instant.parse("2026-08-19T14:59:59Z"),
            zoneId = ZoneId.of("Asia/Tokyo"),
            elapsedMillis = { testScheduler.currentTime },
        )

        DayTicker.dates(timeSource).test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 8, 19))
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 8, 20))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `タイムゾーン変更は再確認の間隔内で拾われる`() = runTest {
        // 東京の 2026-08-19 23:00。次の深夜0時までは1時間ある。
        val timeSource = FakeTimeSource(
            current = Instant.parse("2026-08-19T14:00:00Z"),
            zoneId = ZoneId.of("Asia/Tokyo"),
            elapsedMillis = { testScheduler.currentTime },
        )

        DayTicker.dates(timeSource).test {
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 8, 19))

            // 移動などでタイムゾーンが UTC+14 に変わると、その瞬間から現地は 8/20。
            // 深夜0時の待ち合わせではなく、再確認の間隔で拾われる。
            timeSource.zoneId = ZoneId.of("Pacific/Kiritimati")
            assertThat(awaitItem()).isEqualTo(LocalDate.of(2026, 8, 20))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
