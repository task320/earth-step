package io.github.task320.earthstep.core.domain.usecase

import io.github.task320.earthstep.core.domain.repository.CelebrationQueueRepository
import io.github.task320.earthstep.core.domain.repository.MeasurementStateRepository
import io.github.task320.earthstep.core.domain.repository.MilestoneRepository
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 進捗をすべて消す(P5-14)。
 *
 * 消す対象は距離・達成記録・周回・未再生の演出・歩数の基準値。
 * 歩数の基準値まで消すのは、距離を0に戻したあと最初の歩数サンプルを
 * 「再起動があった」と誤判定させないため(P2-13)。
 *
 * オンボーディングの完了状態は残す。権限の説明をもう一度見せる意味がない。
 */
@Singleton
class ResetProgressUseCase @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val milestoneRepository: MilestoneRepository,
    private val celebrationQueueRepository: CelebrationQueueRepository,
    private val measurementStateRepository: MeasurementStateRepository,
) {

    suspend operator fun invoke() {
        progressRepository.resetAll()
        milestoneRepository.resetAll()
        celebrationQueueRepository.clear()
        measurementStateRepository.clearLastRawSteps()
    }
}
