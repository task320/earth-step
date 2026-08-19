package io.github.task320.earthstep.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.task320.earthstep.core.data.local.EarthStepDatabase
import io.github.task320.earthstep.core.data.local.EarthStepMigrations
import io.github.task320.earthstep.core.data.local.dao.DailyLogDao
import io.github.task320.earthstep.core.data.local.dao.LapRecordDao
import io.github.task320.earthstep.core.data.local.dao.LifetimeStatsDao
import io.github.task320.earthstep.core.data.local.dao.MilestoneAchievementDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EarthStepDatabase =
        Room.databaseBuilder(context, EarthStepDatabase::class.java, EarthStepDatabase.NAME)
            // 距離データは再取得できないため、破壊的マイグレーションは許可しない(P1-3)。
            .apply { EarthStepMigrations.ALL.forEach { migration -> addMigrations(migration) } }
            .build()

    @Provides
    fun provideDailyLogDao(database: EarthStepDatabase): DailyLogDao = database.dailyLogDao()

    @Provides
    fun provideLifetimeStatsDao(database: EarthStepDatabase): LifetimeStatsDao = database.lifetimeStatsDao()

    @Provides
    fun provideMilestoneAchievementDao(database: EarthStepDatabase): MilestoneAchievementDao =
        database.milestoneAchievementDao()

    @Provides
    fun provideLapRecordDao(database: EarthStepDatabase): LapRecordDao = database.lapRecordDao()
}
