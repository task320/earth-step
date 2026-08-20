package io.github.task320.earthstep.core.domain.measurement.source

import io.github.task320.earthstep.core.domain.measurement.model.ActivityTransitionEvent
import io.github.task320.earthstep.core.domain.measurement.model.ActivityUpdate
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.model.StepSample
import kotlinx.coroutines.flow.Flow

/**
 * 位置の供給元(P2-20)。
 *
 * 更新間隔は計測中に変わる(直線なら空け、カーブが多ければ詰める)ため、
 * 固定値ではなく Flow で受け取り、値が変わったら購読し直す。
 */
interface LocationDataSource {
    fun locations(intervalMillis: Flow<Long>): Flow<LocationSample>
}

/** 歩数センサーの供給元(P2-12)。非搭載端末では空の Flow を返す。 */
interface StepDataSource {
    val steps: Flow<StepSample>

    /** 端末が `TYPE_STEP_COUNTER` を持つか。 */
    val isAvailable: Boolean
}

/** 活動判定の供給元(P2-18 / P2-19)。 */
interface ActivityRecognitionDataSource {
    /** 一定間隔で届く活動判定。 */
    val activities: Flow<ActivityUpdate>

    /** `STILL` → `ON_FOOT` のような遷移。GPSの起動・停止の合図に使う。 */
    val transitions: Flow<ActivityTransitionEvent>
}
