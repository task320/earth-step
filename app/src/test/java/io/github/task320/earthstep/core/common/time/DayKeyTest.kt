package io.github.task320.earthstep.core.common.time

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.testing.FakeTimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

/** P1-8: 日付キーはローカルタイムゾーン基準で決まる。 */
class DayKeyTest {

    @Test
    fun `日付キーはISO形式で文字列比較の順序が日付順と一致する`() {
        val keys = listOf(
            LocalDate.of(2026, 8, 9),
            LocalDate.of(2026, 8, 10),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2027, 1, 1),
        ).map(DayKey::of)

        assertThat(keys).containsExactly("2026-08-09", "2026-08-10", "2026-12-31", "2027-01-01").inOrder()
        assertThat(keys).isInStrictOrder()
    }

    @Test
    fun `parse は of の逆変換になる`() {
        val date = LocalDate.of(2026, 2, 28)
        assertThat(DayKey.parse(DayKey.of(date))).isEqualTo(date)
    }

    @Test
    fun `UTCで日付が変わる時刻でも東京では前日として集計される`() {
        // 2026-08-19T23:30Z は東京では翌日 08-20 の 08:30。
        val timeSource = FakeTimeSource(
            current = Instant.parse("2026-08-19T23:30:00Z"),
            zoneId = ZoneId.of("Asia/Tokyo"),
        )
        assertThat(DayKey.of(timeSource.today())).isEqualTo("2026-08-20")

        timeSource.zoneId = ZoneId.of("UTC")
        assertThat(DayKey.of(timeSource.today())).isEqualTo("2026-08-19")
    }

    @Test
    fun `タイムゾーン変更は次の呼び出しから反映される`() {
        val timeSource = FakeTimeSource(
            current = Instant.parse("2026-08-19T14:00:00Z"),
            zoneId = ZoneId.of("Asia/Tokyo"),
        )
        assertThat(timeSource.today()).isEqualTo(LocalDate.of(2026, 8, 19))

        // 東京(UTC+9)で 8/19 23:00 の瞬間、ホノルル(UTC-10)ではまだ 8/19 04:00。
        timeSource.zoneId = ZoneId.of("Pacific/Honolulu")
        assertThat(timeSource.today()).isEqualTo(LocalDate.of(2026, 8, 19))

        // さらに時刻が進み、東京では 8/20 になってもホノルルでは 8/19 のまま。
        timeSource.current = Instant.parse("2026-08-19T16:00:00Z")
        assertThat(timeSource.today()).isEqualTo(LocalDate.of(2026, 8, 19))
        timeSource.zoneId = ZoneId.of("Asia/Tokyo")
        assertThat(timeSource.today()).isEqualTo(LocalDate.of(2026, 8, 20))
    }
}
