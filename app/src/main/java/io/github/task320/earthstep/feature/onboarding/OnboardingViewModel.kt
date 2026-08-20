package io.github.task320.earthstep.feature.onboarding

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val steps: List<OnboardingStep> = emptyList(),
    val currentIndex: Int = 0,
    val permissionState: PermissionState = PermissionState(),
) {
    val currentStep: OnboardingStep? get() = steps.getOrNull(currentIndex)
    val isLastStep: Boolean get() = currentIndex >= steps.lastIndex
    val progressLabel: String get() = "${currentIndex + 1} / ${steps.size}"
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            steps = OnboardingSteps.forSdk(Build.VERSION.SDK_INT),
            permissionState = permissionChecker.currentState(),
        ),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** 設定画面から戻ったときなど、許可状態を読み直す。 */
    fun refreshPermissions() {
        _uiState.update { it.copy(permissionState = permissionChecker.currentState()) }
    }

    fun next() {
        refreshPermissions()
        _uiState.update {
            if (it.isLastStep) it else it.copy(currentIndex = it.currentIndex + 1)
        }
    }

    fun back() {
        _uiState.update {
            if (it.currentIndex == 0) it else it.copy(currentIndex = it.currentIndex - 1)
        }
    }

    /**
     * オンボーディングを終える。
     * バッテリー最適化の案内は一度出したら再提示しない(拒否しても計測は動くため)。
     */
    fun complete(onCompleted: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            settingsRepository.setBatteryOptimizationPromptShown(true)
            onCompleted()
        }
    }
}
