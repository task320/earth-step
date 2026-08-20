package io.github.task320.earthstep.core.domain.repository

import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import kotlinx.coroutines.flow.Flow

/**
 * 未再生の演出を貯めておく(P5-13)。
 *
 * バックグラウンドで達成しても演出は出せないため、次の起動まで持ち越す。
 * 距離そのものと違い、失っても「演出を1回見逃す」だけなので DataStore に置く。
 */
interface CelebrationQueueRepository {

    /** 未再生の演出を、起きた順(累計距離の昇順)で流す。 */
    val pending: Flow<List<PendingCelebration>>

    suspend fun enqueue(celebrations: List<PendingCelebration>)

    /** 先頭の1件を再生済みにする。 */
    suspend fun dequeue()

    suspend fun clear()
}
