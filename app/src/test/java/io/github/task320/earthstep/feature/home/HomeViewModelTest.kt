package io.github.task320.earthstep.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.data.measurement.MeasurementEngine
import io.github.task320.earthstep.core.domain.measurement.MeasurementConfig
import io.github.task320.earthstep.core.domain.model.LifetimeStats
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.testing.FakeActivityRecognitionDataSource
import io.github.task320.earthstep.testing.FakeAppBuildInfo
import io.github.task320.earthstep.testing.FakeLocationDataSource
import io.github.task320.earthstep.testing.FakeMeasurementStateRepository
import io.github.task320.earthstep.testing.FakePermissionChecker
import io.github.task320.earthstep.testing.FakeProgressRepository
import io.github.task320.earthstep.testing.FakeStepDataSource
import io.github.task320.earthstep.testing.FakeTimeSource
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
    private val permissionChecker = FakePermissionChecker()
    private val required = PermissionRequirements.requiredOn(sdkInt = 33)

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
            assertThat(state.measuring).isFalse()
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

    @Test
    fun `権限の状態が状態に載る`() {
        // P3-9: 位置情報だけ許可された状態。
        permissionChecker.state = PermissionState(
            granted = setOf(AppPermission.FINE_LOCATION),
            required = required,
        )
        runTest {
            val viewModel = viewModel()

            viewModel.uiState.test {
                val state = expectMostRecentItem()
                assertThat(state.permissionState.canMeasure).isTrue()
                assertThat(state.permissionState.canMeasureInBackground).isFalse()
                assertThat(HomeWarning.from(state.permissionState))
                    .contains(HomeWarning.MISSING_BACKGROUND_LOCATION)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `refreshPermissionsで許可状態を読み直す`() = runTest {
        permissionChecker.state = PermissionState(required = required)
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(1)

            permissionChecker.state = PermissionState(granted = required, required = required)
            viewModel.refreshPermissions()

            assertThat(expectMostRecentItem().permissionState.canMeasureInBackground).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(stats: LifetimeStats? = null): HomeViewModel {
        val repository = stats?.let { FakeProgressRepository(it) } ?: progressRepository
        return HomeViewModel(
            appBuildInfo = FakeAppBuildInfo(versionName = "1.2.3"),
            progressRepository = repository,
            measurementEngine = engine(repository),
            permissionChecker = permissionChecker,
        )
    }

    /** 状態(`status`)を読むためだけに使う。`run` は呼ばないので購読は始まらない。 */
    private fun engine(repository: FakeProgressRepository) = MeasurementEngine(
        locationDataSource = FakeLocationDataSource(),
        stepDataSource = FakeStepDataSource(),
        activityRecognitionDataSource = FakeActivityRecognitionDataSource(),
        progressRepository = repository,
        measurementStateRepository = FakeMeasurementStateRepository(),
        timeSource = FakeTimeSource(),
        config = MeasurementConfig(),
    )
}
