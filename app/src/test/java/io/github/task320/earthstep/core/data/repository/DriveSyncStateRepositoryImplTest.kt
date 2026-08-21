package io.github.task320.earthstep.core.data.repository

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.testing.InMemoryPreferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/** P7-9: 最終同期日時のDataStoreへの保存。 */
class DriveSyncStateRepositoryImplTest {

    private lateinit var repository: DriveSyncStateRepositoryImpl

    @Before
    fun setUp() {
        repository = DriveSyncStateRepositoryImpl(InMemoryPreferencesDataStore())
    }

    @Test
    fun `既定では未同期`() = runBlocking<Unit> {
        assertThat(repository.lastSyncedAt.first()).isNull()
    }

    @Test
    fun `記録した日時が読み出せる`() = runBlocking<Unit> {
        val at = Instant.parse("2026-08-22T04:00:00Z")

        repository.recordSyncSuccess(at)

        assertThat(repository.lastSyncedAt.first()).isEqualTo(at)
    }

    @Test
    fun `後から記録した日時で上書きされる`() = runBlocking<Unit> {
        repository.recordSyncSuccess(Instant.parse("2026-08-21T00:00:00Z"))
        val latest = Instant.parse("2026-08-22T04:00:00Z")

        repository.recordSyncSuccess(latest)

        assertThat(repository.lastSyncedAt.first()).isEqualTo(latest)
    }
}
