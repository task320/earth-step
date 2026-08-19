package io.github.task320.earthstep.core.common.log

import android.util.Log
import timber.log.Timber

/**
 * リリースビルド用の Timber Tree。
 *
 * ログ方針(P0-6):
 * - WARN 以上のみ OS のログへ流す(VERBOSE/DEBUG/INFO は捨てる)
 * - 緯度・経度・生の位置情報は **ビルド種別を問わず** ログへ出力しない。
 *   端末ログは他アプリからは読めないが、バグ報告や adb 経由で外部へ出る経路があるため。
 *   位置情報をデバッグしたい場合は、距離・精度・件数など集計済みの値のみを出す。
 */
class ReleaseLogTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val resolvedTag = tag ?: DEFAULT_TAG
        Log.println(priority, resolvedTag, message)
        if (t != null) {
            Log.println(priority, resolvedTag, Log.getStackTraceString(t))
        }
    }

    private companion object {
        const val DEFAULT_TAG = "EarthStep"
    }
}
