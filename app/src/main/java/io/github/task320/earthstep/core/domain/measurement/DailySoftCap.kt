package io.github.task320.earthstep.core.domain.measurement

import java.time.LocalDate

/**
 * 1日の距離のソフト上限(仕様1.5 / P2-10)。
 *
 * 上限を超えた分は加算せず、超過量を記録する。日付が変わればカウンタはリセットされる。
 * 「その日ぶんの距離」は端末のローカル日付で数えるため、日付は呼び出し側(P1-8)から渡す。
 */
class DailySoftCap(private val capMeters: Long) {

    private var date: LocalDate? = null
    private var accumulatedMeters: Long = 0L

    /** 直近の [allow] で切り捨てた距離(m)。ログ出力用。 */
    var lastDiscardedMeters: Long = 0L
        private set

    /** 現在の日付で積み上がっている距離(m)。 */
    val todayMeters: Long get() = accumulatedMeters

    /** その日の残量が尽きているか。 */
    fun isExhausted(date: LocalDate): Boolean {
        rollOverIfNeeded(date)
        return accumulatedMeters >= capMeters
    }

    /**
     * [meters] のうち加算してよい分を返す。上限に達していれば 0。
     * 返した分は内部カウンタへ加算される。
     */
    fun allow(date: LocalDate, meters: Long): Long {
        rollOverIfNeeded(date)
        if (meters <= 0L) {
            lastDiscardedMeters = 0L
            return 0L
        }
        val remaining = (capMeters - accumulatedMeters).coerceAtLeast(0L)
        val allowed = minOf(meters, remaining)
        lastDiscardedMeters = meters - allowed
        accumulatedMeters += allowed
        return allowed
    }

    /** 起動時に、その日すでに記録済みの距離をカウンタへ反映する。 */
    fun restore(date: LocalDate, alreadyAccumulatedMeters: Long) {
        this.date = date
        accumulatedMeters = alreadyAccumulatedMeters.coerceAtLeast(0L)
        lastDiscardedMeters = 0L
    }

    private fun rollOverIfNeeded(date: LocalDate) {
        if (this.date != date) {
            this.date = date
            accumulatedMeters = 0L
        }
    }
}
