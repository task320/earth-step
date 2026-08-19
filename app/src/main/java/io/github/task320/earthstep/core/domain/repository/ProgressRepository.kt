package io.github.task320.earthstep.core.domain.repository

import io.github.task320.earthstep.core.domain.model.DailyDistance
import io.github.task320.earthstep.core.domain.model.LapRecord
import io.github.task320.earthstep.core.domain.model.LifetimeStats
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * 距離の永続化と参照(P1-7)。
 *
 * 計測エンジン(P2)は確定した距離だけをここへ流し込む。
 * 生のGPS座標列は保存しない(仕様6.1)。
 */
interface ProgressRepository {

    /** 生涯累計の統計。レコードが無い場合も [LifetimeStats.INITIAL] を流す。 */
    val lifetimeStats: Flow<LifetimeStats>

    /** 当日の距離(m)。ローカル日付が変わると自動的に新しい日の値へ切り替わる(P1-8)。 */
    val todayDistanceMeters: Flow<Long>

    /** 指定日の距離(m)。記録が無ければ 0。 */
    fun distanceMetersOn(date: LocalDate): Flow<Long>

    /** 期間内の日別距離を日付昇順で返す。 */
    fun dailyDistances(from: LocalDate, to: LocalDate): Flow<List<DailyDistance>>

    /** 周回記録を周回番号の昇順で返す。 */
    val lapRecords: Flow<List<LapRecord>>

    /**
     * 確定した距離を加算する。日別ログと累計を1トランザクションで更新する。
     *
     * @param meters 加算する距離(m)。0以下は無視する。
     * @param at 距離が確定した時刻。日付キーはこの時刻をローカルタイムゾーンで解釈して決める。
     * @return 加算後の累計距離(m)。
     */
    suspend fun addDistance(meters: Long, at: Instant = Instant.now()): Long

    /** 周回数を設定する(P4-5 の周回繰り上げから呼ぶ)。 */
    suspend fun setCurrentLap(lap: Int)

    /** 周回の完了を記録する。同じ周回番号の再記録は無視する。 */
    suspend fun recordLapCompleted(lapNumber: Int, completedAt: Instant)

    /** 自己較正した歩幅(cm)を保存する(P2-14)。 */
    suspend fun setStrideLengthCm(strideLengthCm: Double)

    /** 日別ログの合計から累計距離を再計算して保存する(仕様6.4 の競合解決で使う)。 */
    suspend fun recalculateTotalFromDailyLogs(): Long
}
