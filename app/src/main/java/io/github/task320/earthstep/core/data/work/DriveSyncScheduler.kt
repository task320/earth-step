package io.github.task320.earthstep.core.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Driveへの定期同期のスケジューリング(P7-8)。
 *
 * サインインしているかどうかに関わらず常にスケジュールしておき、
 * 実際にサインインしているかは [DriveSyncWorker] 側で毎回確認する。
 * サインインの有無で登録・解除を出し入れするより、常時登録して中身側で無人判定する方が
 * サインイン/サインアウトのたびに呼び忘れる余地がなく単純。
 */
object DriveSyncScheduler {

    private const val UNIQUE_WORK_NAME = "drive_sync_periodic"
    private const val INTERVAL_HOURS = 24L

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            // 既にスケジュール済みなら間隔を初期化し直さない。毎起動ごとに同期タイミングが
            // 先延ばしにされ続けると、一度もWorkerが走らないまま日が経ってしまう。
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
