package io.github.task320.earthstep.core.domain.celebration

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.progress.LapMarker
import io.github.task320.earthstep.core.domain.progress.ProgressEvent
import org.junit.Test

/** P5-13: 演出キューの保存形式。 */
class PendingCelebrationCodecTest {

    @Test
    fun `3種類の演出を往復できる`() {
        val celebrations = listOf(
            PendingCelebration.Milestone(milestoneIndex = 23, totalDistanceMeters = 42_200L),
            PendingCelebration.Marker(LapMarker.HALF, lapNumber = 2, totalDistanceMeters = 60_112_500L),
            PendingCelebration.Lap(lapNumber = 1, totalDistanceMeters = 40_075_000L),
        )

        val decoded = PendingCelebrationCodec.decode(PendingCelebrationCodec.encode(celebrations))

        assertThat(decoded).containsExactlyElementsIn(celebrations).inOrder()
    }

    @Test
    fun `空文字からは何も復元しない`() {
        assertThat(PendingCelebrationCodec.decode("")).isEmpty()
    }

    @Test
    fun `壊れた行は黙って捨てる`() {
        // 演出を1回取りこぼすだけなので、読めない行で落とさない。
        val encoded = listOf(
            "M|23|42200",
            "こわれている",
            "M|not-a-number|1",
            "K|UNKNOWN_MARKER|1|100",
            "L|1|40075000",
        ).joinToString("\n")

        val decoded = PendingCelebrationCodec.decode(encoded)

        assertThat(decoded).containsExactly(
            PendingCelebration.Milestone(23, 42_200L),
            PendingCelebration.Lap(1, 40_075_000L),
        ).inOrder()
    }

    @Test
    fun `ProgressEventから変換できる`() {
        val milestone = MilestoneCatalog.byIndex(1)!!
        val event = ProgressEvent.MilestoneAchieved(milestone, totalDistanceMeters = 300L)

        val celebration = PendingCelebration.from(event)

        assertThat(celebration).isEqualTo(PendingCelebration.Milestone(1, 300L))
    }

    @Test
    fun `大台かどうかは元のマイルストーンから引く`() {
        assertThat(PendingCelebration.Milestone(1, 300L).isMajor).isFalse()
        assertThat(PendingCelebration.Milestone(MilestoneCatalog.SIZE, 40_075_000L).isMajor).isTrue()
    }

    @Test
    fun `周回とマーカーは常に大台扱い`() {
        assertThat(PendingCelebration.Lap(1, 40_075_000L).isMajor).isTrue()
        assertThat(PendingCelebration.Marker(LapMarker.THREE_QUARTERS, 1, 1L).isMajor).isTrue()
    }
}
