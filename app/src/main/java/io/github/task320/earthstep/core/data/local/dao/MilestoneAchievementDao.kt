package io.github.task320.earthstep.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.task320.earthstep.core.data.local.entity.MilestoneAchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneAchievementDao {

    @Query("SELECT * FROM milestone_achievement ORDER BY milestone_index ASC")
    fun observeAll(): Flow<List<MilestoneAchievementEntity>>

    @Query("SELECT milestone_index FROM milestone_achievement ORDER BY milestone_index ASC")
    fun observeAchievedIndexes(): Flow<List<Int>>

    @Query("SELECT * FROM milestone_achievement ORDER BY milestone_index ASC")
    suspend fun getAll(): List<MilestoneAchievementEntity>

    /**
     * 達成の記録。既に同じ番号があれば無視する(P4-3)。
     * @return 挿入された rowId。無視された場合は -1。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(achievement: MilestoneAchievementEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(achievements: List<MilestoneAchievementEntity>)

    @Query("DELETE FROM milestone_achievement")
    suspend fun deleteAll()
}
