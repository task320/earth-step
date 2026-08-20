package io.github.task320.earthstep.core.domain.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * エクスポート/インポートで受け渡すデータ(仕様6.3 / P7-1)。
 *
 * DBスキーマ(仕様6.1)をそのままシリアライズする。手動エクスポートと
 * クラウド同期(P7-7)の両方でこの形式を共用する。
 *
 * 日時は ISO-8601(UTC)の文字列で持つ。epoch millis のままだと、
 * 書き出したJSONを人が開いたときに中身を確認できない。
 *
 * @param schemaVersion 将来テーブル構成が変わっても古いバックアップを読めるようにするための版数。
 */
@Serializable
data class BackupData(
    @SerialName("schema_version")
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @SerialName("exported_at")
    val exportedAt: String,
    @SerialName("lifetime_stats")
    val lifetimeStats: BackupLifetimeStats,
    @SerialName("daily_log")
    val dailyLog: List<BackupDailyLog> = emptyList(),
    @SerialName("milestone_achievements")
    val milestoneAchievements: List<BackupMilestoneAchievement> = emptyList(),
    @SerialName("lap_records")
    val lapRecords: List<BackupLapRecord> = emptyList(),
) {
    companion object {
        /** 現在の形式の版数。テーブル構成を変えたら上げる。 */
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupLifetimeStats(
    @SerialName("total_distance_m")
    val totalDistanceMeters: Long,
    @SerialName("current_lap")
    val currentLap: Int,
    @SerialName("stride_length_cm")
    val strideLengthCm: Double,
)

/** @param date ローカルタイムゾーン基準の "yyyy-MM-dd"(P1-8)。 */
@Serializable
data class BackupDailyLog(
    @SerialName("date")
    val date: String,
    @SerialName("distance_m")
    val distanceMeters: Long,
    /**
     * 最終更新時刻。仕様6.3の例には無いが、同期の参考値として書き出す。
     * 古いバックアップには入っていないので、読めなければ 0 として扱う。
     */
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

/**
 * @param distanceMetersAtAchievement 達成時点の累計距離。
 *   仕様6.3の例では省略されているが、DBスキーマ(仕様6.1)は持っている列なので書き出す。
 *   読み込み時に無ければ、そのマイルストーンの閾値距離で埋める。
 */
@Serializable
data class BackupMilestoneAchievement(
    @SerialName("milestone_index")
    val milestoneIndex: Int,
    @SerialName("achieved_at")
    val achievedAt: String,
    @SerialName("distance_m_at_achievement")
    val distanceMetersAtAchievement: Long? = null,
)

/** @param completedAt 走破日時。進行中の周は null。 */
@Serializable
data class BackupLapRecord(
    @SerialName("lap_number")
    val lapNumber: Int,
    @SerialName("completed_at")
    val completedAt: String? = null,
)
