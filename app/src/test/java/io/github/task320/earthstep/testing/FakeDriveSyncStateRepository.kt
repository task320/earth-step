package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.repository.DriveSyncStateRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** メモリ上だけで完結する [DriveSyncStateRepository]。 */
class FakeDriveSyncStateRepository(lastSyncedAt: Instant? = null) : DriveSyncStateRepository {

    private val state = MutableStateFlow(lastSyncedAt)

    override val lastSyncedAt: Flow<Instant?> = state

    override suspend fun recordSyncSuccess(at: Instant) {
        state.value = at
    }
}
