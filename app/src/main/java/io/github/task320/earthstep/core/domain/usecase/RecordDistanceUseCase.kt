package io.github.task320.earthstep.core.domain.usecase

import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.progress.Earth
import io.github.task320.earthstep.core.domain.progress.LapCalculator
import io.github.task320.earthstep.core.domain.progress.ProgressEvent
import io.github.task320.earthstep.core.domain.progress.ProgressEventSink
import io.github.task320.earthstep.core.domain.repository.MilestoneRepository
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 確定した距離を記録し、それによって起きた出来事を返す(P4-2 / P4-3 / P4-5 / P4-6)。
 *
 * 距離が増える経路をこの1本にまとめてある。計測エンジンが
 * [ProgressRepository.addDistance] を直接呼ぶと、達成判定を通らない距離が生まれてしまう。
 *
 * ## 取りこぼしと二重記録をどう防ぐか
 * - **マイルストーン**: 「前回の累計から今回までの範囲」ではなく「累計距離に届いていて、
 *   まだ記録が無いもの」を対象にする。プロセスが落ちて1回ぶんの判定を飛ばしても、
 *   次の更新でまとめて拾える。記録自体もDBの主キーで冪等(P4-3)。
 * - **周回**: 周回数は累計距離から一意に決まるので、毎回計算し直して上書きする。
 *   カウンタを+1する持ち方だと、取りこぼしや二重処理でずれたまま戻らなくなる。
 * - **周内マーカー**: 演出のためだけの通知で永続化しない。こちらは範囲判定で出す。
 *
 * 起きた出来事は [ProgressEventSink] へ渡す。通知を出すのも演出を貯めるのも
 * 「距離を記録した結果」なので、呼び出し側に回すと渡し忘れる余地が生まれる。
 */
@Singleton
class RecordDistanceUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val milestoneRepository: MilestoneRepository,
    private val progressEventSink: ProgressEventSink,
) {

    /**
     * @param meters 加算する距離(m)。0以下なら何もしない。
     * @param at 距離が確定した時刻。日別ログの日付キーと達成日時に使う。
     * @return 起きた出来事を距離の昇順で返す。演出と通知はこの順に再生する。
     */
    suspend operator fun invoke(meters: Long, at: Instant): List<ProgressEvent> {
        if (meters <= 0L) return emptyList()

        val totalMeters = progressRepository.addDistance(meters, at)
        val previousMeters = totalMeters - meters

        val events = buildList {
            addAll(recordNewAchievements(totalMeters, at))
            addAll(recordCompletedLaps(previousMeters, totalMeters, at))
            addAll(markersBetween(previousMeters, totalMeters))
        }
        val ordered = events.sortedBy { it.totalDistanceMeters }
        if (ordered.isNotEmpty()) {
            progressEventSink.onEvents(ordered)
        }
        return ordered
    }

    /** 累計距離に届いていて、まだ記録が無いマイルストーンを記録する。 */
    private suspend fun recordNewAchievements(totalMeters: Long, at: Instant): List<ProgressEvent.MilestoneAchieved> {
        val achieved = milestoneRepository.achievedIndexes.first()
        val due = MilestoneCatalog.milestones.filter {
            it.distanceMeters <= totalMeters && it.index !in achieved
        }
        return due.mapNotNull { milestone ->
            val recorded = milestoneRepository.recordAchievement(
                milestoneIndex = milestone.index,
                achievedAt = at,
                distanceMeters = totalMeters,
            )
            // 記録済みだった場合は演出も通知も出さない(P4-3)。
            if (recorded) {
                ProgressEvent.MilestoneAchieved(milestone, milestone.distanceMeters)
            } else {
                null
            }
        }
    }

    /** 走破した周を記録し、現在の周回数を累計距離から引き直す(P4-5)。 */
    private suspend fun recordCompletedLaps(
        previousMeters: Long,
        totalMeters: Long,
        at: Instant,
    ): List<ProgressEvent.LapCompleted> {
        val completed = LapCalculator.lapsCompletedBetween(previousMeters, totalMeters)
        if (completed.isEmpty()) return emptyList()

        completed.forEach { progressRepository.recordLapCompleted(it, at) }
        progressRepository.setCurrentLap(LapCalculator.progressOf(totalMeters).lapNumber)

        return completed.map { lapNumber ->
            ProgressEvent.LapCompleted(
                lapNumber = lapNumber,
                totalDistanceMeters = lapNumber * Earth.CIRCUMFERENCE_METERS,
            )
        }
    }

    private fun markersBetween(previousMeters: Long, totalMeters: Long): List<ProgressEvent.LapMarkerReached> =
        LapCalculator.markersReachedBetween(previousMeters, totalMeters).map { reached ->
            ProgressEvent.LapMarkerReached(
                marker = reached.marker,
                lapNumber = reached.lapNumber,
                totalDistanceMeters = reached.totalDistanceMeters,
            )
        }
}
