package io.github.task320.earthstep.core.domain.progress

/**
 * 距離の記録で起きた出来事の受け取り口(P5-12 / P5-13)。
 *
 * 計測エンジンは「何が起きたか」を知っているが、通知の出し方も演出の貯め方も知らない。
 * その先を差し替えられるように、受け取り口だけをドメイン側に置く。
 */
fun interface ProgressEventSink {

    suspend fun onEvents(events: List<ProgressEvent>)
}
