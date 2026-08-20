package io.github.task320.earthstep.core.domain.progress

/** ゲームの基準になる地球の寸法。 */
object Earth {

    /**
     * 地球一周の距離(m)。赤道全周 40,075km(仕様0)。
     *
     * 1周ぶんの距離であり、100番目のマイルストーンの距離でもある。
     * 周内マーカー(仕様4.2)もこの値からの割合で決まる。
     */
    const val CIRCUMFERENCE_METERS = 40_075_000L
}
