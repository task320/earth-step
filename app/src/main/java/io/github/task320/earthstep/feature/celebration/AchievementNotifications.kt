package io.github.task320.earthstep.feature.celebration

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.task320.earthstep.MainActivity
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.domain.progress.ProgressEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 達成通知(仕様3.4 / P5-12)。
 *
 * 複数を一度に達成した場合は個別通知を連発せず「まとめて3個達成」に集約する。
 * 歩いている間に何度も通知が鳴るのは、達成の嬉しさより煩わしさが勝つ。
 *
 * 通知をタップするとアプリが開き、貯めておいた演出(P5-13)が順に再生される。
 */
@Singleton
class AchievementNotifications @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_achievement_name),
            // 達成は知らせたいので常駐通知より一段強くする。
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_achievement_description)
        }
        notificationManager?.createNotificationChannel(channel)
    }

    /** 通知を出せる状態か。拒否されていても計測は続ける(P3-4 の縮退動作)。 */
    fun canNotify(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyAchievements(events: List<ProgressEvent>) {
        if (events.isEmpty() || !canNotify()) return
        ensureChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titleFor(events))
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun titleFor(events: List<ProgressEvent>): String {
        if (events.size > 1) {
            return context.getString(R.string.notification_achievement_multiple, events.size)
        }
        return context.getString(R.string.notification_achievement_single, nameOf(events.single()))
    }

    private fun nameOf(event: ProgressEvent): String = when (event) {
        is ProgressEvent.MilestoneAchieved -> event.milestone.name
        is ProgressEvent.LapCompleted ->
            context.getString(R.string.notification_achievement_lap, event.lapNumber)

        is ProgressEvent.LapMarkerReached -> context.getString(
            R.string.notification_achievement_marker,
            CelebrationContent.markerLabel(event.marker),
            event.lapNumber,
        )
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_SHOW_CELEBRATION)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ID = "achievement"
        const val NOTIFICATION_ID = 2

        /** 通知から演出画面へ入るときのアクション(P5-15 のディープリンク)。 */
        const val ACTION_SHOW_CELEBRATION = "io.github.task320.earthstep.action.SHOW_CELEBRATION"

        private const val REQUEST_CODE = 100
    }
}
