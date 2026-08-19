package io.github.task320.earthstep.core.domain.milestone

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P1-5: マスタの不変条件を検証する。 */
class MilestoneCatalogTest {

    private val milestones = MilestoneCatalog.milestones

    @Test
    fun `マイルストーンはちょうど100件ある`() {
        assertThat(milestones).hasSize(MilestoneCatalog.SIZE)
    }

    @Test
    fun `番号は1から100まで連番である`() {
        assertThat(milestones.map { it.index }).isEqualTo((1..MilestoneCatalog.SIZE).toList())
    }

    @Test
    fun `距離は厳密な昇順である`() {
        val distances = milestones.map { it.distanceMeters }
        assertThat(distances).isInStrictOrder()
    }

    @Test
    fun `100番目は地球一周の40075km である`() {
        val last = milestones.last()
        assertThat(last.index).isEqualTo(MilestoneCatalog.SIZE)
        assertThat(last.distanceMeters).isEqualTo(40_075_000L)
        assertThat(last.distanceMeters).isEqualTo(MilestoneCatalog.EARTH_CIRCUMFERENCE_METERS)
    }

    @Test
    fun `全件が空でない名称と正の距離を持つ`() {
        milestones.forEach { milestone ->
            assertThat(milestone.name).isNotEmpty()
            assertThat(milestone.distanceMeters).isGreaterThan(0L)
        }
    }

    @Test
    fun `名称は重複しない`() {
        assertThat(milestones.map { it.name }.toSet()).hasSize(MilestoneCatalog.SIZE)
    }

    @Test
    fun `大台フラグは100番のみに立つ`() {
        // P1-6: 大台を追加する場合はこのテストも更新する(docs 要確認事項2)。
        assertThat(milestones.filter { it.isMajor }.map { it.index }).containsExactly(MilestoneCatalog.SIZE)
    }

    @Test
    fun `byIndex は範囲内で引け範囲外はnullを返す`() {
        assertThat(MilestoneCatalog.byIndex(1)?.distanceMeters).isEqualTo(300L)
        assertThat(MilestoneCatalog.byIndex(MilestoneCatalog.SIZE)?.isMajor).isTrue()
        assertThat(MilestoneCatalog.byIndex(0)).isNull()
        assertThat(MilestoneCatalog.byIndex(MilestoneCatalog.SIZE + 1)).isNull()
    }

    @Test
    fun `achievedCount は閾値ちょうどを達成扱いにする`() {
        assertThat(MilestoneCatalog.achievedCount(0L)).isEqualTo(0)
        assertThat(MilestoneCatalog.achievedCount(299L)).isEqualTo(0)
        assertThat(MilestoneCatalog.achievedCount(300L)).isEqualTo(1)
        assertThat(MilestoneCatalog.achievedCount(333L)).isEqualTo(2)
        assertThat(MilestoneCatalog.achievedCount(MilestoneCatalog.EARTH_CIRCUMFERENCE_METERS))
            .isEqualTo(MilestoneCatalog.SIZE)
    }

    @Test
    fun `nextAfter は次の未達成を返し全達成後はnullになる`() {
        assertThat(MilestoneCatalog.nextAfter(0L)?.index).isEqualTo(1)
        assertThat(MilestoneCatalog.nextAfter(300L)?.index).isEqualTo(2)
        assertThat(MilestoneCatalog.nextAfter(MilestoneCatalog.EARTH_CIRCUMFERENCE_METERS)).isNull()
    }
}
