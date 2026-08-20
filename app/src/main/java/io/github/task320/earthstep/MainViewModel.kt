package io.github.task320.earthstep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import io.github.task320.earthstep.feature.measurement.MeasurementController
import io.github.task320.earthstep.navigation.EarthStepDestination
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/** 起動直後の状態。設定の読み込みが終わるまでは画面を出さない。 */
sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val startDestination: EarthStepDestination) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val measurementController: MeasurementController,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val onboarded = settingsRepository.onboardingCompleted.first()
            _uiState.value = MainUiState.Ready(
                startDestination = if (onboarded) {
                    EarthStepDestination.HOME
                } else {
                    EarthStepDestination.ONBOARDING
                },
            )
            if (onboarded) {
                startMeasurementIfEnabled()
            }
        }
    }

    /**
     * 計測を開始する。
     *
     * 画面が前面にある状態から開始するため、Android 15 のバックグラウンド起動制限には
     * 抵触しない(仕様7.1-5)。権限が足りない場合は開始せず、ホームの警告に任せる。
     */
    fun startMeasurementIfEnabled() {
        viewModelScope.launch {
            if (!settingsRepository.measurementEnabled.first()) return@launch
            val started = measurementController.start()
            Timber.i("measurement start requested: started=%b", started)
        }
    }
}
