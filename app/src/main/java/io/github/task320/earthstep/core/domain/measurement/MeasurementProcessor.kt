package io.github.task320.earthstep.core.domain.measurement

import io.github.task320.earthstep.core.domain.measurement.model.ActivityUpdate
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.MeasurementMode
import io.github.task320.earthstep.core.domain.measurement.model.MovementState
import io.github.task320.earthstep.core.domain.measurement.model.SegmentRejection
import io.github.task320.earthstep.core.domain.measurement.model.StepSample
import io.github.task320.earthstep.core.domain.measurement.model.UserActivity
import io.github.task320.earthstep.core.domain.measurement.step.FallbackController
import io.github.task320.earthstep.core.domain.measurement.step.StepCounterTracker
import io.github.task320.earthstep.core.domain.measurement.step.StrideCalibrator
import io.github.task320.earthstep.core.domain.model.LifetimeStats

/**
 * 計測の中核となる純粋な状態機械(P2-21)。
 *
 * 入力([MeasurementInput])を1件ずつ受け取り、確定した距離とモードを返すだけで、
 * Android のAPIにもコルーチンにも依存しない。購読と永続化は
 * `core.data.measurement.MeasurementEngine` が担う。
 *
 * ここで組み合わせている部品:
 * - [DistanceAccumulator] : 精度・速度・停止のフィルタ(P2-3〜P2-9)
 * - [FallbackController] : GPS/歩数モードの切り替え(P2-15/P2-16)
 * - [StepCounterTracker] : 再起動によるカウンタリセットの補正(P2-13)
 * - [StrideCalibrator] : 歩幅の自己較正(P2-14)
 * - [LocationIntervalPlanner] : 位置更新間隔の適応(P2-20)
 */
