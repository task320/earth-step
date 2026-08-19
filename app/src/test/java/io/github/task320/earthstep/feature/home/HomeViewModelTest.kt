package io.github.task320.earthstep.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.model.LifetimeStats
import io.github.task320.earthstep.testing.FakeAppBuildInfo
import io.github.task320.earthstep.testing.FakeProgressRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val progressRepository = FakeProgressRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初期状態はバージョン名を保持し累計距離は0である`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.versionName).isEqualTo("1.2.3")
            assertThat(state.totalDistanceMeters).isEqualTo(0L)
            assertThat(state.todayDistanceMeters).isEqualTo(0L)
            assertThat(state.currentLap).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `距離が加算されると累計と当日距離が更新される`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(1)

            progressRepository.addDistance(1_500L, Instant.parse("2026-08-19T03:00:00Z"))

            val state = expectMostRecentItem()
            assertThat(state.totalDistanceMeters).isEqualTo(1_500L)
            assertThat(state.todayDistanceMeters).isEqualTo(1_500L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `周回数が状態に反映される`() = runTest {
        val viewModel = viewModel(LifetimeStats.INITIAL.copy(currentLap = 4))

        viewModel.uiState.test {
            assertThat(expectMostRecentItem().currentLap).isEqualTo(4)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(stats: LifetimeStats? = null): HomeViewModel {
        val repository = stats?.let { FakeProgressRepository(it) } ?: progressRepository
        return HomeViewModel(FakeAppBuildInfo(versionName = "1.2.3"), repository)
    }
}
