package io.github.task320.earthstep.core.domain.progress

/**
 * 周内マーカー(仕様4.2 / P4-6)。
 *
 * どの周でも同じ距離なので、1回設計すれば全周回で使い回せる。
 * 演出は3種類とも大台扱い(仕様3.4 の大台演出を流用する)。
 *
 * @param numerator 周の何割の地点か(分子)。
 * @param denominator 割合の分母。
 */
// 25% / 50% / 75% という割合そのものが値であり、定数へ切り出しても意味を持たない。
@Suppress("MagicNumber")
enum class LapMarker(private val numerator: Long, private val denominator: Long) {
    /** 25%。赤道から北極点までの距離に相当する(メートル法制定時の定義)。 */
    QUARTER(1, 4),

    /** 50%。地球の裏側(対蹠地)に到達。 */
    HALF(1, 2),

    /** 75%。節目としての数字のみを見せる。 */
    THREE_QUARTERS(3, 4),
    ;

    /** 周の開始からこのマーカーまでの距離(m)。 */
    val distanceInLapMeters: Long get() = Earth.CIRCUMFERENCE_METERS * numerator / denominator

    /** 周の何割か(0.0〜1.0)。 */
    val ratio: Float get() = numerator.toFloat() / denominator
}
