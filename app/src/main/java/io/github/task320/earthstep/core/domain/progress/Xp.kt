package io.github.task320.earthstep.core.domain.progress

/**
 * 距離から経験値への換算(仕様2.1 / P4-1)。
 *
 * 1m = 1XP。km 単位のままだと短い外出で端数XPになり達成感が薄いため、m 単位の整数XPとする。
 * 徒歩と走行を区別しないので走行時倍率などの補正は無い。
 *
 * 現状は恒等変換だが、換算式を1か所に閉じておくことで
 * 「XPとして表示している値」と「距離として表示している値」の区別を型より上のレベルで保つ。
 */
object Xp {

    /** 1m あたりのXP。 */
    const val PER_METER = 1L

    fun fromMeters(meters: Long): Long {
        require(meters >= 0) { "distance must not be negative: $meters" }
        return meters * PER_METER
    }

    /** 地球一周ぶんのXP。40,075,000XP。 */
    val forFullLap: Long get() = fromMeters(Earth.CIRCUMFERENCE_METERS)
}
