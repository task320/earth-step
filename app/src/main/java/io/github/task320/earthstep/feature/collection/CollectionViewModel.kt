package io.github.task320.earthstep.feature.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.domain.milestone.Milestone
import io.github.task320.earthstep.core.domain.repository.MilestoneRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 達成記録の1行(仕様3.3 / P5-6)。
 *
 * バッジ画像は持たず、番号・名称・距離・到達日のテキストだけで構成する。
 *
 * @param achievedAt 到達日時。未到達なら null。
 */
data class MilestoneRow(val milestone: Milestone, val achievedAt: Instant?) {
    val achieved: Boolean get() = achievedAt != null
}

data class CollectionUiState(val rows: List<MilestoneRow> = emptyList(), val achievedCount: Int = 0) {
    val total: Int get() = rows.size
}

@HiltViewModel
class CollectionViewModel @Inject constructor(milestoneRepository: MilestoneRepository) : ViewModel() {

    val uiState: StateFlow<CollectionUiState> = milestoneRepository.achievements
        .map { achievements ->
            val byIndex = achievements.associateBy { it.milestoneIndex }
            val rows = milestoneRepository.catalog.map { milestone ->
                MilestoneRow(
                    milestone = milestone,
                    achievedAt = byIndex[milestone.index]?.achievedAt,
                )
            }
            CollectionUiState(rows = rows, achievedCount = achievements.size)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CollectionUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
