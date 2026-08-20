package io.github.task320.earthstep.core.domain.usecase

import io.github.task320.earthstep.core.domain.progress.ProgressSummary
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 画面に出す進捗をまとめて流す(P4-4)。
 *
 * 周回数もマイルストーンの進捗も累計距離から導けるため、
 * DBに持っている `current_lap` ではなく累計距離から計算し直す。
 * こうしておくと、インポート(P7)で距離だけが入れ替わっても表示が食い違わない。
 */
@Singleton
class ObserveProgressSummaryUseCase @Inject constructor(private val progressRepository: ProgressRepository) {

    operator fun invoke(): Flow<ProgressSummary> = combine(
        progressRepository.lifetimeStats.map { it.totalDistanceMeters }.distinctUntilChanged(),
        progressRepository.todayDistanceMeters,
    ) { totalMeters, todayMeters ->
        ProgressSummary.of(totalDistanceMeters = totalMeters, todayDistanceMeters = todayMeters)
    }
}
