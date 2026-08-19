package io.github.task320.earthstep.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.testing.FakeAppBuildInfo
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun `初期状態はバージョン名を保持し累計距離は0である`() = runTest {
        val viewModel = HomeViewModel(FakeAppBuildInfo(versionName = "1.2.3"))

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.versionName).isEqualTo("1.2.3")
            assertThat(state.totalDistanceMeters).isEqualTo(0L)
        }
    }
}
