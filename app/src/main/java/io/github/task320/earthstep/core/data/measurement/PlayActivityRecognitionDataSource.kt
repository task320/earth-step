package io.github.task320.earthstep.core.data.measurement

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.task320.earthstep.core.domain.measurement.model.ActivityTransitionEvent
import io.github.task320.earthstep.core.domain.measurement.model.ActivityUpdate
import io.github.task320.earthstep.core.domain.measurement.model.UserActivity
import io.github.task320.earthstep.core.domain.measurement.source.ActivityRecognitionDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * Play Services の ActivityRecognition / ActivityTransition を購読する(P2-18 / P2-19)。
 *
 * どちらの API も結果を `PendingIntent` で返すため、購読している間だけ
 * 動的登録の BroadcastReceiver を立てて受け取る。マニフェストへ静的に宣言しないのは、
 * 計測していない間に起こされても捨てるだけで、無駄に電力を使うだけだから。
 *
 * `PendingIntent` の送信はアプリ自身の権限で行われるため、レシーバは非公開でよい。
 */
@Singleton
class PlayActivityRecognitionDataSource @Inject constructor(@ApplicationContext private val context: Context) :
    ActivityRecognitionDataSource {

    private val client = ActivityRecognition.getClient(context)

    @SuppressLint("MissingPermission")
    override val activities: Flow<ActivityUpdate> = callbackFlow {
        val action = "${context.packageName}.ACTIVITY_UPDATE"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val result = intent?.let(ActivityRecognitionResult::extractResult) ?: return
                val probable = result.mostProbableActivity
                trySend(
                    ActivityUpdate(
                        activity = probable.type.toUserActivity(),
                        confidence = probable.confidence,
                        timestampMillis = result.time,
                    ),
                )
            }
        }
        val pendingIntent = registerReceiver(receiver, action)

        client.requestActivityUpdates(UPDATE_INTERVAL_MILLIS, pendingIntent)
            .addOnFailureListener { Timber.w(it, "requestActivityUpdates failed") }

        awaitClose {
            client.removeActivityUpdates(pendingIntent)
            context.unregisterReceiver(receiver)
        }
    }

    @SuppressLint("MissingPermission")
    override val transitions: Flow<ActivityTransitionEvent> = callbackFlow {
        val action = "${context.packageName}.ACTIVITY_TRANSITION"
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null || !ActivityTransitionResult.hasResult(intent)) return
                ActivityTransitionResult.extractResult(intent)?.transitionEvents?.forEach { event ->
                    trySend(
                        ActivityTransitionEvent(
                            activity = event.activityType.toUserActivity(),
                            isEnter = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                            timestampMillis = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
        val pendingIntent = registerReceiver(receiver, action)

        client.requestActivityTransitionUpdates(transitionRequest(), pendingIntent)
            .addOnFailureListener { Timber.w(it, "requestActivityTransitionUpdates failed") }

        awaitClose {
            client.removeActivityTransitionUpdates(pendingIntent)
            context.unregisterReceiver(receiver)
        }
    }

    private fun registerReceiver(receiver: BroadcastReceiver, action: String): PendingIntent {
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(action),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(action).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    /**
     * 監視する遷移(仕様1.2 / P2-19)。
     * `STILL` から出る/入る、`ON_FOOT` へ入る/出る の4種だけを見て、GPSの起動と停止を決める。
     */
    private fun transitionRequest(): ActivityTransitionRequest = ActivityTransitionRequest(
        listOf(DetectedActivity.STILL, DetectedActivity.WALKING, DetectedActivity.RUNNING)
            .flatMap { type ->
                listOf(
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT,
                ).map { transition ->
                    ActivityTransition.Builder()
                        .setActivityType(type)
                        .setActivityTransition(transition)
                        .build()
                }
            },
    )

    private fun Int.toUserActivity(): UserActivity = when (this) {
        DetectedActivity.ON_FOOT, DetectedActivity.WALKING, DetectedActivity.RUNNING -> UserActivity.ON_FOOT
        DetectedActivity.ON_BICYCLE -> UserActivity.ON_BICYCLE
        DetectedActivity.IN_VEHICLE -> UserActivity.IN_VEHICLE
        DetectedActivity.STILL -> UserActivity.STILL
        else -> UserActivity.UNKNOWN
    }

    private companion object {
        /** 活動判定の要求間隔。細かくしても判定は変わらず、電力だけ食う。 */
        const val UPDATE_INTERVAL_MILLIS = 30_000L
    }
}
