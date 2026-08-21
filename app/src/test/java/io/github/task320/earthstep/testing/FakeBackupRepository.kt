package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.backup.BackupData
import io.github.task320.earthstep.core.domain.backup.BackupLifetimeStats
import io.github.task320.earthstep.core.domain.repository.BackupRepository

/** メモリ上だけで完結する [BackupRepository]。 */
class FakeBackupRepository(initial: BackupData = emptySnapshot()) : BackupRepository {

    private var data = initial

    override suspend fun snapshot(): BackupData = data

    override suspend fun replaceAll(data: BackupData) {
        this.data = data
    }

    companion object {
        fun emptySnapshot(totalDistanceMeters: Long = 0L): BackupData = BackupData(
            exportedAt = "2026-01-01T00:00:00Z",
            lifetimeStats = BackupLifetimeStats(
                totalDistanceMeters = totalDistanceMeters,
                currentLap = 0,
                strideLengthCm = 70.0,
            ),
        )
    }
}
