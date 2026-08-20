package io.github.task320.earthstep.core.data.measurement

import io.github.task320.earthstep.core.common.time.AppTimeSource
import io.github.task320.earthstep.core.domain.measurement.DailySoftCap
import io.github.task320.earthstep.core.domain.measurement.MeasurementConfig
import io.github.task320.earthstep.core.domain.measurement.MeasurementInput
import io.github.task320.earthstep.core.domain.measurement.MeasurementProcessor
import io.github.task320.earthstep.core.domain.measurement.model.ActivityUpdate
import io.github.task320.earthstep.core.domain.measurement.model.MeasurementMode
import io.github.task320.earthstep.core.domain.measurement.model.MovementState
import io.github.task320.earthstep.core.domain.measurement.model.UserActivity
import io.github.task320.earthstep.core.domain.measurement.source.ActivityRecognitionDataSource
import io.github.task320.earthstep.core.domain.measurement.source.LocationDataSource
import io.github.task320.earthstep.core.domain.measurement.source.StepDataSource
import io.github.task320.earthstep.core.domain.repository.MeasurementStateRepository
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import io.github.task320.earthstep.core.domain.usecase.RecordDistanceUseCase
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 計測エンジン(P2-21)。
 *
 * 位置・歩数・活動判定の3つの購読を1本の入力列へ束ね、[MeasurementProcessor] へ流し、
 * 確定した距離を [RecordDistanceUseCase] へ渡す。判定ロジックは持たず、
 * 「購読する」「永続化する」「日次上限をかける」だけを担当する。
 *
 * 常駐の仕組み(Foreground Service)は P3-1 で被せる。ここでは [run] が
 * キャンセルされるまで動き続けるコルーチンとして書いておく。
 */
