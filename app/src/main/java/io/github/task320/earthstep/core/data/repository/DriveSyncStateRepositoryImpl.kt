package io.github.task320.earthstep.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.task320.earthstep.core.domain.repository.DriveSyncStateRepository
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class DriveSyncStateRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) :
    DriveSyncStateRepository {

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            // 読み込み失敗(ファイル破損など)で画面を落とさない。「未同期」扱いへフォールバックする。
            if (throwable is IOException) {
                Timber.w(throwable, "drive sync state preferences read failed")
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    override val lastSyncedAt: Flow<Instant?> = preferences
        .map { prefs -> prefs[Keys.LAST_SYNCED_AT]?.let { runCatching { Instant.parse(it) }.getOrNull() } }
        .distinctUntilChanged()

    override suspend fun recordSyncSuccess(at: Instant) {
        dataStore.edit { it[Keys.LAST_SYNCED_AT] = at.toString() }
    }

    private object Keys {
        val LAST_SYNCED_AT = stringPreferencesKey("drive_last_synced_at")
    }
}
