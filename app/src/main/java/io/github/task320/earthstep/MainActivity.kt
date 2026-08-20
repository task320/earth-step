package io.github.task320.earthstep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.navigation.EarthStepNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EarthStepTheme {
                val mainViewModel: MainViewModel = viewModel()
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                when (val state = uiState) {
                    // 起動直後は開始画面が決まるまで何も描かない。
                    // 一瞬ホームを出してからオンボーディングへ飛ばすと、ちらつきになる。
                    MainUiState.Loading -> Unit
                    is MainUiState.Ready -> EarthStepNavHost(
                        startDestination = state.startDestination,
                        onOnboardingFinished = mainViewModel::startMeasurementIfEnabled,
                    )
                }
            }
        }
    }
}
