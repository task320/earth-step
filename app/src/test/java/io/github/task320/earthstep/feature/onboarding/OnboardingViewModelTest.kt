package io.github.task320.earthstep.feature.onboarding

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.testing.FakePermissionChecker
import io.github.task320.earthstep.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** P3-8: オンボーディングの進行と完了。 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val settingsRepository = FakeSettingsRepository()
    private val permissionChecker = FakePermissionChecker()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `最初のページはルール説明`() {
        val viewModel = OnboardingViewModel(settingsRepository, permissionChecker)

        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.RULES)
        assertThat(viewModel.uiState.value.currentIndex).isEqualTo(0)
    }

    @Test
    fun `nextで次のページへ進み最後で止まる`() {
        val viewModel = OnboardingViewModel(settingsRepository, permissionChecker)
        val lastIndex = viewModel.uiState.value.steps.lastIndex

        repeat(lastIndex + 3) { viewModel.next() }

        assertThat(viewModel.uiState.value.currentIndex).isEqualTo(lastIndex)
        assertThat(viewModel.uiState.value.isLastStep).isTrue()
    }

    @Test
    fun `backで前のページへ戻り先頭で止まる`() {
        val viewModel = OnboardingViewModel(settingsRepository, permissionChecker)
        viewModel.next()

        viewModel.back()
        viewModel.back()

        assertThat(viewModel.uiState.value.currentIndex).isEqualTo(0)
    }

    @Test
    fun `ページ送りのたびに許可状態を読み直す`() {
        // 権限ダイアログの結果を反映するため、進むたびに読み直す。
        val viewModel = OnboardingViewModel(settingsRepository, permissionChecker)
        val before = permissionChecker.readCount

        viewModel.next()

        assertThat(permissionChecker.readCount).isGreaterThan(before)
    }

    @Test
    fun `refreshPermissionsで最新の許可状態が載る`() {
        val required = PermissionRequirements.requiredOn(sdkInt = 33)
        val viewModel = OnboardingViewModel(settingsRepository, permissionChecker)
        permissionChecker.state = PermissionState(
            granted = setOf(AppPermission.FINE_LOCATION),
            required = required,
        )

        viewModel.refreshPermissions()

        assertThat(viewModel.uiState.value.permissionState.canMeasure).isTrue()
    }

    @Test
    fun `完了で設定が書き込まれコールバックが呼ばれる`() = runTest {
        val viewModel = OnboardingViewModel(settingsRepository, permissionChecker)
        var finished = false

        viewModel.complete { finished = true }

        assertThat(finished).isTrue()
        assertThat(settingsRepository.isOnboardingCompleted()).isTrue()
        // バッテリー最適化の案内は一度出したら再提示しない。
        assertThat(settingsRepository.isBatteryOptimizationPromptShown()).isTrue()
    }

    @Test
    fun `進捗ラベルは現在位置と総数を示す`() {
        val viewModel = OnboardingViewModel(settingsRepository, permissionChecker)
        val total = viewModel.uiState.value.steps.size

        assertThat(viewModel.uiState.value.progressLabel).isEqualTo("1 / $total")
    }
}
