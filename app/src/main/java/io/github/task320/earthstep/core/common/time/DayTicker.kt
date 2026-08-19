package io.github.task320.earthstep.core.common.time

import java.time.LocalDate
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/** 日付境界をまたぐたびに新しい日付を流す(P1-8)。 */
object DayTicker {

    /**
     * タイムゾーン変更を取り込むための再確認間隔。
     * 日付変更は次の深夜0時ちょうどに起こすが、その間もこの間隔で日付を確認する。
     * 変化が無ければ [distinctUntilChanged] が下流への再通知を抑える。
     */
    private const val MAX_WAIT_MILLIS = 60L * 60L * 1_000L

    fun dates(timeSource: AppTimeSource): Flow<LocalDate> = flow {
        while (currentCoroutineContext().isActive) {
            val now = timeSource.now()
            val zone = timeSource.zone()
            val today = now.atZone(zone).toLocalDate()
            emit(today)

            val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant()
            val untilMidnight = nextMidnight.toEpochMilli() - now.toEpochMilli()
            delay(untilMidnight.coerceIn(1L, MAX_WAIT_MILLIS))
        }
    }.distinctUntilChanged()
}
