package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.celebration.PendingCelebration
import io.github.task320.earthstep.core.domain.repository.CelebrationQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** メモリ上だけで完結する [CelebrationQueueRepository]。 */
class FakeCelebrationQueueRepository : CelebrationQueueRepository {

    private val state = MutableStateFlow<List<PendingCelebration>>(emptyList())

    override val pending: Flow<List<PendingCelebration>> = state

    override suspend fun enqueue(celebrations: List<PendingCelebration>) {
        state.value = (state.value + celebrations).sortedBy { it.totalDistanceMeters }
    }

    override suspend fun dequeue() {
        state.value = state.value.drop(1)
    }

    override suspend fun clear() {
        state.value = emptyList()
    }

    fun current(): List<PendingCelebration> = state.value
}
