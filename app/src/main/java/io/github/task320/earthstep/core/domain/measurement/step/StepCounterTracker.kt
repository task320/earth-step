package io.github.task320.earthstep.core.domain.measurement.step

import io.github.task320.earthstep.core.domain.measurement.model.StepSample

/**
 * `TYPE_STEP_COUNTER` の累積値を「増分」へ変換する(仕様1.3 / P2-13)。
 *
 * このセンサーは端末の再起動でカウンタが0へ戻る。前回値より小さい値が来たら再起動とみなし、
 * ベースラインを補正する。補正しないと増分が巨大な負の値になり、距離が壊れる。
 *
 * 前回値はプロセスをまたいで保持する必要があるため、
 * 生成時に永続化しておいた値を [lastRawSteps] として渡す。
 */
class StepCounterTracker(lastRawSteps: Long? = null) {

    /** 最後に観測したセンサーの累積値。永続化の対象。 */
    var lastRawSteps: Long? = lastRawSteps
        private set

    /** 再起動を検知した回数。ログ・テスト用。 */
    var rebootCount: Int = 0
        private set

    /**
     * サンプルを1件処理し、前回からの歩数の増分を返す。
     *
     * - 初回は基準値が無いため増分0(この時点の値をベースラインにする)
     * - 再起動を検知した場合は、再起動後に歩いた分(=センサーの現在値)を増分とする
     */
    fun onSample(sample: StepSample): Long {
        val previous = lastRawSteps
        lastRawSteps = sample.cumulativeSteps

        return when {
            previous == null -> 0L
            sample.cumulativeSteps < previous -> {
                rebootCount++
                // 再起動後の値がそのまま「起動以降に歩いた歩数」。
                sample.cumulativeSteps
            }
            else -> sample.cumulativeSteps - previous
        }
    }

    /** 永続化した値から状態を復元する。 */
    fun restore(rawSteps: Long?) {
        lastRawSteps = rawSteps
    }
}
