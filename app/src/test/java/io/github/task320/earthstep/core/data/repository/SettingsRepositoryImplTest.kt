package io.github.task320.earthstep.core.data.repository

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/** P1-7: DataStore に保存する設定の既定値と読み書き。 */
class SettingsRepositoryImplTest {

    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())
    }

    @Test
    fun `既定ではオンボーディング未完了で計測は有効`() = runBlocking<Unit> {
        assertThat(repository.onboardingCompleted.first()).isFalse()
        assertThat(repository.measurementEnabled.first()).isTrue()
        assertThat(repository.batteryOptimizationPromptShown.first()).isFalse()
    }

    @Test
    fun `書き込んだ値が読み出せる`() = runBlocking<Unit> {
        repository.setOnboardingCompleted(true)
        repository.setMeasurementEnabled(false)
        repository.setBatteryOptimizationPromptShown(true)

        assertThat(repository.onboardingCompleted.first()).isTrue()
        assertThat(repository.measurementEnabled.first()).isFalse()
        assertThat(repository.batteryOptimizationPromptShown.first()).isTrue()
    }

    @Test
    fun `設定の変更はFlowへ流れる`() = runBlocking<Unit> {
        assertThat(repository.measurementEnabled.first()).isTrue()
        repository.setMeasurementEnabled(false)
        assertThat(repository.measurementEnabled.first()).isFalse()
    }
}
