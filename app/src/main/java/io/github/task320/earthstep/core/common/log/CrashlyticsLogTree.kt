package io.github.task320.earthstep.core.common.log

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * WARN 以上の Timber ログを Crashlytics へ転送する Tree(P8-8)。
 * DebugTree/ReleaseLogTree と並行して常時 plant する。
 * ログ本文に緯度・経度を含めない方針(P0-6, ReleaseLogTree参照)はここでも前提とする。
 */
class CrashlyticsLogTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log("${tag ?: "EarthStep"}: $message")
        if (t != null) {
            crashlytics.recordException(t)
        }
    }
}
