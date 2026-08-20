package io.github.task320.earthstep.feature.map

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.progress.LapSkin
import org.junit.Test

/** P5-4 / P5-5: 地図の座標とトレイル。 */
class WorldMapTest {

    @Test
    fun `地図は全行が同じ列数を持つ`() {
        assertThat(WorldMapDots.rows).hasSize(WorldMapDots.ROWS)
        WorldMapDots.rows.forEach { row ->
            assertThat(row).hasLength(WorldMapDots.COLUMNS)
        }
    }

    @Test
    fun `地図は陸と海の2種類だけで構成される`() {
        val characters = WorldMapDots.rows.flatMap { it.toList() }.toSet()

        assertThat(characters).containsExactly('#', '.')
    }

    @Test
    fun `列は左右にシームレスに巻き取る`() {
        // 右端の次が左端。周回のラップ処理がこれに依存している。
        val leftEdge = WorldMapDots.isLand(0, WorldMapDots.EQUATOR_ROW)

        assertThat(WorldMapDots.isLand(WorldMapDots.COLUMNS, WorldMapDots.EQUATOR_ROW)).isEqualTo(leftEdge)
        assertThat(WorldMapDots.isLand(-WorldMapDots.COLUMNS, WorldMapDots.EQUATOR_ROW)).isEqualTo(leftEdge)
    }

    @Test
    fun `範囲外の行は海として扱う`() {
        assertThat(WorldMapDots.isLand(0, -1)).isFalse()
        assertThat(WorldMapDots.isLand(0, WorldMapDots.ROWS)).isFalse()
    }

    @Test
    fun `進捗0では左端から始まる`() {
        val trail = WorldMapTrail.of(lapRatio = 0f)

        assertThat(trail.headColumn).isEqualTo(0)
        assertThat(trail.trailColumns).containsExactly(0)
    }

    @Test
    fun `進捗に応じて現在地が東へ進む`() {
        val half = WorldMapTrail.of(lapRatio = 0.5f)

        assertThat(half.headColumn).isEqualTo(WorldMapDots.COLUMNS / 2)
        assertThat(half.trailColumns).hasSize(WorldMapDots.COLUMNS / 2 + 1)
    }

    @Test
    fun `進捗1_0でも右端に収まる`() {
        // 1.0 で列数ぴったりになるため、はみ出さないよう最後の列へ寄せる。
        val full = WorldMapTrail.of(lapRatio = 1f)

        assertThat(full.headColumn).isEqualTo(WorldMapDots.COLUMNS - 1)
    }

    @Test
    fun `範囲外の進捗は0から1へ丸める`() {
        assertThat(WorldMapTrail.of(lapRatio = -0.5f).headColumn).isEqualTo(0)
        assertThat(WorldMapTrail.of(lapRatio = 2f).headColumn).isEqualTo(WorldMapDots.COLUMNS - 1)
    }

    @Test
    fun `周回ごとにトレイルの色が変わる`() {
        val colors = LapSkin.entries.map { it.trailColor() }

        assertThat(colors.toSet()).hasSize(LapSkin.entries.size)
    }
}
