package io.github.task320.earthstep.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.task320.earthstep.core.data.auth.GoogleDriveAuthorization
import io.github.task320.earthstep.core.domain.repository.GoogleAuthRepository
import io.github.task320.earthstep.core.domain.usecase.SyncResult
import io.github.task320.earthstep.core.domain.usecase.SyncWithDriveUseCase
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Driveへの定期同期(P7-8)。
 *
 * バックグラウンドではActivityを起動できないため、認可の同意画面が要る状態
 * ([GoogleDriveAuthorization.Outcome.NeedsConsent])では同期をあきらめる。
 * 一度でも手動で「今すぐ同期」(P7-7)を行って許可済みにしておけば、以降はここで無人のまま進む。
 */
@HiltWorker
class DriveSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val googleAuthRepository: GoogleAuthRepository,
    private val syncWithDrive: SyncWithDriveUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // サインインしていなければ同期対象が無い。リトライしても状況は変わらないので成功扱いで終える。
        googleAuthRepository.signedInAccount.first() ?: return Result.success()

        return when (val outcome = GoogleDriveAuthorization.authorize(applicationContext)) {
            is GoogleDriveAuthorization.Outcome.Authorized -> when (syncWithDrive(outcome.accessToken)) {
                is SyncResult.Success -> Result.success()
                SyncResult.Failed -> Result.retry()
            }

            is GoogleDriveAuthorization.Outcome.NeedsConsent -> {
                Timber.i("drive sync skipped: consent screen required, cannot show from background")
                Result.success()
            }

            GoogleDriveAuthorization.Outcome.Failed -> Result.retry()
        }
    }
}
