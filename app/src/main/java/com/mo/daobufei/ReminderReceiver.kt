package com.mo.daobufei

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// 到点弹提醒并推上流体云,把自己重新排到明天同一时间(每日重复)。
// 注意:流体云只提升 ongoing 通知,所以提醒必须 setOngoing(true),
// 撤下靠"知道了"按钮或超时自动清理
class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(REMINDER_CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL,
                    context.getString(R.string.channel_reminder_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = context.getString(R.string.channel_reminder_desc) }
            )
        }

        // "知道了"按钮 / 超时清理:撤下通知
        if (intent.action == ACTION_DISMISS) {
            NotificationManagerCompat.from(context)
                .cancel(intent.getIntExtra(EXTRA_NOTIF_ID, -1))
            return
        }

        val id = intent.getLongExtra("id", 0L)
        val title = intent.getStringExtra("title") ?: context.getString(R.string.notif_default_title)
        val subtitle = intent.getStringExtra("subtitle").orEmpty()
        val hour = intent.getIntExtra("hour", 8)
        val minute = intent.getIntExtra("minute", 0)
        val notifId = (id % Int.MAX_VALUE).toInt()

        val openApp = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismiss = PendingIntent.getBroadcast(
            context, notifId, dismissIntent(context, notifId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle(title)
            .setContentText(subtitle.ifEmpty { context.getString(R.string.notif_default_text) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // 流体云只提升 ongoing 通知:必须 ongoing 才会上岛,chip 右侧显示标题
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(title.take(8))
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_close, context.getString(R.string.notif_action_dismiss), dismiss)
            .build()
        NotificationManagerCompat.from(context).notify(notifId, notification)

        // 挂了 10 分钟还没处理就自动撤下,免得一直占着岛
        context.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + AUTO_DISMISS_MS,
            dismiss
        )

        // 每日重复:立即排到明天同一时间
        ReminderScheduler.schedule(
            context, Reminder(id, title, subtitle, hour, minute)
        )
    }

    private fun dismissIntent(context: Context, notifId: Int): Intent =
        Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_DISMISS)
            .putExtra(EXTRA_NOTIF_ID, notifId)

    companion object {
        private const val REMINDER_CHANNEL = "reminder_channel"
        private const val ACTION_DISMISS = "com.mo.daobufei.action.DISMISS_REMINDER"
        private const val EXTRA_NOTIF_ID = "notifId"
        private const val AUTO_DISMISS_MS = 10 * 60 * 1000L
    }
}