@Singleton
class MeasurementEngine @Inject constructor(
    private val locationDataSource: LocationDataSource,
    private val stepDataSource: StepDataSource,
    private val activityRecognitionDataSource: ActivityRecognitionDataSource,
    private val progressRepository: ProgressRepository,
    private val recordDistance: RecordDistanceUseCase,
    private val measurementStateRepository: MeasurementStateRepository,
    private val timeSource: AppTimeSource,
    private val config: MeasurementConfig,
) {

    /** 計測の現在の様子。常駐通知(P3-1)と設定画面(P5-14)が読む。 */
    data class Status(
        val running: Boolean = false,
        val mode: MeasurementMode = MeasurementMode.GPS,
        val movementState: MovementState = MovementState.MOVING,
        val strideLengthCm: Double = 0.0,
        val stepSensorAvailable: Boolean = true,
        val dailyCapReached: Boolean = false,
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    private val intervalMillis = MutableStateFlow(config.locationIntervalStraightMillis)

    /**
     * まだ永続化していないメートル未満の端数。
     * 1サンプルごとに丸めると、5秒ごとの細かい加算で誤差が積もるため持ち越す。
     */
    private var carryMeters = 0.0
    private var persistedStrideCm = 0.0
    private var persistedRawSteps: Long? = null

    /** キャンセルされるまで計測を続ける。 */
    suspend fun run() {
        val stats = progressRepository.lifetimeStats.first()
        val processor = MeasurementProcessor(
            config = config,
            initialStrideLengthCm = stats.strideLengthCm,
            lastRawSteps = measurementStateRepository.lastRawSteps.first(),
        )
        val dailyCap = DailySoftCap(config.dailyCapMeters)
        val today = timeSource.today()
        dailyCap.restore(today, progressRepository.distanceMetersOn(today).first())

        processor.start(timeSource.now().toEpochMilli())
        _status.value = Status(
            running = true,
            strideLengthCm = processor.strideLengthCm,
            stepSensorAvailable = stepDataSource.isAvailable,
        )
        persistedStrideCm = stats.strideLengthCm
        persistedRawSteps = measurementStateRepository.lastRawSteps.first()
        carryMeters = 0.0

        try {
            inputs().collect { input -> apply(processor.process(input), input, dailyCap) }
        } finally {
            withContext(NonCancellable) {
                finish(processor, dailyCap)
            }
        }
    }

    /** 1件ぶんの結果を永続化と状態へ反映する。 */
    private suspend fun apply(outcome: MeasurementProcessor.Outcome, input: MeasurementInput, dailyCap: DailySoftCap) {
        carryMeters += outcome.confirmedMeters
        val whole = carryMeters.toLong()
        if (whole > 0L) {
            carryMeters -= whole
            persist(whole, input.timestampMillis, dailyCap)
        }

        if (input is MeasurementInput.Location) {
            intervalMillis.value = outcome.intervalMillis
        }
        if (abs(outcome.strideLengthCm - persistedStrideCm) >= STRIDE_PERSIST_THRESHOLD_CM) {
            persistedStrideCm = outcome.strideLengthCm
            progressRepository.setStrideLengthCm(persistedStrideCm)
        }
        val rawSteps = outcome.lastRawSteps
        if (rawSteps != null && rawSteps != persistedRawSteps) {
            persistedRawSteps = rawSteps
            measurementStateRepository.setLastRawSteps(rawSteps)
        }

        _status.value = _status.value.copy(
            mode = outcome.mode,
            movementState = outcome.movementState,
            strideLengthCm = outcome.strideLengthCm,
            dailyCapReached = dailyCap.isExhausted(timeSource.today()),
        )
    }

    /**
     * 計測終了時の後始末。寝かせていた距離を取りこぼさないよう、
     * キャンセル済みのスコープでも書き切る。
     * 収集中は端数を切り捨てて持ち越すが、最後は持ち越し先が無いので四捨五入する。
     */
    private suspend fun finish(processor: MeasurementProcessor, dailyCap: DailySoftCap) {
        val meters = (processor.flush().confirmedMeters + carryMeters).roundToLong()
        carryMeters = 0.0
        if (meters > 0L) {
            persist(meters, timeSource.now().toEpochMilli(), dailyCap)
        }
        if (persistedStrideCm != processor.strideLengthCm) {
            progressRepository.setStrideLengthCm(processor.strideLengthCm)
        }
        _status.value = _status.value.copy(running = false)
    }

    private suspend fun persist(meters: Long, timestampMillis: Long, dailyCap: DailySoftCap) {
        val at = Instant.ofEpochMilli(timestampMillis)
        val allowed = dailyCap.allow(timeSource.dateOf(at), meters)
        if (allowed > 0L) {
            // 距離の加算とマイルストーン判定は必ずこの経路を通す(P4-2)。
            recordDistance(allowed, at)
        }
        if (dailyCap.lastDiscardedMeters > 0L) {
            // 仕様1.5: 上限超過は落とすだけでなく記録に残す。
            Timber.i("daily cap reached: discarded %dm", dailyCap.lastDiscardedMeters)
        }
    }

    private fun inputs(): Flow<MeasurementInput> = merge(
        locationDataSource.locations(intervalMillis).map(MeasurementInput::Location),
        stepDataSource.steps.map(MeasurementInput::Steps),
        activityRecognitionDataSource.activities.map(MeasurementInput::Activity),
        transitionsAsActivities(),
        ticks(),
    )

    /**
     * 活動の遷移(P2-19)も活動判定として流す。
     * `ON_FOOT` へ入った/出た瞬間は、次の定期判定を待たずに対象可否へ反映したい。
     */
    private fun transitionsAsActivities(): Flow<MeasurementInput> =
        activityRecognitionDataSource.transitions.map { event ->
            val activity = if (event.isEnter) event.activity else UserActivity.UNKNOWN
            MeasurementInput.Activity(
                ActivityUpdate(
                    activity = activity,
                    confidence = TRANSITION_CONFIDENCE,
                    timestampMillis = event.timestampMillis,
                ),
            )
        }

    /** 位置が完全に途絶えたことを検知するための定期入力(P2-15)。 */
    private fun ticks(): Flow<MeasurementInput> = flow {
        while (currentCoroutineContext().isActive) {
            delay(TICK_INTERVAL_MILLIS)
            emit(MeasurementInput.Tick(timeSource.now().toEpochMilli()))
        }
    }

    private companion object {
        /** 歩幅をDBへ書き戻す変化量のしきい値(cm)。細かい変化のたびに書かない。 */
        const val STRIDE_PERSIST_THRESHOLD_CM = 0.5

        /** 遷移イベントは判定そのものなので、信頼度は最大として扱う。 */
        const val TRANSITION_CONFIDENCE = 100

        const val TICK_INTERVAL_MILLIS = 5_000L
    }
}
