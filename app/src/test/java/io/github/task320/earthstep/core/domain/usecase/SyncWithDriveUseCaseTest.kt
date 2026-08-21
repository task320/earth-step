package io.github.task320.earthstep.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.backup.BackupCodec
import io.github.task320.earthstep.core.domain.backup.BackupDailyLog
import io.github.task320.earthstep.core.domain.backup.BackupData
import io.github.task320.earthstep.core.domain.backup.BackupLifetimeStats
import io.github.task320.earthstep.testing.FakeBackupRepository
import io.github.task320.earthstep.testing.FakeDriveSyncRepository
import io.github.task320.earthstep.testing.FakeDriveSyncStateRepository
import io.github.task320.earthstep.testing.FakeTimeSource
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** P7-7: ローカルとDriveの突き合わせ。中身はP7-3/P7-4(インポート/マージ)の合成でしかない。 */
class SyncWithDriveUseCaseTest {

    private val local = BackupData(
        exportedAt = "2026-01-01T00:00:00Z",
        lifetimeStats = BackupLifetimeStats(totalDistanceMeters = 1_000L, currentLap = 0, strideLengthCm = 70.0),
        dailyLog = listOf(BackupDailyLog(date = "2026-01-01", distanceMeters = 1_000L)),
    )

    private val driveSyncStateRepository = FakeDriveSyncStateRepository()
    private val timeSource = FakeTimeSource(current = Instant.parse("2026-08-22T04:00:00Z"))

    private fun useCase(driveSyncRepository: FakeDriveSyncRepository, backupRepository: FakeBackupRepository) =
        SyncWithDriveUseCase(
            driveSyncRepository = driveSyncRepository,
            driveSyncStateRepository = driveSyncStateRepository,
            exportBackup = ExportBackupUseCase(backupRepository),
            importBackup = ImportBackupUseCase(backupRepository),
            timeSource = timeSource,
        )

    @Test
    fun `Driveに何も無ければローカルの内容をそのままアップロードする`() = runBlocking<Unit> {
        val backupRepository = FakeBackupRepository(local)
        val driveSyncRepository = FakeDriveSyncRepository(remoteJson = null)

        val result = useCase(driveSyncRepository, backupRepository).invoke(accessToken = "token")

        assertThat(result).isEqualTo(SyncResult.Success(addedMeters = 0L))
        assertThat(driveSyncRepository.lastUploaded).isEqualTo(BackupCodec.encode(local))
        assertThat(driveSyncStateRepository.lastSyncedAt.first()).isEqualTo(timeSource.now())
    }

    @Test
    fun `Drive側に別の日の記録があればマージして増分を返す`() = runBlocking<Unit> {
        val backupRepository = FakeBackupRepository(local)
        val remote = local.copy(
            dailyLog = listOf(BackupDailyLog(date = "2026-01-02", distanceMeters = 500L)),
        )
        val driveSyncRepository = FakeDriveSyncRepository(remoteJson = BackupCodec.encode(remote))

        val result = useCase(driveSyncRepository, backupRepository).invoke(accessToken = "token")

        assertThat(result).isEqualTo(SyncResult.Success(addedMeters = 500L))
        // マージ後の状態がローカルにも反映され、Driveへも書き戻る。
        assertThat(backupRepository.snapshot().lifetimeStats.totalDistanceMeters).isEqualTo(1_500L)
        assertThat(driveSyncRepository.lastUploaded).isNotNull()
    }

    @Test
    fun `同じ日の記録は多い方だけが残り二重加算しない`() = runBlocking<Unit> {
        val backupRepository = FakeBackupRepository(local)
        val remote = local.copy(
            dailyLog = listOf(BackupDailyLog(date = "2026-01-01", distanceMeters = 800L)),
        )
        val driveSyncRepository = FakeDriveSyncRepository(remoteJson = BackupCodec.encode(remote))

        val result = useCase(driveSyncRepository, backupRepository).invoke(accessToken = "token")

        assertThat(result).isEqualTo(SyncResult.Success(addedMeters = 0L))
        assertThat(backupRepository.snapshot().lifetimeStats.totalDistanceMeters).isEqualTo(1_000L)
    }

    @Test
    fun `壊れたJSONを受け取ったら同期を中断する`() = runBlocking<Unit> {
        val backupRepository = FakeBackupRepository(local)
        val driveSyncRepository = FakeDriveSyncRepository(remoteJson = "not a json")

        val result = useCase(driveSyncRepository, backupRepository).invoke(accessToken = "token")

        assertThat(result).isEqualTo(SyncResult.Failed)
        // 壊れたデータで正常なバックアップを上書きしないよう、Driveへは書き込まない。
        assertThat(driveSyncRepository.lastUploaded).isNull()
    }

    @Test
    fun `アップロードに失敗したら伝える`() = runBlocking<Unit> {
        val backupRepository = FakeBackupRepository(local)
        val driveSyncRepository = FakeDriveSyncRepository(remoteJson = null, uploadSucceeds = false)

        val result = useCase(driveSyncRepository, backupRepository).invoke(accessToken = "token")

        assertThat(result).isEqualTo(SyncResult.Failed)
        assertThat(driveSyncStateRepository.lastSyncedAt.first()).isNull()
    }
}
