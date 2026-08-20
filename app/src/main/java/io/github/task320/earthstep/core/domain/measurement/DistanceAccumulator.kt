package io.github.task320.earthstep.core.domain.measurement

import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.MovementState
import io.github.task320.earthstep.core.domain.measurement.model.SegmentRejection

/**
 * 位置サンプル列から距離を積み上げる計算層(P2-3〜P2-9)。
 *
 * Android に依存しないため、擬似ログを流すだけで挙動を検証できる。
 * 位置・センサーのAPIを触る処理は [io.github.task320.earthstep.core.domain.measurement.MeasurementEngine]
 * より外側に置き、このクラスへは値だけを渡す。
 *
 * ## 起点(アンカー)の扱い
 * 区間を棄却したときはアンカーを進めない。こうすると次の正常な点は「最後に信頼できた点」から
 * 測られるため、仕様1.5の3点整合性チェック(A→B→C で B が外れ値なら A→C で計算)が
 * 特別扱い無しで成立する。ただしアンカーが古くなりすぎた場合は
 * [MeasurementConfig.maxSegmentGapMillis] で切り、その区間は歩数フォールバックへ委ねる。
 *
 * ## 距離の確定
 * 加算した距離はすぐには確定させず [RetroactiveDistanceBuffer] に寝かせる。
 * 停止が確定したら窓の中の距離を捨てる(仕様1.4の遡及除外)。
 * これにより、永続化した距離を後から引く処理が不要になる。
 */
class DistanceAccumulator(private val config: MeasurementConfig = MeasurementConfig()) {

    /**
     * 1サンプルぶんの処理結果。
     *
     * @param addedMeters このサンプルで新たに積んだ区間距離(まだ確定していない)。
     * @param confirmedMeters 遡及除外の窓から出て確定した距離。永続化してよい。
     * @param pendingMeters まだ取り消されうる距離の残高。
     * @param retractedMeters このサンプルで取り消した距離(停止確定時)。
     * @param rejection 区間を加算しなかった理由。加算した場合は null。
     */
    data class Update(
        val addedMeters: Double = 0.0,
        val confirmedMeters: Double = 0.0,
        val pendingMeters: Double = 0.0,
        val retractedMeters: Double = 0.0,
        val rejection: SegmentRejection? = null,
        val state: MovementState = MovementState.MOVING,
    )

    private val buffer = RetroactiveDistanceBuffer(config.retroactiveWindowMillis)
    private val stopDetector = StopDetector(config)

    private var anchor: LocationSample? = null
    private var lastTimestampMillis: Long? = null

    val movementState: MovementState get() = stopDetector.state

    /**
     * 位置サンプルを1件処理する。
     *
     * @param newSteps 前回のサンプルからの歩数の増分。歩数センサーが無い端末では 0 でよいが、
     *   その場合は停止判定が速度のみに頼ることになる(仕様1.4)。
     */
    fun onLocation(sample: LocationSample, newSteps: Long = 0L): Update {
        val previousTimestamp = lastTimestampMillis
        if (previousTimestamp != null && sample.timestampMillis <= previousTimestamp) {
            return Update(
                pendingMeters = buffer.pendingMeters,
                rejection = SegmentRejection.NON_MONOTONIC_TIME,
                state = stopDetector.state,
            )
        }
        lastTimestampMillis = sample.timestampMillis

        val confirmed = buffer.confirmExpired(sample.timestampMillis)

        if (sample.isMock) {
            // モック区間は前後ともXP対象外にするため、起点ごと捨てる(仕様1.5 / P2-6)。
            anchor = null
            return result(confirmed, SegmentRejection.MOCK_PROVIDER)
        }
        if (sample.accuracyMeters > config.maxAccuracyMeters) {
            // 起点は進めない。次の正常な点が最後に信頼できた点から測られる(3点整合性チェック)。
            return result(confirmed, SegmentRejection.LOW_ACCURACY)
        }

        val previousAnchor = anchor
        val segment = previousAnchor?.let { Segment.between(it, sample) }
        val stateChange = stopDetector.update(
            timestampMillis = sample.timestampMillis,
            speedMps = observedSpeedMps(sample, segment),
            newSteps = newSteps,
        )

        if (stateChange == MovementState.STOPPED) {
            val retracted = buffer.retractAll()
            anchor = sample
            return Update(
                confirmedMeters = confirmed,
                pendingMeters = buffer.pendingMeters,
                retractedMeters = retracted,
                rejection = SegmentRejection.STOPPED,
                state = stopDetector.state,
            )
        }
        if (stopDetector.state == MovementState.STOPPED) {
            // 停止中も起点だけは進めておく。再開時に滞留中のドリフトを拾わないため。
            anchor = sample
            return result(confirmed, SegmentRejection.STOPPED)
        }

        if (segment == null) {
            anchor = sample
            return result(confirmed, SegmentRejection.NO_ANCHOR)
        }
        if (segment.elapsedMillis > config.maxSegmentGapMillis) {
            anchor = sample
            return result(confirmed, SegmentRejection.GAP_TOO_LONG)
        }
        if (segment.speedMps > config.maxSegmentSpeedMps) {
            // 起点を進めないので、次の点が正常なら A→C として拾い直される(仕様1.5)。
            return result(confirmed, SegmentRejection.SPEED_TOO_HIGH)
        }

        buffer.add(sample.timestampMillis, segment.meters)
        anchor = sample
        return result(confirmed, rejection = null).copy(addedMeters = segment.meters)
    }

    /**
     * 歩数フォールバックで算出した距離を積む(P2-15/P2-16)。
     * GPS区間ではないため遡及除外の対象にせず、その場で確定させる
     * (仕様1.4のとおり、歩数ベースの距離は歩いた分しか増えない)。
     */
    fun onFallbackDistance(meters: Double): Update {
        if (meters <= 0.0) return result(0.0, rejection = null)
        return result(meters, rejection = null).copy(addedMeters = meters)
    }

    /** 計測を止めるときに、寝かせている距離を確定させる。 */
    fun flush(): Update = Update(
        confirmedMeters = buffer.flush(),
        state = stopDetector.state,
    )

    /** サービス再生成などで状態を捨てる。寝かせていた距離は失われる。 */
    fun reset() {
        buffer.retractAll()
        stopDetector.reset()
        anchor = null
        lastTimestampMillis = null
    }

    private fun result(confirmedMeters: Double, rejection: SegmentRejection?) = Update(
        confirmedMeters = confirmedMeters,
        pendingMeters = buffer.pendingMeters,
        rejection = rejection,
        state = stopDetector.state,
    )

    /**
     * 停止判定に使う速度。
     * 端末が速度を報告する場合はそれを優先し、無ければ直前の点からの区間速度で代用する。
     */
    private fun observedSpeedMps(sample: LocationSample, segment: Segment?): Double =
        sample.speedMetersPerSecond?.toDouble() ?: segment?.speedMps ?: 0.0

    private data class Segment(val meters: Double, val elapsedMillis: Long) {

        val speedMps: Double get() = meters / (elapsedMillis / MILLIS_PER_SECOND)

        companion object {
            private const val MILLIS_PER_SECOND = 1_000.0

            fun between(from: LocationSample, to: LocationSample): Segment = Segment(
                meters = Geo.distanceMeters(from, to),
                elapsedMillis = to.timestampMillis - from.timestampMillis,
            )
        }
    }
}
