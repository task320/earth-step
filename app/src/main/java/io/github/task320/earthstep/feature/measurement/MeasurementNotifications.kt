package io.github.task320.earthstep.feature.measurement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.task320.earthstep.MainActivity
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 常駐通知の組み立て(P3-1)。
 *
 * 表示するのは「累計距離」と「次のマイルストーンまでの残り」。
 * 通知は歩いている間ずっと出ているため、更新のたびに文言が変わりすぎないよう情報を絞る。
 */
@Singleton
class MeasurementNotifications @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    /** 常駐通知のチャンネル。ユーザーが個別に切れるよう、達成通知(P5-12)とは分ける。 */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_measurement_name),
            // 常駐通知は音を出さない。歩くたびに鳴ると邪魔にしかならない。
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_measurement_description)
            setShowBadge(false)
        }
        notificationManager?.createNotificationChannel(channel)
    }

    fun build(totalDistanceMeters: Long): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(
            context.getString(
                R.string.notification_measurement_title,
                DistanceFormatter.formatDistance(totalDistanceMeters),
            ),
        )
        .setContentText(nextMilestoneText(totalDistanceMeters))
        .setContentIntent(contentIntent())
        .setOngoing(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()

    fun update(totalDistanceMeters: Long) {
        notificationManager?.notify(NOTIFICATION_ID, build(totalDistanceMeters))
    }

    private fun nextMilestoneText(totalDistanceMeters: Long): String {
        val next = MilestoneCatalog.nextAfter(totalDistanceMeters)
            ?: return context.getString(R.string.notification_measurement_all_achieved)
        val remaining = next.distanceMeters - totalDistanceMeters
        return context.getString(
            R.string.notification_measurement_next,
            next.name,
            DistanceFormatter.formatDistance(remaining),
        )
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ID = "measurement"
        const val NOTIFICATION_ID = 1
    }
}
