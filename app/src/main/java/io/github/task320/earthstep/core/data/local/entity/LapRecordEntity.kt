package io.github.task320.earthstep.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 仕様6.1 `lap_record`。進行中の周は `completed_at` が null。 */
@Entity(tableName = "lap_record")
data class LapRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "lap_number")
    val lapNumber: Int,
    /** 走破日時(epoch millis)。進行中なら null。 */
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
)
