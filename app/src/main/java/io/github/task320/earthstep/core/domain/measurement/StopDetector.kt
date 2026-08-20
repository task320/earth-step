package io.github.task320.earthstep.core.domain.measurement

import io.github.task320.earthstep.core.domain.measurement.model.MovementState

/**
 * 停止・再開の判定(仕様1.4 / P2-7・P2-9)。
 *
 * 速度と歩数の両方を見るのは、GPSの瞬間的なジッターだけで停止と誤判定しないため。
 * 停止も再開も「条件が [MeasurementConfig.stopHoldMillis] 継続したら確定」というヒステリシスを持つ。
 */
class StopDetector(private val config: MeasurementConfig) {

    var state: MovementState = MovementState.MOVING
        private set

    private var stopCandidateSinceMillis: Long? = null
    private var resumeCandidateSinceMillis: Long? = null

    /**
     * 1サンプルぶん状態を進める。
     *
     * @param speedMps その時点の速度(m/s)。
     * @param newSteps 前回のサンプルからの歩数の増分。
     * @return 状態が変わったら新しい状態、変わらなければ null。
     */
    fun update(timestampMillis: Long, speedMps: Double, newSteps: Long): MovementState? = when (state) {
        MovementState.MOVING -> updateWhileMoving(timestampMillis, speedMps, newSteps)
        MovementState.STOPPED -> updateWhileStopped(timestampMillis, speedMps, newSteps)
    }

    private fun updateWhileMoving(timestampMillis: Long, speedMps: Double, newSteps: Long): MovementState? {
        val looksStopped = speedMps < config.stopSpeedMps && newSteps <= 0L
        if (!looksStopped) {
            stopCandidateSinceMillis = null
            return null
        }
        val since = stopCandidateSinceMillis ?: timestampMillis.also { stopCandidateSinceMillis = it }
        if (timestampMillis - since < config.stopHoldMillis) return null

        state = MovementState.STOPPED
        stopCandidateSinceMillis = null
        resumeCandidateSinceMillis = null
        return MovementState.STOPPED
    }

    private fun updateWhileStopped(timestampMillis: Long, speedMps: Double, newSteps: Long): MovementState? {
        val looksMoving = speedMps >= config.stopSpeedMps && newSteps > 0L
        if (!looksMoving) {
            resumeCandidateSinceMillis = null
            return null
        }
        val since = resumeCandidateSinceMillis ?: timestampMillis.also { resumeCandidateSinceMillis = it }
        if (timestampMillis - since < config.resumeHoldMillis) return null

        state = MovementState.MOVING
        resumeCandidateSinceMillis = null
        stopCandidateSinceMillis = null
        return MovementState.MOVING
    }

    fun reset() {
        state = MovementState.MOVING
        stopCandidateSinceMillis = null
        resumeCandidateSinceMillis = null
    }
}
