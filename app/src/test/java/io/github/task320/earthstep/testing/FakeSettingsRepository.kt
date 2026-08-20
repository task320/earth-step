package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** メモリ上だけで完結する [SettingsRepository]。 */
class FakeSettingsRepository(
    onboardingCompleted: Boolean = false,
    measurementEnabled: Boolean = true,
    batteryOptimizationPromptShown: Boolean = false,
) : SettingsRepository {

    private val onboardingState = MutableStateFlow(onboardingCompleted)
    private val measurementState = MutableStateFlow(measurementEnabled)
    private val batteryPromptState = MutableStateFlow(batteryOptimizationPromptShown)

    override val onboardingCompleted: Flow<Boolean> = onboardingState
    override val measurementEnabled: Flow<Boolean> = measurementState
    override val batteryOptimizationPromptShown: Flow<Boolean> = batteryPromptState

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingState.value = completed
    }

    override suspend fun setMeasurementEnabled(enabled: Boolean) {
        measurementState.value = enabled
    }

    override suspend fun setBatteryOptimizationPromptShown(shown: Boolean) {
        batteryPromptState.value = shown
    }

    fun isOnboardingCompleted(): Boolean = onboardingState.value

    fun isBatteryOptimizationPromptShown(): Boolean = batteryPromptState.value
}
