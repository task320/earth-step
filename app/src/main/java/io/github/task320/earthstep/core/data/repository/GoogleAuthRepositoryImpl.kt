package io.github.task320.earthstep.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.task320.earthstep.core.domain.model.GoogleAccount
import io.github.task320.earthstep.core.domain.repository.GoogleAuthRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class GoogleAuthRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) :
    GoogleAuthRepository {

    private val preferences: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            // 読み込み失敗(ファイル破損など)で画面を落とさない。未サインイン扱いへフォールバックする。
            if (throwable is IOException) {
                Timber.w(throwable, "google auth preferences read failed")
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    override val signedInAccount: Flow<GoogleAccount?> = preferences
        .map { prefs ->
            val id = prefs[Keys.ID] ?: return@map null
            GoogleAccount(id = id, email = prefs[Keys.EMAIL], displayName = prefs[Keys.DISPLAY_NAME])
        }
        .distinctUntilChanged()

    override suspend fun setSignedInAccount(account: GoogleAccount?) {
        dataStore.edit { prefs ->
            prefs.remove(Keys.ID)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.DISPLAY_NAME)
            if (account != null) {
                prefs[Keys.ID] = account.id
                account.email?.let { prefs[Keys.EMAIL] = it }
                account.displayName?.let { prefs[Keys.DISPLAY_NAME] = it }
            }
        }
    }

    private object Keys {
        val ID = stringPreferencesKey("google_account_id")
        val EMAIL = stringPreferencesKey("google_account_email")
        val DISPLAY_NAME = stringPreferencesKey("google_account_display_name")
    }
}
