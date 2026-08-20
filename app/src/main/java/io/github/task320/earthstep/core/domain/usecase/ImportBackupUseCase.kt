package io.github.task320.earthstep.core.domain.usecase

import io.github.task320.earthstep.core.domain.backup.BackupCodec
import io.github.task320.earthstep.core.domain.backup.BackupData
import io.github.task320.earthstep.core.domain.backup.BackupMerger
import io.github.task320.earthstep.core.domain.backup.BackupParseResult
import io.github.task320.earthstep.core.domain.repository.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSONを読み込んで、いまの記録と突き合わせる(P7-3 / P7-4)。
 *
 * 読み込んだ内容で上書きするのではなく、必ず現在の記録とマージする。
 * 機種変更でも同期でも「両方の端末で歩いた分を残す」のが目的だから。
 */
@Singleton
class ImportBackupUseCase @Inject constructor(private val backupRepository: BackupRepository) {

    suspend operator fun invoke(text: String): ImportResult = when (val parsed = BackupCodec.decode(text)) {
        is BackupParseResult.Malformed -> ImportResult.Malformed(parsed.reason)

        is BackupParseResult.UnsupportedVersion -> ImportResult.UnsupportedVersion(parsed.version)

        is BackupParseResult.Success -> {
            val local = backupRepository.snapshot()
            val merged = BackupMerger.merge(local = local, imported = parsed.data)
            backupRepository.replaceAll(merged)
            ImportResult.Success(
                previousTotalMeters = local.lifetimeStats.totalDistanceMeters,
                mergedTotalMeters = merged.lifetimeStats.totalDistanceMeters,
                merged = merged,
            )
        }
    }
}

/** インポートの結果。 */
sealed interface ImportResult {

    /**
     * @param previousTotalMeters 取り込む前の累計距離。
     * @param mergedTotalMeters 取り込んだ後の累計距離。
     */
    data class Success(val previousTotalMeters: Long, val mergedTotalMeters: Long, val merged: BackupData) :
        ImportResult {
        /** 取り込みで増えた距離(m)。同じ内容を読み込めば 0 になる。 */
        val addedMeters: Long get() = mergedTotalMeters - previousTotalMeters
    }

    data class UnsupportedVersion(val version: Int) : ImportResult

    data class Malformed(val reason: String) : ImportResult
}
