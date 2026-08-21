package io.github.task320.earthstep.core.domain.usecase

import io.github.task320.earthstep.core.common.time.AppTimeSource
import io.github.task320.earthstep.core.domain.repository.DriveSyncRepository
import io.github.task320.earthstep.core.domain.repository.DriveSyncStateRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drive の `appDataFolder` との同期(P7-7)。
 *
 * 手順は手動インポート/エクスポート(P7-3〜P7-4)の合成でしかない。
 * 1. Driveに既存のバックアップがあれば取得し、いまの記録とマージする(競合解決はP7-4のまま)
 * 2. マージ後の状態をDriveへ書き戻す(取得できなかった場合も、いまの記録をそのまま書く)
 *
 * こうしておくと「先に書き込んだ端末の記録が上書きで消える」ことがない。
 * 成功したら「最終同期日時」(P7-9)を必ず記録する。手動同期(P7-7)・自動同期(P7-8)の
 * どちらから呼ばれても同じ経路を通るので、記録漏れが起きない。
 */
@Singleton
class SyncWithDriveUseCase @Inject constructor(
    private val driveSyncRepository: DriveSyncRepository,
    private val driveSyncStateRepository: DriveSyncStateRepository,
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val timeSource: AppTimeSource,
) {

    suspend operator fun invoke(accessToken: String): SyncResult {
        val remoteJson = driveSyncRepository.download(accessToken)
        val addedMeters = if (remoteJson != null) {
            when (val result = importBackup(remoteJson)) {
                is ImportResult.Success -> result.addedMeters
                // 壊れたJSONや未来の版数を無理に取り込んでDriveへ書き戻すと、
                // 破損したデータで正常なバックアップを上書きしてしまう。同期そのものを中断する。
                is ImportResult.Malformed, is ImportResult.UnsupportedVersion -> return SyncResult.Failed
            }
        } else {
            0L
        }

        val uploaded = driveSyncRepository.upload(accessToken, exportBackup())
        if (!uploaded) return SyncResult.Failed

        driveSyncStateRepository.recordSyncSuccess(timeSource.now())
        return SyncResult.Success(addedMeters)
    }
}

sealed interface SyncResult {
    /** @param addedMeters Driveから取り込んで増えた距離(m)。Drive側に何も無ければ 0。 */
    data class Success(val addedMeters: Long) : SyncResult
    data object Failed : SyncResult
}
