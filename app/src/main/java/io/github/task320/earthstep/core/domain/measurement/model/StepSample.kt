package io.github.task320.earthstep.core.domain.measurement.model

/**
 * 歩数センサー(`TYPE_STEP_COUNTER`)の1サンプル(P2-1)。
 *
 * @param cumulativeSteps 端末起動からの累積歩数。再起動で0へ戻る(仕様1.3 / P2-13)。
 * @param timestampMillis 取得時刻(epoch millis)。
 */
data class StepSample(val cumulativeSteps: Long, val timestampMillis: Long)
