package io.github.task320.earthstep.core.domain.measurement

/**
 * 直近数秒ぶんの加算距離を取り消せるように保持するリングバッファ(仕様1.4 / P2-8)。
 *
 * GPSのノイズは停止する直前から出始めることが多い。そのため加算した距離をすぐ確定させず、
 * [windowMillis] だけ寝かせてから確定させる。窓の中にいる間に停止が確定したら、その分は捨てる。
 *
 * 確定を遅らせることで「DBへ書いた距離をあとから引く」という後始末を不要にしている。
 */
class RetroactiveDistanceBuffer(private val windowMillis: Long) {

    private data class Entry(val timestampMillis: Long, val meters: Double)

    private val entries = ArrayDeque<Entry>()

    /** まだ確定していない距離(m)。 */
    var pendingMeters: Double = 0.0
        private set

    fun add(timestampMillis: Long, meters: Double) {
        if (meters <= 0.0) return
        entries.addLast(Entry(timestampMillis, meters))
        pendingMeters += meters
    }

    /**
     * [nowMillis] の時点で時間窓から出た距離を確定させ、その合計を返す。
     * 返した分はバッファから消える。
     */
    fun confirmExpired(nowMillis: Long): Double {
        val threshold = nowMillis - windowMillis
        var confirmed = 0.0
        while (entries.isNotEmpty() && entries.first().timestampMillis <= threshold) {
            confirmed += entries.removeFirst().meters
        }
        pendingMeters -= confirmed
        return confirmed
    }

    /** 保持している距離をすべて捨て、捨てた合計を返す(停止確定時)。 */
    fun retractAll(): Double {
        val retracted = pendingMeters
        entries.clear()
        pendingMeters = 0.0
        return retracted
    }

    /** 保持している距離をすべて確定させ、その合計を返す(計測終了時)。 */
    fun flush(): Double {
        val flushed = pendingMeters
        entries.clear()
        pendingMeters = 0.0
        return flushed
    }
}
