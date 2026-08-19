package io.github.task320.earthstep.core.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 現在時刻とタイムゾーンの供給元(P1-8)。
 *
 * 日付境界の扱いをテスト可能にするため、実装側で `Instant.now()` / `ZoneId.systemDefault()` を
 * 直接呼ばずに必ずこのインターフェース経由で取得する。
 *
 * [zone] は呼ぶたびに評価する。端末のタイムゾーンは実行中に変わりうるため、
 * 起動時の値をキャッシュしてはいけない。
 */
interface AppTimeSource {

    fun now(): Instant

    fun zone(): ZoneId

    /** ローカルタイムゾーン基準の今日の日付。日別ログのキーになる。 */
    fun today(): LocalDate = dateOf(now())

    /** [instant] をローカルタイムゾーンで解釈した日付。 */
    fun dateOf(instant: Instant): LocalDate = instant.atZone(zone()).toLocalDate()
}
