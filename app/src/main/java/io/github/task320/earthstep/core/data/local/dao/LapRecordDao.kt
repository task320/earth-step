package io.github.task320.earthstep.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.task320.earthstep.core.data.local.entity.LapRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LapRecordDao {

    @Query("SELECT * FROM lap_record ORDER BY lap_number ASC")
    fun observeAll(): Flow<List<LapRecordEntity>>

    @Query("SELECT * FROM lap_record ORDER BY lap_number ASC")
    suspend fun getAll(): List<LapRecordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(lap: LapRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(laps: List<LapRecordEntity>)

    @Query("DELETE FROM lap_record")
    suspend fun deleteAll()
}
