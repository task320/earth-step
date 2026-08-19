package io.github.task320.earthstep.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.common.AppBuildInfo
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(appBuildInfo: AppBuildInfo, progressRepository: ProgressRepository) :
    ViewModel() {

    private val initialState = HomeUiState.initial(appBuildInfo.versionName)

    val uiState: StateFlow<HomeUiState> = combine(
        progressRepository.lifetimeStats,
        progressRepository.todayDistanceMeters,
    ) { stats, todayMeters ->
        HomeUiState(
            totalDistanceMeters = stats.totalDistanceMeters,
            todayDistanceMeters = todayMeters,
            currentLap = stats.currentLap,
            versionName = appBuildInfo.versionName,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = initialState,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
