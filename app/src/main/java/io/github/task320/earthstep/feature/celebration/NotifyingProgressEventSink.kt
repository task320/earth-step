package io.github.task320.earthstep.feature.celebration

import android.annotation.SuppressLint
import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.progress.ProgressEvent
import io.github.task320.earthstep.core.domain.progress.ProgressEventSink
import io.github.task320.earthstep.core.domain.repository.CelebrationQueueRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 起きた出来事を通知と演出キューへ振り分ける(P5-12 / P5-13)。
 *
 * 通知は「起きたことをその場で知らせる」もの、演出キューは
 * 「次にアプリを開いたときに見せる」もの。両方に流すのは、
 * 通知を見逃してもアプリを開けば必ず演出が出るようにするため。
 */
@Singleton
class NotifyingProgressEventSink @Inject constructor(
    private val celebrationQueueRepository: CelebrationQueueRepository,
    private val achievementNotifications: AchievementNotifications,
) : ProgressEventSink {

    // canNotify() で権限を確認してから通知する。
    @SuppressLint("MissingPermission")
    override suspend fun onEvents(events: List<ProgressEvent>) {
        if (events.isEmpty()) return
        celebrationQueueRepository.enqueue(events.map(PendingCelebration::from))
        achievementNotifications.notifyAchievements(events)
    }
}
