package io.github.task320.earthstep.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.common.AppBuildInfo
import io.github.task320.earthstep.core.data.measurement.MeasurementEngine
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.usecase.ObserveProgressSummaryUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    appBuildInfo: AppBuildInfo,
    observeProgressSummary: ObserveProgressSummaryUseCase,
    measurementEngine: MeasurementEngine,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val permissionState = MutableStateFlow(permissionChecker.currentState())

    val uiState: StateFlow<HomeUiState> = combine(
        observeProgressSummary(),
        measurementEngine.status,
        permissionState,
    ) { progress, status, permissions ->
        HomeUiState(
            progress = progress,
            measuring = status.running,
            permissionState = permissions,
            versionName = appBuildInfo.versionName,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState.initial(appBuildInfo.versionName),
    )

    /** 設定画面から戻ってきたときなど、権限の状態を読み直す(P3-9)。 */
    fun refreshPermissions() {
        permissionState.value = permissionChecker.currentState()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
