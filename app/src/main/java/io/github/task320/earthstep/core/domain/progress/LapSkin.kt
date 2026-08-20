package io.github.task320.earthstep.core.domain.progress

/**
 * 周回ごとの見た目バリエーション(仕様4.3 / P4-8)。
 *
 * 1・2・3・5・10・20・50・100周目の8段階で切り替わり、それ以降は100周ごとに次の色へ進む。
 * 8段階は初期リリースの目安で、素材の余力に応じて増減できる(仕様4.3)。
 * 用意した色を使い切ったら先頭へ戻す。色が足りないことでクラッシュさせるより、
 * 見た目が一巡する方が害が小さい。
 */
enum class LapSkin {
    LAP_1,
    LAP_2,
    LAP_3,
    LAP_5,
    LAP_10,
    LAP_20,
    LAP_50,
    LAP_100,
    ;

    companion object {
        /** 見た目が変わる周回の境目。 */
        private val THRESHOLDS = listOf(1, 2, 3, 5, 10, 20, 50, 100)

        /** 100周を超えたあと、次の色へ進むまでの周回数。 */
        private const val LAPS_PER_SKIN_AFTER_HUNDRED = 100

        /**
         * [lapNumber] 周目で使う見た目の通し番号。
         * 100周目までは境目の数(8段階)、それ以降は100周ごとに1つ進む。
         */
        fun indexFor(lapNumber: Int): Int {
            require(lapNumber >= 1) { "lapNumber must be positive: $lapNumber" }
            if (lapNumber < THRESHOLDS.last()) {
                return THRESHOLDS.indexOfLast { it <= lapNumber }
            }
            val hundreds = lapNumber / LAPS_PER_SKIN_AFTER_HUNDRED
            return THRESHOLDS.lastIndex + (hundreds - 1)
        }

        /** [lapNumber] 周目の見た目。用意した色を超えたら先頭へ戻す。 */
        fun forLap(lapNumber: Int): LapSkin = entries[indexFor(lapNumber) % entries.size]

        /** [lapNumber] 周目で見た目が切り替わるか。演出を出すかの判断に使う。 */
        fun changesAt(lapNumber: Int): Boolean {
            require(lapNumber >= 1) { "lapNumber must be positive: $lapNumber" }
            if (lapNumber == 1) return true
            return indexFor(lapNumber) != indexFor(lapNumber - 1)
        }
    }
}
