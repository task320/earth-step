package io.github.task320.earthstep.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 計測エンジンがプロセスをまたいで引き継ぐ必要のある状態(P2-13)。
 *
 * 歩数センサーの累積値は、前回値と比べて初めて「増分」になる。プロセスが死んで前回値を失うと
 * 復帰直後の歩数を丸ごと取りこぼすか、再起動と誤判定して二重に数えることになるため、
 * 距離そのものと同じくらい確実に持ち越す必要がある。
 */
interface MeasurementStateRepository {

    /** 最後に観測した `TYPE_STEP_COUNTER` の累積値。未観測なら null。 */
    val lastRawSteps: Flow<Long?>

    suspend fun setLastRawSteps(steps: Long)

    /** 端末の再起動などで歩数の基準を捨てる。 */
    suspend fun clearLastRawSteps()
}
