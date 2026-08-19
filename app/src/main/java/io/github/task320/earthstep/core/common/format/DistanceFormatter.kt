package io.github.task320.earthstep.core.common.format

import java.util.Locale

/**
 * 距離(m)と経験値(XP)の表示用フォーマッタ。
 *
 * 仕様 2.1 のとおり 1m = 1XP のため、XP は距離(m)をそのまま整数で表示する。
 * 桁数は距離帯で切り替える(序盤の数百m〜終盤の数万kmまで同じ画面に出るため):
 * - 1km 未満 : `823 m`
 * - 100km 未満 : `12.34 km`
 * - 10,000km 未満 : `1,234.5 km`
 * - それ以上 : `40,075 km`
 */
object DistanceFormatter {

    private const val METERS_PER_KM = 1_000.0
    private const val ONE_KM_IN_METERS = 1_000L
    private const val TWO_DECIMALS_UPPER_KM = 100.0
    private const val ONE_DECIMAL_UPPER_KM = 10_000.0

    fun formatDistance(meters: Long, locale: Locale = Locale.getDefault()): String {
        require(meters >= 0) { "distance must not be negative: $meters" }
        if (meters < ONE_KM_IN_METERS) {
            return String.format(locale, "%,d m", meters)
        }
        val km = meters / METERS_PER_KM
        val pattern = when {
            km < TWO_DECIMALS_UPPER_KM -> "%,.2f km"
            km < ONE_DECIMAL_UPPER_KM -> "%,.1f km"
            else -> "%,.0f km"
        }
        return String.format(locale, pattern, km)
    }

    fun formatXp(meters: Long, locale: Locale = Locale.getDefault()): String {
        require(meters >= 0) { "xp must not be negative: $meters" }
        return String.format(locale, "%,d XP", meters)
    }
}
