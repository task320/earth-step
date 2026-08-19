package io.github.task320.earthstep.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.task320.earthstep.core.data.local.dao.DailyLogDao
import io.github.task320.earthstep.core.data.local.dao.LapRecordDao
import io.github.task320.earthstep.core.data.local.dao.LifetimeStatsDao
import io.github.task320.earthstep.core.data.local.dao.MilestoneAchievementDao
import io.github.task320.earthstep.core.data.local.entity.DailyLogEntity
import io.github.task320.earthstep.core.data.local.entity.LapRecordEntity
import io.github.task320.earthstep.core.data.local.entity.LifetimeStatsEntity
import io.github.task320.earthstep.core.data.local.entity.MilestoneAchievementEntity

/**
 * 端末内DB(仕様6.1)。
 *
 * 生のGPS座標列は保存しない。日別距離へ集計した時点で破棄する。
 * スキーマJSONは `app/schemas/` へ出力してコミットする(P1-3、[EarthStepMigrations] 参照)。
 */
@Database(
    entities = [
        DailyLogEntity::class,
        LifetimeStatsEntity::class,
        MilestoneAchievementEntity::class,
        LapRecordEntity::class,
    ],
    version = EarthStepDatabase.VERSION,
    exportSchema = true,
)
abstract class EarthStepDatabase : RoomDatabase() {

    abstract fun dailyLogDao(): DailyLogDao

    abstract fun lifetimeStatsDao(): LifetimeStatsDao

    abstract fun milestoneAchievementDao(): MilestoneAchievementDao

    abstract fun lapRecordDao(): LapRecordDao

    companion object {
        const val VERSION = 1
        const val NAME = "earthstep.db"
    }
}
