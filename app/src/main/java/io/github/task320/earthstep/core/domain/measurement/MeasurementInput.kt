package io.github.task320.earthstep.core.domain.measurement

import io.github.task320.earthstep.core.domain.measurement.model.ActivityUpdate
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.StepSample

/**
 * 計測エンジンへの入力を1本のストリームへ束ねるための型。
 *
 * 位置・歩数・活動判定はそれぞれ別のAPIから非同期に届く。個別に状態を持つと
 * 到着順に依存したバグが出るため、単一の入力列として順に処理する。
 */
sealed interface MeasurementInput {

    val timestampMillis: Long

    data class Location(val sample: LocationSample) : MeasurementInput {
        override val timestampMillis: Long get() = sample.timestampMillis
    }

    data class Steps(val sample: StepSample) : MeasurementInput {
        override val timestampMillis: Long get() = sample.timestampMillis
    }

    data class Activity(val update: ActivityUpdate) : MeasurementInput {
        override val timestampMillis: Long get() = update.timestampMillis
    }

    /** 何も届かないまま時間が経ったことを知らせる。歩数フォールバックの判定に使う(P2-15)。 */
    data class Tick(override val timestampMillis: Long) : MeasurementInput
}
