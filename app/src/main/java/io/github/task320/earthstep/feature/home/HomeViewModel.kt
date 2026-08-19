package io.github.task320.earthstep.feature.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.common.AppBuildInfo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class HomeViewModel @Inject constructor(appBuildInfo: AppBuildInfo) : ViewModel() {

    // P1-7 で ProgressRepository の Flow を stateIn したものに差し替える。
    val uiState: StateFlow<HomeUiState> = MutableStateFlow(
        HomeUiState(
            totalDistanceMeters = 0L,
            versionName = appBuildInfo.versionName,
        ),
    ).asStateFlow()
}
