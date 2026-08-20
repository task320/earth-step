package io.github.task320.earthstep.core.domain.progress

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P4-8: 周回ごとの見た目の切り替わり(仕様4.3)。 */
class LapSkinTest {

    @Test
    fun `境目の周回で見た目が変わる`() {
        assertThat(LapSkin.forLap(1)).isEqualTo(LapSkin.LAP_1)
        assertThat(LapSkin.forLap(2)).isEqualTo(LapSkin.LAP_2)
        assertThat(LapSkin.forLap(3)).isEqualTo(LapSkin.LAP_3)
        assertThat(LapSkin.forLap(5)).isEqualTo(LapSkin.LAP_5)
        assertThat(LapSkin.forLap(10)).isEqualTo(LapSkin.LAP_10)
        assertThat(LapSkin.forLap(20)).isEqualTo(LapSkin.LAP_20)
        assertThat(LapSkin.forLap(50)).isEqualTo(LapSkin.LAP_50)
        assertThat(LapSkin.forLap(100)).isEqualTo(LapSkin.LAP_100)
    }

    @Test
    fun `境目の間は前の見た目を保つ`() {
        assertThat(LapSkin.forLap(4)).isEqualTo(LapSkin.LAP_3)
        assertThat(LapSkin.forLap(9)).isEqualTo(LapSkin.LAP_5)
        assertThat(LapSkin.forLap(49)).isEqualTo(LapSkin.LAP_20)
        assertThat(LapSkin.forLap(99)).isEqualTo(LapSkin.LAP_50)
        assertThat(LapSkin.forLap(150)).isEqualTo(LapSkin.LAP_100)
    }

    @Test
    fun `100周以降は100周ごとに次へ進む`() {
        assertThat(LapSkin.indexFor(100)).isEqualTo(7)
        assertThat(LapSkin.indexFor(199)).isEqualTo(7)
        assertThat(LapSkin.indexFor(200)).isEqualTo(8)
        assertThat(LapSkin.indexFor(300)).isEqualTo(9)
    }

    @Test
    fun `用意した色を使い切ったら先頭へ戻る`() {
        // 素材は8種類。100周ごとに1つ進むので、1000周目で通し番号16になり一巡する。
        // 色が足りないことで落とすより、見た目が一巡する方が害が小さい。
        assertThat(LapSkin.indexFor(900)).isEqualTo(15)
        assertThat(LapSkin.forLap(900)).isEqualTo(LapSkin.LAP_100)
        assertThat(LapSkin.indexFor(1_000)).isEqualTo(16)
        assertThat(LapSkin.forLap(1_000)).isEqualTo(LapSkin.LAP_1)
    }

    @Test
    fun `切り替わる周回だけ演出を出す`() {
        assertThat(LapSkin.changesAt(1)).isTrue()
        assertThat(LapSkin.changesAt(2)).isTrue()
        assertThat(LapSkin.changesAt(4)).isFalse()
        assertThat(LapSkin.changesAt(50)).isTrue()
        assertThat(LapSkin.changesAt(51)).isFalse()
        assertThat(LapSkin.changesAt(200)).isTrue()
        assertThat(LapSkin.changesAt(201)).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `0周目は存在しない`() {
        LapSkin.forLap(0)
    }
}
