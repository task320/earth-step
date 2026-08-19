package io.github.task320.earthstep.core.common.time

import java.time.LocalDate

/**
 * `daily_log` の主キー文字列("2026-08-19" 形式 / 仕様6.1)。
 *
 * [LocalDate.toString] は ISO-8601 (`yyyy-MM-dd`) を返すため、
 * 文字列比較の昇順と日付の昇順が一致する。DAO の範囲検索はこれに依存している。
 */
object DayKey {

    fun of(date: LocalDate): String = date.toString()

    fun parse(key: String): LocalDate = LocalDate.parse(key)
}
