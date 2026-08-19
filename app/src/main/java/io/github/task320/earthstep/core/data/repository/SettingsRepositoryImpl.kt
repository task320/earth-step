package io.github.task320.earthstep.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            // 読み込み失敗(ファイル破損など)で画面を落とさない。既定値へフォールバックする。
            if (throwable is IOException) {
                Timber.w(throwable, "settings preferences read failed")
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    override val onboardingCompleted: Flow<Boolean> = booleanFlow(Keys.ONBOARDING_COMPLETED, false)

    override val measurementEnabled: Flow<Boolean> = booleanFlow(Keys.MEASUREMENT_ENABLED, true)

    override val batteryOptimizationPromptShown: Flow<Boolean> =
        booleanFlow(Keys.BATTERY_OPTIMIZATION_PROMPT_SHOWN, false)

    override suspend fun setOnboardingCompleted(completed: Boolean) = putBoolean(Keys.ONBOARDING_COMPLETED, completed)

    override suspend fun setMeasurementEnabled(enabled: Boolean) = putBoolean(Keys.MEASUREMENT_ENABLED, enabled)

    override suspend fun setBatteryOptimizationPromptShown(shown: Boolean) =
        putBoolean(Keys.BATTERY_OPTIMIZATION_PROMPT_SHOWN, shown)

    private fun booleanFlow(key: Preferences.Key<Boolean>, default: Boolean): Flow<Boolean> =
        preferences.map { it[key] ?: default }.distinctUntilChanged()

    private suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MEASUREMENT_ENABLED = booleanPreferencesKey("measurement_enabled")
        val BATTERY_OPTIMIZATION_PROMPT_SHOWN = booleanPreferencesKey("battery_optimization_prompt_shown")
    }
}
