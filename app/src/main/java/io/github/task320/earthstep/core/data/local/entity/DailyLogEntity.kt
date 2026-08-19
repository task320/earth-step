package io.github.task320.earthstep.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 仕様6.1 `daily_log`。日付キーはローカルタイムゾーン基準の "yyyy-MM-dd"(P1-8)。 */
@Entity(tableName = "daily_log")
data class DailyLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "distance_m")
    val distanceM: Long,
    /** 最終更新時刻(epoch millis)。同期時の参考値。 */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
