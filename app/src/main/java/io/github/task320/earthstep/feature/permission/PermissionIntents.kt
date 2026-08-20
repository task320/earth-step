package io.github.task320.earthstep.feature.permission

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import timber.log.Timber

/**
 * 設定画面への導線(仕様7.1-2 / 7.2)。
 *
 * 端末によっては該当の設定画面が存在しないことがあるため、
 * どの導線も「開けなかったら1段上の画面へ落とす」形にしてある。
 */
object PermissionIntents {

    /** アプリの詳細設定。背景位置を「常に許可」へ変えてもらうときに開く(P3-3)。 */
    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startSafely(intent)
    }

    /**
     * バッテリー最適化の除外をその場でお願いする(仕様7.2 / P3-7)。
     *
     * 直接ダイアログを出す `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` は
     * 端末によっては塞がれているため、開けなければ一覧画面へ誘導する。
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (context.startSafely(direct)) return

        context.startSafely(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * 開けたら true。
     * 開けるかを `resolveActivity` で先に調べる方法は、Android 11 以降のパッケージ可視性の制約で
     * 正しく判定できないことがあるため、実際に投げて失敗を拾う。
     */
    private fun Context.startSafely(intent: Intent): Boolean = runCatching { startActivity(intent) }
        .onFailure { Timber.w(it, "could not open settings: %s", intent.action) }
        .isSuccess
}
