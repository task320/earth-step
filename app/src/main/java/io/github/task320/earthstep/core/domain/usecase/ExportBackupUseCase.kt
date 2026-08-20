package io.github.task320.earthstep.core.domain.usecase

import io.github.task320.earthstep.core.domain.backup.BackupCodec
import io.github.task320.earthstep.core.domain.repository.BackupRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * いまの記録をJSONへ書き出す(P7-2)。
 *
 * 書き出し先への保存は呼び出し側(SAF)が行う。ここは文字列を作るところまで。
 */
@Singleton
class ExportBackupUseCase @Inject constructor(private val backupRepository: BackupRepository) {

    suspend operator fun invoke(): String = BackupCodec.encode(backupRepository.snapshot())
}