class MeasurementProcessor(
    private val config: MeasurementConfig = MeasurementConfig(),
    initialStrideLengthCm: Double = LifetimeStats.DEFAULT_STRIDE_LENGTH_CM,
    lastRawSteps: Long? = null,
) {

    /**
     * 1件の入力を処理した結果。
     *
     * @param confirmedMeters 永続化してよい確定距離。日次上限(P2-10)は呼び出し側で適用する。
     * @param rejection 距離にしなかった理由。
     */
    data class Outcome(
        val confirmedMeters: Double = 0.0,
        val pendingMeters: Double = 0.0,
        val retractedMeters: Double = 0.0,
        val rejection: SegmentRejection? = null,
        val mode: MeasurementMode = MeasurementMode.GPS,
        val movementState: MovementState = MovementState.MOVING,
        val strideLengthCm: Double = LifetimeStats.DEFAULT_STRIDE_LENGTH_CM,
        val lastRawSteps: Long? = null,
        val intervalMillis: Long = 0L,
    )

    private val accumulator = DistanceAccumulator(config)
    private val fallbackController = FallbackController(config)
    private val stepTracker = StepCounterTracker(lastRawSteps)
    private val intervalPlanner = LocationIntervalPlanner(config)
    private val strideCalibrator = StrideCalibrator(config, initialStrideLengthCm)

    /** GPSモード中、次の位置サンプルまでに数えた歩数。停止判定と歩幅較正に使う。 */
    private var stepsSinceLocation = 0L

    /** 歩幅較正のために貯めているGPS距離と歩数。 */
    private var calibrationMeters = 0.0
    private var calibrationSteps = 0L

    /** 直近の活動判定がXP対象(人力移動)かどうか(仕様1.1)。 */
    private var onFoot = true

    val mode: MeasurementMode get() = fallbackController.mode
    val movementState: MovementState get() = accumulator.movementState
    val strideLengthCm: Double get() = strideCalibrator.strideLengthCm
    val intervalMillis: Long get() = intervalPlanner.intervalMillis

    /** 計測開始。フォールバック判定の基準時刻を入れる。 */
    fun start(nowMillis: Long) {
        fallbackController.start(nowMillis)
    }

    fun process(input: MeasurementInput): Outcome = when (input) {
        is MeasurementInput.Location -> onLocation(input.sample)
        is MeasurementInput.Steps -> onSteps(input.sample)
        is MeasurementInput.Activity -> onActivity(input.update)
        is MeasurementInput.Tick -> onTick(input.timestampMillis)
    }

    /** 計測終了時に、寝かせている距離を確定させる。 */
    fun flush(): Outcome = outcome(accumulator.flush().confirmedMeters, rejection = null)

    private fun onActivity(update: ActivityUpdate): Outcome {
        val confident = update.confidence >= config.minOnFootConfidence
        when {
            update.activity == UserActivity.ON_FOOT && confident -> onFoot = true
            update.activity == UserActivity.ON_BICYCLE && confident -> stopCountingAsHuman()
            update.activity == UserActivity.IN_VEHICLE && confident -> stopCountingAsHuman()
            // STILL / UNKNOWN / 信頼度が低い判定では対象可否を変えない。
            // 静止は停止判定(仕様1.4)が速度と歩数から確定させる方が確実なため。
            else -> Unit
        }
        return outcome(0.0, if (onFoot) null else SegmentRejection.NOT_ON_FOOT)
    }

    private fun stopCountingAsHuman() {
        onFoot = false
        // 乗り物区間をまたいで距離を測らないよう、起点を捨てる。
        accumulator.reset()
        resetCalibrationWindow()
    }

    private fun onLocation(sample: LocationSample): Outcome {
        intervalPlanner.onLocation(sample)

        if (fallbackController.onLocation(sample) != null) {
            onModeChanged()
        }
        if (!onFoot) {
            return outcome(0.0, SegmentRejection.NOT_ON_FOOT)
        }
        if (fallbackController.mode == MeasurementMode.STEPS) {
            return outcome(0.0, SegmentRejection.STEP_FALLBACK_ACTIVE)
        }

        val newSteps = stepsSinceLocation
        stepsSinceLocation = 0L

        val update = accumulator.onLocation(sample, newSteps)
        accumulateCalibration(update.addedMeters, newSteps)

        return Outcome(
            confirmedMeters = update.confirmedMeters,
            pendingMeters = update.pendingMeters,
            retractedMeters = update.retractedMeters,
            rejection = update.rejection,
            mode = fallbackController.mode,
            movementState = accumulator.movementState,
            strideLengthCm = strideCalibrator.strideLengthCm,
            lastRawSteps = stepTracker.lastRawSteps,
            intervalMillis = intervalPlanner.intervalMillis,
        )
    }

    private fun onSteps(sample: StepSample): Outcome {
        val delta = stepTracker.onSample(sample)
        if (delta <= 0L) return outcome(0.0, rejection = null)

        if (fallbackController.mode == MeasurementMode.GPS) {
            // GPSモードでは歩数を距離にしない。停止判定と歩幅較正の材料として貯めるだけ。
            stepsSinceLocation += delta
            return outcome(0.0, rejection = null)
        }
        if (!onFoot) return outcome(0.0, SegmentRejection.NOT_ON_FOOT)

        val meters = strideCalibrator.distanceMetersFor(delta)
        return outcome(accumulator.onFallbackDistance(meters).confirmedMeters, rejection = null)
    }

    private fun onTick(nowMillis: Long): Outcome {
        if (fallbackController.onElapsed(nowMillis) != null) {
            onModeChanged()
        }
        return outcome(0.0, rejection = null)
    }

    /**
     * モードが変わったら、両方の起点を捨てる(P2-16)。
     * GPS側のアンカーを残すと切り替えをまたいだ直線距離が、歩数側の残りを残すと
     * 同じ区間の歩数が、それぞれ二重に効いてしまう。
     */
    private fun onModeChanged() {
        accumulator.reset()
        stepsSinceLocation = 0L
        resetCalibrationWindow()
    }

    /**
     * GPSで測れた距離と、その間の歩数を貯めて歩幅を較正する(P2-14)。
     * 1区間ぶんの歩数では誤差が大きいため、一定歩数まとまってから平均へ入れる。
     */
    private fun accumulateCalibration(addedMeters: Double, newSteps: Long) {
        if (addedMeters <= 0.0 || newSteps <= 0L) return
        calibrationMeters += addedMeters
        calibrationSteps += newSteps
        if (calibrationSteps >= config.minStepsForCalibration) {
            strideCalibrator.observe(calibrationMeters, calibrationSteps)
            resetCalibrationWindow()
        }
    }

    private fun resetCalibrationWindow() {
        calibrationMeters = 0.0
        calibrationSteps = 0L
    }

    private fun outcome(confirmedMeters: Double, rejection: SegmentRejection?) = Outcome(
        confirmedMeters = confirmedMeters,
        rejection = rejection,
        mode = fallbackController.mode,
        movementState = accumulator.movementState,
        strideLengthCm = strideCalibrator.strideLengthCm,
        lastRawSteps = stepTracker.lastRawSteps,
        intervalMillis = intervalPlanner.intervalMillis,
    )
}
