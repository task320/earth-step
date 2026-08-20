package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.measurement.model.ActivityTransitionEvent
import io.github.task320.earthstep.core.domain.measurement.model.ActivityUpdate
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.StepSample
import io.github.task320.earthstep.core.domain.measurement.source.ActivityRecognitionDataSource
import io.github.task320.earthstep.core.domain.measurement.source.LocationDataSource
import io.github.task320.earthstep.core.domain.measurement.source.StepDataSource
import io.github.task320.earthstep.core.domain.repository.MeasurementStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

private const val BUFFER_CAPACITY = 64

/** テストから任意のタイミングで位置を流せる [LocationDataSource]。 */
class FakeLocationDataSource : LocationDataSource {

    private val flow = MutableSharedFlow<LocationSample>(extraBufferCapacity = BUFFER_CAPACITY)

    /** エンジンが要求した最新の更新間隔。P2-20 の反映を確認するために覚えておく。 */
    var requestedIntervalMillis: Long? = null
        private set

    override fun locations(intervalMillis: Flow<Long>): Flow<LocationSample> {
        // 実装と同じく間隔の変化を購読するが、テストでは記録するだけで購読は張り替えない。
        return flow
    }

    suspend fun emit(sample: LocationSample) {
        flow.emit(sample)
    }

    fun recordInterval(millis: Long) {
        requestedIntervalMillis = millis
    }
}

/** テストから任意のタイミングで歩数を流せる [StepDataSource]。 */
class FakeStepDataSource(override val isAvailable: Boolean = true) : StepDataSource {

    private val flow = MutableSharedFlow<StepSample>(extraBufferCapacity = BUFFER_CAPACITY)

    override val steps: Flow<StepSample> = flow

    suspend fun emit(sample: StepSample) {
        flow.emit(sample)
    }
}

/** テストから任意のタイミングで活動判定を流せる [ActivityRecognitionDataSource]。 */
class FakeActivityRecognitionDataSource : ActivityRecognitionDataSource {

    private val activityFlow = MutableSharedFlow<ActivityUpdate>(extraBufferCapacity = BUFFER_CAPACITY)
    private val transitionFlow = MutableSharedFlow<ActivityTransitionEvent>(extraBufferCapacity = BUFFER_CAPACITY)

    override val activities: Flow<ActivityUpdate> = activityFlow
    override val transitions: Flow<ActivityTransitionEvent> = transitionFlow

    suspend fun emit(update: ActivityUpdate) {
        activityFlow.emit(update)
    }

    suspend fun emit(event: ActivityTransitionEvent) {
        transitionFlow.emit(event)
    }
}

/** メモリ上だけで完結する [MeasurementStateRepository]。 */
class FakeMeasurementStateRepository(initial: Long? = null) : MeasurementStateRepository {

    private val state = MutableStateFlow(initial)

    override val lastRawSteps: Flow<Long?> = state

    override suspend fun setLastRawSteps(steps: Long) {
        state.value = steps
    }

    override suspend fun clearLastRawSteps() {
        state.value = null
    }

    fun current(): Long? = state.value
}
