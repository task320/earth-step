package io.github.task320.earthstep.core.domain.backup

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.progress.Earth
import org.junit.Test

/**
 * P7-5: 2端末ぶんのバックアップを合成し、二重カウントが起きないこと・
 * 過小評価がどの範囲に収まるかを検証する(仕様6.4)。
 */
class BackupMergerTest {

    @Test
    fun `同じ日付は距離が大きい方を採る`() {
        // 2台で同じ散歩を計測した場合。合算すると二重に増えるため、多い方だけを残す。
        val phone = backup(days = mapOf("2026-08-01" to 5_000L))
        val tablet = backup(days = mapOf("2026-08-01" to 4_800L))

        val merged = BackupMerger.merge(local = phone, imported = tablet)

        assertThat(merged.dailyLog.single().distanceMeters).isEqualTo(5_000L)
        assertThat(merged.lifetimeStats.totalDistanceMeters).isEqualTo(5_000L)
    }

    @Test
    fun `同じ内容を二度取り込んでも増えない`() {
        // 同じファイルを読み直しても距離が積み上がらないこと。
        val phone = backup(days = mapOf("2026-08-01" to 5_000L, "2026-08-02" to 3_000L))

        val once = BackupMerger.merge(local = phone, imported = phone)
        val twice = BackupMerger.merge(local = once, imported = phone)

        assertThat(once.lifetimeStats.totalDistanceMeters).isEqualTo(8_000L)
        assertThat(twice.lifetimeStats.totalDistanceMeters).isEqualTo(8_000L)
    }

    @Test
    fun `別々の日に歩いた分は両方残る`() {
        // 機種変更の本来の用途。日付が違えば両方が加算される。
        val oldPhone = backup(days = mapOf("2026-08-01" to 5_000L, "2026-08-02" to 3_000L))
        val newPhone = backup(days = mapOf("2026-08-03" to 4_000L))

        val merged = BackupMerger.merge(local = newPhone, imported = oldPhone)

        assertThat(merged.dailyLog.map { it.date })
            .containsExactly("2026-08-01", "2026-08-02", "2026-08-03").inOrder()
        assertThat(merged.lifetimeStats.totalDistanceMeters).isEqualTo(12_000L)
    }

    @Test
    fun `2台持ちの日は過小評価になるが二重計上はしない`() {
        // 仕様6.4 のトレードオフ。同じ日に2台で別々に歩いた場合、
        // 実際は 5,000 + 4,000 = 9,000m 歩いていても 5,000m しか残らない。
        // 「二重に数える」より「少なく数える」方を選ぶ。
        val phone = backup(days = mapOf("2026-08-01" to 5_000L))
        val watch = backup(days = mapOf("2026-08-01" to 4_000L))

        val merged = BackupMerger.merge(local = phone, imported = watch)

        assertThat(merged.lifetimeStats.totalDistanceMeters).isEqualTo(5_000L)
        // 過小評価の幅は「その日の少ない方」に等しい。合算値の2倍にはならない。
        assertThat(merged.lifetimeStats.totalDistanceMeters).isLessThan(9_000L)
        assertThat(merged.lifetimeStats.totalDistanceMeters).isAtLeast(5_000L)
    }

    @Test
    fun `累計は端末の値ではなく日別ログから再計算する`() {
        // 端末ごとの累計値を信じると、片方が壊れていたときに引きずられる。
        val corrupted = backup(days = mapOf("2026-08-01" to 5_000L)).let {
            it.copy(lifetimeStats = it.lifetimeStats.copy(totalDistanceMeters = 999_999_999L))
        }
        val other = backup(days = mapOf("2026-08-02" to 3_000L))

        val merged = BackupMerger.merge(local = corrupted, imported = other)

        assertThat(merged.lifetimeStats.totalDistanceMeters).isEqualTo(8_000L)
    }

    @Test
    fun `周回数は再計算した累計から導く`() {
        val phone = backup(days = mapOf("2026-08-01" to Earth.CIRCUMFERENCE_METERS + 1_000L))
        val other = backup(days = emptyMap())

        val merged = BackupMerger.merge(local = phone, imported = other)

        assertThat(merged.lifetimeStats.currentLap).isEqualTo(2)
    }

    @Test
    fun `達成記録は和集合になる`() {
        val phone = backup(
            achievements = listOf(achievement(1, "2026-08-01T09:00:00Z")),
        )
        val tablet = backup(
            achievements = listOf(achievement(2, "2026-08-02T09:00:00Z")),
        )

        val merged = BackupMerger.merge(local = phone, imported = tablet)

        assertThat(merged.milestoneAchievements.map { it.milestoneIndex })
            .containsExactly(1, 2).inOrder()
    }

    @Test
    fun `同じ達成が両方にあれば早い方の日時を残す`() {
        val phone = backup(achievements = listOf(achievement(1, "2026-08-05T09:00:00Z")))
        val tablet = backup(achievements = listOf(achievement(1, "2026-08-01T09:00:00Z")))

        val merged = BackupMerger.merge(local = phone, imported = tablet)

        assertThat(merged.milestoneAchievements.single().achievedAt).isEqualTo("2026-08-01T09:00:00Z")
    }

