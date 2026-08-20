package io.github.task320.earthstep.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.data.local.EarthStepDatabase
import io.github.task320.earthstep.core.domain.backup.BackupCodec
import io.github.task320.earthstep.core.domain.backup.BackupParseResult
import io.github.task320.earthstep.core.domain.usecase.ExportBackupUseCase
import io.github.task320.earthstep.core.domain.usecase.ImportBackupUseCase
import io.github.task320.earthstep.core.domain.usecase.ImportResult
import io.github.task320.earthstep.core.domain.usecase.RecordDistanceUseCase
import io.github.task320.earthstep.testing.FakeTimeSource
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P7-2 〜 P7-5: 実際のDBを通したエクスポート/インポートの検証。
 *
 * 純粋なマージのテスト([io.github.task320.earthstep.core.domain.backup.BackupMergerTest])とは別に、
 * DBへの書き戻しまで含めて二重計上が起きないことを見る。
 */
@RunWith(RobolectricTestRunner::class)
class BackupRoundTripTest {

    private lateinit var database: EarthStepDatabase
    private lateinit var backupRepository: BackupRepositoryImpl
    private lateinit var progressRepository: ProgressRepositoryImpl
    private lateinit var milestoneRepository: MilestoneRepositoryImpl
    private lateinit var recordDistance: RecordDistanceUseCase
    private lateinit var exportBackup: ExportBackupUseCase
    private lateinit var importBackup: ImportBackupUseCase

    private val timeSource = FakeTimeSource(current = Instant.parse("2026-08-20T03:00:00Z"))

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EarthStepDatabase::class.java,
        )
            .setQueryExecutor(Dispatchers.IO.asExecutor())
            .setTransactionExecutor(Dispatchers.IO.asExecutor())
            .build()

        backupRepository = BackupRepositoryImpl(database, timeSource, Dispatchers.IO)
        progressRepository = ProgressRepositoryImpl(database, timeSource, Dispatchers.IO)
        milestoneRepository = MilestoneRepositoryImpl(database.milestoneAchievementDao(), Dispatchers.IO)
        recordDistance = RecordDistanceUseCase(progressRepository, milestoneRepository) { }
        exportBackup = ExportBackupUseCase(backupRepository)
        importBackup = ImportBackupUseCase(backupRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `歩いた記録を書き出して読み戻すと同じ状態になる`() = runBlocking<Unit> {
        recordDistance(5_000L, Instant.parse("2026-08-19T03:00:00Z"))
        recordDistance(3_000L, Instant.parse("2026-08-20T03:00:00Z"))

        val json = exportBackup()
        val result = importBackup(json)

        assertThat(result).isInstanceOf(ImportResult.Success::class.java)
        assertThat((result as ImportResult.Success).addedMeters).isEqualTo(0L)
        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(8_000L)
    }

    @Test
    fun `同じファイルを何度読み込んでも距離は増えない`() = runBlocking<Unit> {
        recordDistance(5_000L, Instant.parse("2026-08-19T03:00:00Z"))
        val json = exportBackup()

        repeat(3) { importBackup(json) }

        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(5_000L)
    }

    @Test
    fun `別端末のバックアップを取り込むと別の日の距離が足される`() = runBlocking<Unit> {
        // 旧端末で 8/18 に 4km 歩いた記録を模したJSONを作る。
        recordDistance(4_000L, Instant.parse("2026-08-18T03:00:00Z"))
        val oldPhoneJson = exportBackup()

        // 端末をまっさらに戻し、新端末で 8/20 に 2km 歩いた状態にする。
        progressRepository.resetAll()
        milestoneRepository.resetAll()
        recordDistance(2_000L, Instant.parse("2026-08-20T03:00:00Z"))

        val result = importBackup(oldPhoneJson) as ImportResult.Success

        assertThat(result.addedMeters).isEqualTo(4_000L)
        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(6_000L)
        assertThat(progressRepository.distanceMetersOn(java.time.LocalDate.of(2026, 8, 18)).first())
            .isEqualTo(4_000L)
    }

    @Test
    fun `達成記録は取り込みで和集合になる`() = runBlocking<Unit> {
        // 旧端末で #1〜#10 を達成(828m)。
        recordDistance(828L, Instant.parse("2026-08-18T03:00:00Z"))
        val oldPhoneJson = exportBackup()

        progressRepository.resetAll()
        milestoneRepository.resetAll()

        importBackup(oldPhoneJson)

        assertThat(milestoneRepository.achievedIndexes.first()).hasSize(10)
    }

    @Test
    fun `達成日時は取り込んでも保たれる`() = runBlocking<Unit> {
        val achievedAt = Instant.parse("2026-08-18T03:00:00Z")
        recordDistance(300L, achievedAt)
        val json = exportBackup()

        progressRepository.resetAll()
        milestoneRepository.resetAll()
        importBackup(json)

        val achievement = milestoneRepository.achievements.first().single()
        assertThat(achievement.milestoneIndex).isEqualTo(1)
        assertThat(achievement.achievedAt).isEqualTo(achievedAt)
    }

    @Test
    fun `書き出したJSONは仕様6_3の形式で読める`() = runBlocking<Unit> {
        recordDistance(1_500L, Instant.parse("2026-08-20T03:00:00Z"))

        val decoded = BackupCodec.decode(exportBackup())

        assertThat(decoded).isInstanceOf(BackupParseResult.Success::class.java)
        val data = (decoded as BackupParseResult.Success).data
        assertThat(data.schemaVersion).isEqualTo(1)
        assertThat(data.lifetimeStats.totalDistanceMeters).isEqualTo(1_500L)
        assertThat(data.dailyLog).hasSize(1)
    }

    @Test
    fun `壊れたJSONを読み込んでも記録は変わらない`() = runBlocking<Unit> {
        recordDistance(5_000L, Instant.parse("2026-08-20T03:00:00Z"))

        val result = importBackup("これはJSONではない")

        assertThat(result).isInstanceOf(ImportResult.Malformed::class.java)
        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(5_000L)
    }

    @Test
    fun `新しい版数のJSONを読み込んでも記録は変わらない`() = runBlocking<Unit> {
        recordDistance(5_000L, Instant.parse("2026-08-20T03:00:00Z"))
        val json = exportBackup().replace("\"schema_version\": 1", "\"schema_version\": 99")

        val result = importBackup(json)

        assertThat(result).isEqualTo(ImportResult.UnsupportedVersion(99))
        assertThat(progressRepository.lifetimeStats.first().totalDistanceMeters).isEqualTo(5_000L)
    }

    @Test
    fun `周回記録も取り込まれる`() = runBlocking<Unit> {
        recordDistance(
            io.github.task320.earthstep.core.domain.progress.Earth.CIRCUMFERENCE_METERS,
            Instant.parse("2026-08-20T03:00:00Z"),
        )
        val json = exportBackup()

        progressRepository.resetAll()
        milestoneRepository.resetAll()
        importBackup(json)

        assertThat(progressRepository.lapRecords.first().map { it.lapNumber }).containsExactly(1)
        assertThat(progressRepository.lifetimeStats.first().currentLap).isEqualTo(2)
    }
}
