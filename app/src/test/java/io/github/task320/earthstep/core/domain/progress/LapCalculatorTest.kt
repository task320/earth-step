package io.github.task320.earthstep.core.domain.progress

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P4-5 / P4-6: 周回と周内マーカーの計算。 */
class LapCalculatorTest {

    private val lap = Earth.CIRCUMFERENCE_METERS

    @Test
    fun `距離0は1周目の開始地点`() {
        val progress = LapCalculator.progressOf(0L)

        assertThat(progress.lapNumber).isEqualTo(1)
        assertThat(progress.distanceInLapMeters).isEqualTo(0L)
        assertThat(progress.completedLaps).isEqualTo(0)
        assertThat(progress.ratio).isEqualTo(0f)
    }

    @Test
    fun `1周ちょうどで2周目の開始地点になる`() {
        val progress = LapCalculator.progressOf(lap)

        assertThat(progress.lapNumber).isEqualTo(2)
        assertThat(progress.distanceInLapMeters).isEqualTo(0L)
        assertThat(progress.completedLaps).isEqualTo(1)
    }

    @Test
    fun `1周に1m足りなければまだ1周目`() {
        val progress = LapCalculator.progressOf(lap - 1)

        assertThat(progress.lapNumber).isEqualTo(1)
        assertThat(progress.remainingInLapMeters).isEqualTo(1L)
        assertThat(progress.completedLaps).isEqualTo(0)
    }

    @Test
    fun `周をまたいだ距離は周内の距離に折り返す`() {
        val progress = LapCalculator.progressOf(lap * 2 + 1_000L)

        assertThat(progress.lapNumber).isEqualTo(3)
        assertThat(progress.distanceInLapMeters).isEqualTo(1_000L)
        assertThat(progress.completedLaps).isEqualTo(2)
    }

    @Test
    fun `1回の更新で走破した周をすべて返す`() {
        // P4-5: 1回の更新で2周以上進む場合も取りこぼさない。
        val laps = LapCalculator.lapsCompletedBetween(previousMeters = 0L, currentMeters = lap * 3)

        assertThat(laps).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `周を越えなければ走破は無い`() {
        val laps = LapCalculator.lapsCompletedBetween(previousMeters = 100L, currentMeters = 200L)

        assertThat(laps).isEmpty()
    }

    @Test
    fun `距離が減る更新では何も起きない`() {
        assertThat(LapCalculator.lapsCompletedBetween(lap, lap)).isEmpty()
        assertThat(LapCalculator.markersReachedBetween(lap, lap - 1)).isEmpty()
    }

    @Test
    fun `周内マーカーは25_50_75パーセントの地点にある`() {
        assertThat(LapMarker.QUARTER.distanceInLapMeters).isEqualTo(10_018_750L)
        assertThat(LapMarker.HALF.distanceInLapMeters).isEqualTo(20_037_500L)
        assertThat(LapMarker.THREE_QUARTERS.distanceInLapMeters).isEqualTo(30_056_250L)
    }

    @Test
    fun `通過したマーカーを距離順に返す`() {
        val reached = LapCalculator.markersReachedBetween(previousMeters = 0L, currentMeters = lap)

        assertThat(reached.map { it.marker }).containsExactly(
            LapMarker.QUARTER,
            LapMarker.HALF,
            LapMarker.THREE_QUARTERS,
        ).inOrder()
        assertThat(reached.map { it.lapNumber }).containsExactly(1, 1, 1)
    }

    @Test
    fun `2周ぶん進んだら両方の周のマーカーが出る`() {
        val reached = LapCalculator.markersReachedBetween(previousMeters = 0L, currentMeters = lap * 2)

        assertThat(reached).hasSize(6)
        assertThat(reached.map { it.lapNumber }).containsExactly(1, 1, 1, 2, 2, 2).inOrder()
        assertThat(reached.map { it.totalDistanceMeters }).isInStrictOrder()
    }

    @Test
    fun `マーカーちょうどの距離は到達扱いになる`() {
        val reached = LapCalculator.markersReachedBetween(
            previousMeters = LapMarker.QUARTER.distanceInLapMeters - 1,
            currentMeters = LapMarker.QUARTER.distanceInLapMeters,
        )

        assertThat(reached.map { it.marker }).containsExactly(LapMarker.QUARTER)
    }

    @Test
    fun `同じマーカーを二度またがない`() {
        val first = LapCalculator.markersReachedBetween(0L, LapMarker.QUARTER.distanceInLapMeters)
        val second = LapCalculator.markersReachedBetween(
            LapMarker.QUARTER.distanceInLapMeters,
            LapMarker.QUARTER.distanceInLapMeters + 1_000L,
        )

        assertThat(first).hasSize(1)
        assertThat(second).isEmpty()
    }

    @Test
    fun `次のマーカーを返し周の終盤ではnullになる`() {
        assertThat(LapCalculator.nextMarkerAfter(0L)).isEqualTo(LapMarker.QUARTER)
        assertThat(LapCalculator.nextMarkerAfter(LapMarker.QUARTER.distanceInLapMeters))
            .isEqualTo(LapMarker.HALF)
        assertThat(LapCalculator.nextMarkerAfter(LapMarker.THREE_QUARTERS.distanceInLapMeters)).isNull()
    }

    @Test
    fun `次のマーカーは周をまたぐと先頭へ戻る`() {
        assertThat(LapCalculator.nextMarkerAfter(lap)).isEqualTo(LapMarker.QUARTER)
    }
}
