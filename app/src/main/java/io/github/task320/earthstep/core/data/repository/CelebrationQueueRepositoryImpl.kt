package io.github.task320.earthstep.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.celebration.PendingCelebrationCodec
import io.github.task320.earthstep.core.domain.repository.CelebrationQueueRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class CelebrationQueueRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) :
    CelebrationQueueRepository {

    override val pending: Flow<List<PendingCelebration>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Timber.w(throwable, "celebration queue read failed")
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            PendingCelebrationCodec.decode(preferences[Keys.QUEUE].orEmpty())
        }
        .distinctUntilChanged()

    override suspend fun enqueue(celebrations: List<PendingCelebration>) {
        if (celebrations.isEmpty()) return
        dataStore.edit { preferences ->
            val current = PendingCelebrationCodec.decode(preferences[Keys.QUEUE].orEmpty())
            // 再生は起きた順に行う。同じ距離のものは追加された順を保つ。
            val merged = (current + celebrations).sortedBy { it.totalDistanceMeters }
            preferences[Keys.QUEUE] = PendingCelebrationCodec.encode(merged.take(MAX_QUEUE_SIZE))
        }
    }

    override suspend fun dequeue() {
        dataStore.edit { preferences ->
            val current = PendingCelebrationCodec.decode(preferences[Keys.QUEUE].orEmpty())
            preferences[Keys.QUEUE] = PendingCelebrationCodec.encode(current.drop(1))
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(Keys.QUEUE) }
    }

    private object Keys {
        val QUEUE = stringPreferencesKey("celebration_queue")
    }

    private companion object {
        /**
         * 貯める上限。長く起動しないまま100個ぶん貯まっても、
         * 起動時に延々と演出を見せられる方が困る。
         */
        const val MAX_QUEUE_SIZE = 20
    }
}