    @Test
    fun `周回記録は和集合で早い走破日時を残す`() {
        val phone = backup(
            laps = listOf(
                BackupLapRecord(1, "2026-08-10T00:00:00Z"),
                BackupLapRecord(2, null),
            ),
        )
        val tablet = backup(laps = listOf(BackupLapRecord(1, "2026-08-08T00:00:00Z")))

        val merged = BackupMerger.merge(local = phone, imported = tablet)

        assertThat(merged.lapRecords.map { it.lapNumber }).containsExactly(1, 2).inOrder()
        assertThat(merged.lapRecords.first().completedAt).isEqualTo("2026-08-08T00:00:00Z")
    }

    @Test
    fun `進行中の周より走破済みの記録を優先する`() {
        val phone = backup(laps = listOf(BackupLapRecord(1, null)))
        val tablet = backup(laps = listOf(BackupLapRecord(1, "2026-08-08T00:00:00Z")))

        val merged = BackupMerger.merge(local = phone, imported = tablet)

        assertThat(merged.lapRecords.single().completedAt).isEqualTo("2026-08-08T00:00:00Z")
    }

    @Test
    fun `歩幅はより多く歩いた側の値を採る`() {
        // 歩幅はGPS区間の実績から少しずつ較正される(P2-14)。
        val littleWalked = backup(days = mapOf("2026-08-01" to 1_000L), strideCm = 60.0)
        val muchWalked = backup(days = mapOf("2026-08-02" to 500_000L), strideCm = 74.0)

        val merged = BackupMerger.merge(local = littleWalked, imported = muchWalked)

        assertThat(merged.lifetimeStats.strideLengthCm).isEqualTo(74.0)
    }

    @Test
    fun `書き出し日時は新しい方を引き継ぐ`() {
        val older = backup(exportedAt = "2026-08-01T00:00:00Z")
        val newer = backup(exportedAt = "2026-08-20T00:00:00Z")

        val merged = BackupMerger.merge(local = older, imported = newer)

        assertThat(merged.exportedAt).isEqualTo("2026-08-20T00:00:00Z")
    }

    @Test
    fun `空のバックアップを取り込んでも記録は減らない`() {
        val phone = backup(
            days = mapOf("2026-08-01" to 5_000L),
            achievements = listOf(achievement(1, "2026-08-01T09:00:00Z")),
        )
        val empty = backup()

        val merged = BackupMerger.merge(local = phone, imported = empty)

        assertThat(merged.lifetimeStats.totalDistanceMeters).isEqualTo(5_000L)
        assertThat(merged.milestoneAchievements).hasSize(1)
    }

    @Test
    fun `マージの結果は取り込む向きを変えても同じになる`() {
        // どちらの端末で取り込んでも同じ状態へ収束する。
        val phone = backup(
            days = mapOf("2026-08-01" to 5_000L, "2026-08-02" to 1_000L),
            achievements = listOf(achievement(1, "2026-08-01T09:00:00Z")),
            laps = listOf(BackupLapRecord(1, "2026-08-10T00:00:00Z")),
            strideCm = 70.0,
        )
        val tablet = backup(
            days = mapOf("2026-08-02" to 3_000L, "2026-08-03" to 2_000L),
            achievements = listOf(achievement(2, "2026-08-02T09:00:00Z")),
            laps = listOf(BackupLapRecord(1, "2026-08-08T00:00:00Z")),
            strideCm = 70.0,
        )

        val fromPhone = BackupMerger.merge(local = phone, imported = tablet)
        val fromTablet = BackupMerger.merge(local = tablet, imported = phone)

        assertThat(fromPhone.dailyLog).isEqualTo(fromTablet.dailyLog)
        assertThat(fromPhone.milestoneAchievements).isEqualTo(fromTablet.milestoneAchievements)
        assertThat(fromPhone.lapRecords).isEqualTo(fromTablet.lapRecords)
        assertThat(fromPhone.lifetimeStats).isEqualTo(fromTablet.lifetimeStats)
    }

    private fun backup(
        days: Map<String, Long> = emptyMap(),
        achievements: List<BackupMilestoneAchievement> = emptyList(),
        laps: List<BackupLapRecord> = emptyList(),
        strideCm: Double = 70.0,
        exportedAt: String = "2026-08-20T12:00:00Z",
    ) = BackupData(
        exportedAt = exportedAt,
        lifetimeStats = BackupLifetimeStats(
            totalDistanceMeters = days.values.sum(),
            currentLap = 1,
            strideLengthCm = strideCm,
        ),
        dailyLog = days.map { (date, meters) -> BackupDailyLog(date = date, distanceMeters = meters) },
        milestoneAchievements = achievements,
        lapRecords = laps,
    )

    private fun achievement(index: Int, achievedAt: String) = BackupMilestoneAchievement(
        milestoneIndex = index,
        achievedAt = achievedAt,
    )
}
