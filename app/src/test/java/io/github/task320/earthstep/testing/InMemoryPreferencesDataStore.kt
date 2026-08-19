package io.github.task320.earthstep.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * メモリ上だけで完結する [DataStore]。
 *
 * 実ファイルの DataStore はテンポラリファイルの置き換えにOSのリネームを使うため、
 * Windows 上の単体テストで失敗しうる。設定の読み書きロジックの検証には実ファイルは要らないので、
 * ここでは同じ契約(直列化された更新と、更新後の値を流す `data`)だけを再現する。
 */
class InMemoryPreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {

    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = mutex.withLock {
        val updated = transform(state.value)
        state.value = updated
        updated
    }
}
