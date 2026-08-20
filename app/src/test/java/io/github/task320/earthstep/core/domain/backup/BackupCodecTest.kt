package io.github.task320.earthstep.core.domain.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P7-1 / P7-3: 仕様6.3 の形式で読み書きできること。 */
class BackupCodecTest {

    private val sample = BackupData(
        exportedAt = "2026-08-20T12:00:00Z",
        lifetimeStats = BackupLifetimeStats(
            totalDistanceMeters = 12_345_678L,
            currentLap = 1,
            strideLengthCm = 68.5,
        ),
        dailyLog = listOf(
            BackupDailyLog(date = "2026-08-01", distanceMeters = 5_230L, updatedAt = "2026-08-01T23:00:00Z"),
        ),
        milestoneAchievements = listOf(
            BackupMilestoneAchievement(
                milestoneIndex = 1,
                achievedAt = "2026-08-01T09:15:00Z",
                distanceMetersAtAchievement = 305L,
            ),
        ),
        lapRecords = listOf(BackupLapRecord(lapNumber = 1, completedAt = null)),
    )

    @Test
    fun `書き出して読み戻すと同じ内容になる`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(sample))

        assertThat(decoded).isInstanceOf(BackupParseResult.Success::class.java)
        assertThat((decoded as BackupParseResult.Success).data).isEqualTo(sample)
    }

    @Test
    fun `仕様6_3のキー名で書き出す`() {
        val json = BackupCodec.encode(sample)

        listOf(
            "schema_version",
            "exported_at",
            "lifetime_stats",
            "total_distance_m",
            "current_lap",
            "stride_length_cm",
            "daily_log",
            "distance_m",
            "milestone_achievements",
            "milestone_index",
            "achieved_at",
            "lap_records",
            "lap_number",
        ).forEach { key ->
            assertThat(json).contains(key)
        }
    }

    @Test
    fun `仕様6_3の例をそのまま読める`() {
        // 仕様書に載っている最小形。distance_m_at_achievement と updated_at は無い。
        val json = """
            {
              "schema_version": 1,
              "exported_at": "2026-08-19T12:00:00Z",
              "lifetime_stats": {
                "total_distance_m": 12345678,
                "current_lap": 1,
                "stride_length_cm": 68.5
              },
              "daily_log": [
                {"date": "2026-08-01", "distance_m": 5230}
              ],
              "milestone_achievements": [
                {"milestone_index": 1, "achieved_at": "2026-08-01T09:15:00Z"}
              ],
              "lap_records": [
                {"lap_number": 1, "completed_at": null}
              ]
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(json)

        assertThat(decoded).isInstanceOf(BackupParseResult.Success::class.java)
        val data = (decoded as BackupParseResult.Success).data
        assertThat(data.dailyLog.single().updatedAt).isNull()
        assertThat(data.milestoneAchievements.single().distanceMetersAtAchievement).isNull()
        assertThat(data.lapRecords.single().completedAt).isNull()
    }

    @Test
    fun `知らないフィールドは無視して読む`() {
        // 新しい版で書いたJSONを古いアプリでも読めるようにするため。
        val json = BackupCodec.encode(sample).replace(
            "\"schema_version\": 1",
            "\"schema_version\": 1,\n    \"future_field\": \"something\"",
        )

        assertThat(BackupCodec.decode(json)).isInstanceOf(BackupParseResult.Success::class.java)
    }

    @Test
    fun `新しい版数のJSONは読み込まずに知らせる`() {
        // 内容を推測して取り込むと壊れた状態になりうる(P7-3)。
        val json = BackupCodec.encode(sample).replace("\"schema_version\": 1", "\"schema_version\": 2")

        val decoded = BackupCodec.decode(json)

        assertThat(decoded).isEqualTo(BackupParseResult.UnsupportedVersion(2))
    }

    @Test
    fun `版数が0以下のJSONは壊れているとみなす`() {
        val json = BackupCodec.encode(sample).replace("\"schema_version\": 1", "\"schema_version\": 0")

        assertThat(BackupCodec.decode(json)).isInstanceOf(BackupParseResult.Malformed::class.java)
    }

    @Test
    fun `JSONでない文字列は壊れているとみなす`() {
        assertThat(BackupCodec.decode("これはJSONではない")).isInstanceOf(BackupParseResult.Malformed::class.java)
        assertThat(BackupCodec.decode("")).isInstanceOf(BackupParseResult.Malformed::class.java)
    }

    @Test
    fun `必須フィールドが欠けたJSONは壊れているとみなす`() {
        val json = """{"schema_version": 1, "exported_at": "2026-08-20T12:00:00Z"}"""

        assertThat(BackupCodec.decode(json)).isInstanceOf(BackupParseResult.Malformed::class.java)
    }
}
