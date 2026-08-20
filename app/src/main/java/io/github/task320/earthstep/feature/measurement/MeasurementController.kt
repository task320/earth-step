package io.github.task320.earthstep.feature.measurement

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * 計測サービスの開始・停止(P3-1)。
 *
 * 権限が無いまま location 型のフォアグラウンドサービスを開始すると
 * Android 14 以降は `SecurityException` で落ちるため、開始前に必ず権限を確認する。
 */
@Singleton
class MeasurementController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
) {

    /**
     * 計測を開始する。
     * @return 開始できたら true、権限が足りず開始しなかったら false。
     */
    fun start(): Boolean {
        if (!permissionChecker.currentState().canMeasure) {
            Timber.w("cannot start measurement: location permission is missing")
            return false
        }
        ContextCompat.startForegroundService(context, MeasurementService.intent(context))
        return true
    }

    /**
     * 計測を止める。
     * `startService` で停止用の intent を投げる方法は、Android 8 以降のバックグラウンド起動制限に
     * かかりうるため使わない。`stopService` なら制限を受けず、`onDestroy` で
     * 寝かせていた距離も書き切られる。
     */
    fun stop() {
        context.stopService(MeasurementService.intent(context))
    }
}
