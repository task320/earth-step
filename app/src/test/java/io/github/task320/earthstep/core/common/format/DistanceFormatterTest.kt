package io.github.task320.earthstep.core.common.format

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale

class DistanceFormatterTest {

    private val locale = Locale.US

    @Test
    fun `1km 未満は m 表示になる`() {
        assertThat(DistanceFormatter.formatDistance(0L, locale)).isEqualTo("0 m")
        assertThat(DistanceFormatter.formatDistance(823L, locale)).isEqualTo("823 m")
        assertThat(DistanceFormatter.formatDistance(999L, locale)).isEqualTo("999 m")
    }

    @Test
    fun `100km 未満は小数2桁の km 表示になる`() {
        assertThat(DistanceFormatter.formatDistance(1_000L, locale)).isEqualTo("1.00 km")
        // マラソンの正式距離(マイルストーン23)
        assertThat(DistanceFormatter.formatDistance(42_195L, locale)).isEqualTo("42.20 km")
    }

    @Test
    fun `10000km 未満は小数1桁の km 表示になる`() {
        assertThat(DistanceFormatter.formatDistance(100_000L, locale)).isEqualTo("100.0 km")
        // ナイル川(マイルストーン82)
        assertThat(DistanceFormatter.formatDistance(6_670_000L, locale)).isEqualTo("6,670.0 km")
    }

    @Test
    fun `10000km 以上は整数の km 表示になる`() {
        // 地球一周(マイルストーン100)
        assertThat(DistanceFormatter.formatDistance(40_075_000L, locale)).isEqualTo("40,075 km")
    }

    @Test
    fun `XP は 1m = 1XP でそのまま整数表示される`() {
        assertThat(DistanceFormatter.formatXp(0L, locale)).isEqualTo("0 XP")
        assertThat(DistanceFormatter.formatXp(40_075_000L, locale)).isEqualTo("40,075,000 XP")
    }

    @Test
    fun `負の距離は受け付けない`() {
        assertThrows(IllegalArgumentException::class.java) {
            DistanceFormatter.formatDistance(-1L, locale)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DistanceFormatter.formatXp(-1L, locale)
        }
    }
}
