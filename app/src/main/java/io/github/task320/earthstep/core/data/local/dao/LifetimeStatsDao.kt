package io.github.task320.earthstep.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.task320.earthstep.core.data.local.entity.LifetimeStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifetimeStatsDao {

    @Query("SELECT * FROM lifetime_stats WHERE id = :id")
    fun observe(id: Int): Flow<LifetimeStatsEntity?>

    @Query("SELECT * FROM lifetime_stats WHERE id = :id")
    suspend fun get(id: Int): LifetimeStatsEntity?

    /** 初期行の作成。既にあれば何もしない。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stats: LifetimeStatsEntity)

    @Query("UPDATE lifetime_stats SET total_distance_m = total_distance_m + :meters WHERE id = :id")
    suspend fun addDistance(meters: Long, id: Int)

    @Query("UPDATE lifetime_stats SET total_distance_m = :meters WHERE id = :id")
    suspend fun setTotalDistance(meters: Long, id: Int)

    @Query("UPDATE lifetime_stats SET current_lap = :lap WHERE id = :id")
    suspend fun setCurrentLap(lap: Int, id: Int)

    @Query("UPDATE lifetime_stats SET stride_length_cm = :strideLengthCm WHERE id = :id")
    suspend fun setStrideLengthCm(strideLengthCm: Double, id: Int)
}
