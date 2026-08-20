package io.github.task320.earthstep.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import io.github.task320.earthstep.core.domain.repository.MeasurementStateRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class MeasurementStateRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) :
    MeasurementStateRepository {

    override val lastRawSteps: Flow<Long?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Timber.w(throwable, "measurement state read failed")
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { it[Keys.LAST_RAW_STEPS] }
        .distinctUntilChanged()

    override suspend fun setLastRawSteps(steps: Long) {
        dataStore.edit { it[Keys.LAST_RAW_STEPS] = steps }
    }

    override suspend fun clearLastRawSteps() {
        dataStore.edit { it.remove(Keys.LAST_RAW_STEPS) }
    }

    private object Keys {
        val LAST_RAW_STEPS = longPreferencesKey("last_raw_steps")
    }
}
