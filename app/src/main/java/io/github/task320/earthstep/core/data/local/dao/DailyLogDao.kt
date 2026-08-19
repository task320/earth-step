package io.github.task320.earthstep.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.task320.earthstep.core.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {

    /**
     * 加算対象の行が無ければ距離0で作る。
     *
     * SQLite の UPSERT (`ON CONFLICT ... DO UPDATE`) は 3.24 以降の構文で、
     * minSdk 26 の端末(Android 8.0 は SQLite 3.18)では使えない。
     * そのため「行を用意する」→「加算する」の2文に分け、
     * 呼び出し側のトランザクションで束ねる(ProgressRepositoryImpl 参照)。
     */
    @Query("INSERT OR IGNORE INTO daily_log (date, distance_m, updated_at) VALUES (:date, 0, :updatedAt)")
    suspend fun insertIfAbsent(date: String, updatedAt: Long)

    /** 既存の行へ距離を加算する。行が無ければ何も起きない。 */
    @Query("UPDATE daily_log SET distance_m = distance_m + :meters, updated_at = :updatedAt WHERE date = :date")
    suspend fun incrementDistance(date: String, meters: Long, updatedAt: Long)

    @Query("SELECT distance_m FROM daily_log WHERE date = :date")
    fun observeDistance(date: String): Flow<Long?>

    @Query("SELECT * FROM daily_log WHERE date = :date")
    suspend fun findByDate(date: String): DailyLogEntity?

    @Query("SELECT * FROM daily_log WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeRange(from: String, to: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_log ORDER BY date ASC")
    suspend fun getAll(): List<DailyLogEntity>

    /** 日別ログの合計(仕様6.4 の累計再計算に使う)。1件も無ければ 0。 */
    @Query("SELECT COALESCE(SUM(distance_m), 0) FROM daily_log")
    suspend fun sumAll(): Long

    /** インポート時の一括投入。同じ日付は上書きする(競合解決は呼び出し側で済ませる)。 */
    @Upsert
    suspend fun upsertAll(logs: List<DailyLogEntity>)

    @Query("DELETE FROM daily_log")
    suspend fun deleteAll()
}
