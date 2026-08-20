package io.github.task320.earthstep.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.task320.earthstep.core.data.measurement.FusedLocationDataSource
import io.github.task320.earthstep.core.data.measurement.PlayActivityRecognitionDataSource
import io.github.task320.earthstep.core.data.measurement.StepCounterDataSource
import io.github.task320.earthstep.core.data.repository.MeasurementStateRepositoryImpl
import io.github.task320.earthstep.core.domain.measurement.MeasurementConfig
import io.github.task320.earthstep.core.domain.measurement.source.ActivityRecognitionDataSource
import io.github.task320.earthstep.core.domain.measurement.source.LocationDataSource
import io.github.task320.earthstep.core.domain.measurement.source.StepDataSource
import io.github.task320.earthstep.core.domain.progress.ProgressEventSink
import io.github.task320.earthstep.core.domain.repository.MeasurementStateRepository
import io.github.task320.earthstep.feature.celebration.NotifyingProgressEventSink
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MeasurementModule {

    @Binds
    @Singleton
    abstract fun bindLocationDataSource(impl: FusedLocationDataSource): LocationDataSource

    @Binds
    @Singleton
    abstract fun bindStepDataSource(impl: StepCounterDataSource): StepDataSource

    @Binds
    @Singleton
    abstract fun bindActivityRecognitionDataSource(
        impl: PlayActivityRecognitionDataSource,
    ): ActivityRecognitionDataSource

    @Binds
    @Singleton
    abstract fun bindProgressEventSink(impl: NotifyingProgressEventSink): ProgressEventSink

    @Binds
    @Singleton
    abstract fun bindMeasurementStateRepository(impl: MeasurementStateRepositoryImpl): MeasurementStateRepository

    companion object {
        /** 閾値は実地検証(P8-1)で見直す。差し替えられるよう注入可能にしておく。 */
        @Provides
        @Singleton
        fun provideMeasurementConfig(): MeasurementConfig = MeasurementConfig()
    }
}
