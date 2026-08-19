package io.github.task320.earthstep.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.task320.earthstep.core.domain.model.LifetimeStats

/**
 * 仕様6.1 `lifetime_stats`。常に1行のみ。
 * 単一行を保証するため、主キーは [SINGLETON_ID] 固定とする。
 */
@Entity(tableName = "lifetime_stats")
data class LifetimeStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "total_distance_m")
    val totalDistanceM: Long = 0L,
    @ColumnInfo(name = "current_lap")
    val currentLap: Int = 1,
    @ColumnInfo(name = "stride_length_cm")
    val strideLengthCm: Double = LifetimeStats.DEFAULT_STRIDE_LENGTH_CM,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
