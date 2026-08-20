package io.github.task320.earthstep.core.domain.backup

import io.github.task320.earthstep.core.domain.progress.LapCalculator

/**
 * 端末間の競合解決(仕様6.4 / P7-4)。
 *
 * ## なぜ単純な合算にしないか
 * 同じ外出を2台で計測していた場合、合算すると距離が二重に増える。
 * 一度増えた距離は取り消せず、マイルストーンも周回も先に進んでしまうため、
 * 「二重に数える」より「少なく数える」方を選ぶ。
 *
 * ## 解決のしかた
 * - `daily_log`: 日付をキーに、距離が大きい方を採る
 * - `milestone_achievement` / `lap_record`: 和集合。日時が両方にあれば早い方を採る
 * - `total_distance_m`: 日別ログの合計から再計算する。端末ごとの累計値は比較も合算もしない
 * - `current_lap`: 再計算した累計距離から導く(P4 と同じく累計距離を唯一の真実にする)
 *
 * ## トレードオフ
 * 2台を同時に持って同じ散歩を計測した場合、その日は片方ぶんしか残らないので過小評価になる。
 * 別々の日に別々の端末で歩いた場合は、日付が違うので両方が残る。
 */
object BackupMerger {

    /**
     * [local](いまの端末)と [imported](読み込んだJSON)を突き合わせる。
     *
     * @return 取り込むべき最終状態。`exported_at` は新しい方を引き継ぐ。
     */
    fun merge(local: BackupData, imported: BackupData): BackupData {
        val dailyLog = mergeDailyLogs(local.dailyLog, imported.dailyLog)
        val totalMeters = dailyLog.sumOf { it.distanceMeters }

        return BackupData(
            schemaVersion = BackupData.CURRENT_SCHEMA_VERSION,
            exportedAt = maxOf(local.exportedAt, imported.exportedAt),
            lifetimeStats = BackupLifetimeStats(
                totalDistanceMeters = totalMeters,
                currentLap = LapCalculator.progressOf(totalMeters).lapNumber,
                strideLengthCm = mergeStride(local, imported),
            ),
            dailyLog = dailyLog,
            milestoneAchievements = mergeAchievements(
                local.milestoneAchievements,
                imported.milestoneAchievements,
            ),
            lapRecords = mergeLapRecords(local.lapRecords, imported.lapRecords),
        )
    }

    /** 同じ日付があれば距離が大きい方。日付は文字列比較で昇順に並ぶ(P1-8)。 */
    private fun mergeDailyLogs(local: List<BackupDailyLog>, imported: List<BackupDailyLog>): List<BackupDailyLog> =
        (local + imported)
            .groupBy { it.date }
            .map { (_, entries) -> entries.maxBy { it.distanceMeters } }
            .sortedBy { it.date }

    /** 和集合。両方にあれば達成日時が早い方を残す。 */
    private fun mergeAchievements(
        local: List<BackupMilestoneAchievement>,
        imported: List<BackupMilestoneAchievement>,
    ): List<BackupMilestoneAchievement> = (local + imported)
        .groupBy { it.milestoneIndex }
        .map { (_, entries) -> entries.minBy { it.achievedAt } }
        .sortedBy { it.milestoneIndex }

    /**
     * 和集合。走破日時が両方にあれば早い方、片方だけなら入っている方を残す。
     * 進行中(null)より走破済みの記録を優先する。
     */
    private fun mergeLapRecords(local: List<BackupLapRecord>, imported: List<BackupLapRecord>): List<BackupLapRecord> =
        (local + imported)
            .groupBy { it.lapNumber }
            .map { (lapNumber, entries) ->
                val earliest = entries.mapNotNull { it.completedAt }.minOrNull()
                BackupLapRecord(lapNumber = lapNumber, completedAt = earliest)
            }
            .sortedBy { it.lapNumber }

    /**
     * 歩幅は、より多く歩いた側の値を採る。
     * 歩幅はGPS区間の実績から少しずつ較正されるため(P2-14)、
     * 歩いた距離が長い側の方が本人の歩幅に寄っている。
     */
    private fun mergeStride(local: BackupData, imported: BackupData): Double {
        val localMeters = local.dailyLog.sumOf { it.distanceMeters }
        val importedMeters = imported.dailyLog.sumOf { it.distanceMeters }
        return if (importedMeters > localMeters) {
            imported.lifetimeStats.strideLengthCm
        } else {
            local.lifetimeStats.strideLengthCm
        }
    }
}
