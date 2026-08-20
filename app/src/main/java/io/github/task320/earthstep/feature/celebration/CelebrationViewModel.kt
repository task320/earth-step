package io.github.task320.earthstep.feature.celebration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.progress.ProgressSummary
import io.github.task320.earthstep.core.domain.repository.CelebrationQueueRepository
import io.github.task320.earthstep.core.domain.usecase.ObserveProgressSummaryUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 演出の再生状態(P5-13)。
 *
 * @param current いま再生する演出。無ければ null。
 * @param remaining これから再生する残り件数。
 */
data class CelebrationUiState(
    val current: PendingCelebration? = null,
    val remaining: Int = 0,
    val progress: ProgressSummary = ProgressSummary(),
)

@HiltViewModel
class CelebrationViewModel @Inject constructor(
    private val celebrationQueueRepository: CelebrationQueueRepository,
    observeProgressSummary: ObserveProgressSummaryUseCase,
) : ViewModel() {

    val uiState: StateFlow<CelebrationUiState> = combine(
        celebrationQueueRepository.pending,
        observeProgressSummary(),
    ) { pending, progress ->
        CelebrationUiState(
            current = pending.firstOrNull(),
            remaining = (pending.size - 1).coerceAtLeast(0),
            progress = progress,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CelebrationUiState(),
    )

    /** 再生し終えた1件を捨てて次へ進む。 */
    fun dismissCurrent() {
        viewModelScope.launch { celebrationQueueRepository.dequeue() }
    }

    /** まとめて閉じる。貯まりすぎたときに1件ずつ送らせない。 */
    fun skipAll() {
        viewModelScope.launch { celebrationQueueRepository.clear() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
