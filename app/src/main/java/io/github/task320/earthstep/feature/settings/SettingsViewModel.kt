package io.github.task320.earthstep.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import io.github.task320.earthstep.core.domain.usecase.ResetProgressUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val measurementEnabled: Boolean = true,
    val strideLengthCm: Double = 0.0,
    val totalDistanceMeters: Long = 0L,
    val permissionState: PermissionState = PermissionState(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val resetProgress: ResetProgressUseCase,
    private val permissionChecker: PermissionChecker,
    progressRepository: ProgressRepository,
) : ViewModel() {

    private val permissionState = MutableStateFlow(permissionChecker.currentState())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.measurementEnabled,
        progressRepository.lifetimeStats,
        permissionState,
    ) { enabled, stats, permissions ->
        SettingsUiState(
            measurementEnabled = enabled,
            strideLengthCm = stats.strideLengthCm,
            totalDistanceMeters = stats.totalDistanceMeters,
            permissionState = permissions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    fun refreshPermissions() {
        permissionState.value = permissionChecker.currentState()
    }

    fun setMeasurementEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMeasurementEnabled(enabled) }
    }

    fun reset() {
        viewModelScope.launch { resetProgress() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
