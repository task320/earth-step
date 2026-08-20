package io.github.task320.earthstep.core.domain.progress

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import org.junit.Test

/** P4-4 / P4-7: 表示用の進捗の組み立て。 */
class ProgressSummaryTest {

    @Test
    fun `開始直後は1番目のマイルストーンが次の目標`() {
        val summary = ProgressSummary.of(totalDistanceMeters = 0L, todayDistanceMeters = 0L)

        assertThat(summary.nextMilestone?.index).isEqualTo(1)
        assertThat(summary.remainingToNextMilestoneMeters).isEqualTo(300L)
        assertThat(summary.achievedMilestoneCount).isEqualTo(0)
        assertThat(summary.milestoneRatio).isEqualTo(0f)
    }

    @Test
    fun `達成した分だけ次の目標が進む`() {
        val summary = ProgressSummary.of(totalDistanceMeters = 333L, todayDistanceMeters = 333L)

        assertThat(summary.achievedMilestoneCount).isEqualTo(2)
        assertThat(summary.nextMilestone?.index).isEqualTo(3)
        assertThat(summary.remainingToNextMilestoneMeters).isEqualTo(541L - 333L)
    }

    @Test
    fun `進捗率は直前のマイルストーンからの区間で測る`() {
        // #1 の 300m と #2 の 333m の中間 = 316.5m。
        val summary = ProgressSummary.of(totalDistanceMeters = 316L, todayDistanceMeters = 0L)

        assertThat(summary.milestoneRatio).isWithin(0.03f).of(0.5f)
    }

    @Test
    fun `XPは累計距離と同じ値`() {
        val summary = ProgressSummary.of(totalDistanceMeters = 12_345L, todayDistanceMeters = 0L)

        assertThat(summary.xp).isEqualTo(12_345L)
    }

    @Test
    fun `100個すべて達成すると次の目標が無くなる`() {
        val summary = ProgressSummary.of(
            totalDistanceMeters = Earth.CIRCUMFERENCE_METERS,
            todayDistanceMeters = 0L,
        )

        assertThat(summary.nextMilestone).isNull()
        assertThat(summary.remainingToNextMilestoneMeters).isEqualTo(0L)
        assertThat(summary.achievedMilestoneCount).isEqualTo(MilestoneCatalog.SIZE)
        assertThat(summary.milestoneRatio).isEqualTo(1f)
    }

    @Test
    fun `1周目はマイルストーン一覧を出す`() {
        val summary = ProgressSummary.of(totalDistanceMeters = 1_000L, todayDistanceMeters = 0L)

        assertThat(summary.showsMilestoneList).isTrue()
        assertThat(summary.lapProgress.lapNumber).isEqualTo(1)
    }

    @Test
    fun `2周目以降はマイルストーン一覧を出さない`() {
        // 仕様4.1 / P4-7: 周回カウンターと周内マーカーのみに切り替える。
        val summary = ProgressSummary.of(
            totalDistanceMeters = Earth.CIRCUMFERENCE_METERS + 1_000L,
            todayDistanceMeters = 0L,
        )

        assertThat(summary.showsMilestoneList).isFalse()
        assertThat(summary.lapProgress.lapNumber).isEqualTo(2)
        assertThat(summary.lapProgress.distanceInLapMeters).isEqualTo(1_000L)
    }

    @Test
    fun `周回の見た目が周に応じて決まる`() {
        val secondLap = ProgressSummary.of(Earth.CIRCUMFERENCE_METERS, 0L)

        assertThat(secondLap.lapSkin).isEqualTo(LapSkin.LAP_2)
    }

    @Test
    fun `次の周内マーカーを示す`() {
        val summary = ProgressSummary.of(totalDistanceMeters = 1_000L, todayDistanceMeters = 0L)

        assertThat(summary.nextLapMarker).isEqualTo(LapMarker.QUARTER)
    }

    @Test
    fun `当日距離はそのまま持ち回る`() {
        val summary = ProgressSummary.of(totalDistanceMeters = 5_000L, todayDistanceMeters = 1_200L)

        assertThat(summary.todayDistanceMeters).isEqualTo(1_200L)
    }
}
