package io.github.task320.earthstep.feature.measurement

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.task320.earthstep.core.data.measurement.MeasurementEngine
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 計測を常駐させるフォアグラウンドサービス(P3-1)。
 *
 * サービス自身は計測ロジックを持たず、[MeasurementEngine] を動かし続けることと
 * 常駐通知を出し続けることだけを担当する。
 *
 * ## プロセス死からの復帰(P3-6)
 * `START_STICKY` を返すので、メモリ不足で殺されてもシステムが再生成する。
 * 再生成時は intent が null で来るため、状態は引数ではなく永続化層から復元する。
 * [MeasurementEngine.run] は起動のたびに歩幅・歩数の基準値・当日距離をDBから読み直すので、
 * サービス側で引き継ぐ状態は持たない。
 *
 * 遡及除外バッファ(最大3秒ぶん、徒歩なら数メートル)だけは、プロセスを強制終了された場合に
 * 失われる。確定した距離は都度DBへ書いているため、失われるのはこの範囲に限られる。
 */
@AndroidEntryPoint
class MeasurementService : Service() {

    @Inject lateinit var engine: MeasurementEngine

    @Inject lateinit var progressRepository: ProgressRepository

    @Inject lateinit var notifications: MeasurementNotifications

    @Inject lateinit var permissionChecker: PermissionChecker

    private val scope = CoroutineScope(SupervisorJob())
    private var engineJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!permissionChecker.currentState().canMeasure) {
            // Android 14 以降、位置情報の権限が無い状態で location 型の FGS を始めると落ちる。
            Timber.w("location permission missing; not starting measurement")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithCurrentDistance()
        if (engineJob == null) {
            engineJob = scope.launch { engine.run() }
            scope.launch { keepNotificationUpdated() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // エンジンのキャンセルで、寝かせていた距離が書き切られる。
        scope.cancel()
        engineJob = null
        super.onDestroy()
    }

    private fun startForegroundWithCurrentDistance() {
        ServiceCompat.startForeground(
            this,
            MeasurementNotifications.NOTIFICATION_ID,
            notifications.build(totalDistanceMeters = 0L),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        scope.launch {
            val total = progressRepository.lifetimeStats.first().totalDistanceMeters
            notifications.update(total)
        }
    }

    /**
     * 累計距離が変わったら通知を差し替える。
     * 1mごとに更新すると歩いている間じゅう通知を書き換えることになるため、
     * 表示が変わる粒度([NOTIFICATION_STEP_METERS])まで間引く。
     */
    private suspend fun keepNotificationUpdated() {
        progressRepository.lifetimeStats
            .map { it.totalDistanceMeters }
            .map { it / NOTIFICATION_STEP_METERS }
            .distinctUntilChanged()
            .collect { notifications.update(it * NOTIFICATION_STEP_METERS) }
    }

    companion object {
        /** 通知を更新する距離の刻み(m)。 */
        private const val NOTIFICATION_STEP_METERS = 10L

        fun intent(context: Context): Intent = Intent(context, MeasurementService::class.java)
    }
}
