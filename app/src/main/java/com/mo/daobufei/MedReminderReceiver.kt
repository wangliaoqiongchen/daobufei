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

// 服药时间到:弹提醒通知(推上流体云),带"已服"按钮;并把自己重新排到明天同一时间。
// 流体云只提升 ongoing 通知,所以提醒必须 ongoing,撤下靠"已服"或超时自动清理
class MedReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    context.getString(R.string.channel_medication_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = context.getString(R.string.channel_medication_desc) }
            )
        }

        // 超时清理:撤下通知
        if (intent.action == ACTION_DISMISS) {
            NotificationManagerCompat.from(context)
                .cancel(intent.getIntExtra(EXTRA_NOTIF_ID, -1))
            return
        }

        val medId = intent.getLongExtra("medId", 0L)
        val name = intent.getStringExtra("name") ?: context.getString(R.string.notif_default_med_name)
        val note = intent.getStringExtra("note").orEmpty()
        val time = intent.getStringExtra("time") ?: "00:00"
        val notifId = MedicationStore.doseNotificationId(time, medId)

        val openApp = PendingIntent.getActivity(
            context, 2, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val takenIntent = Intent(context, MedTakenReceiver::class.java)
            .putExtra("medId", medId)
            .putExtra("time", time)
        val takenAction = PendingIntent.getBroadcast(
            context, notifId, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismiss = PendingIntent.getBroadcast(
            context, notifId or DISMISS_FLAG, dismissIntent(context, notifId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_med_notification)
            .setContentTitle(context.getString(R.string.notif_med_title, name))
            .setContentText(note.ifEmpty { context.getString(R.string.notif_med_text, time) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // 流体云只提升 ongoing 通知:必须 ongoing 才会上岛,chip 右侧显示药名
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(name.take(8))
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_medication, context.getString(R.string.notif_action_taken), takenAction)
            .build()
        NotificationManagerCompat.from(context).notify(notifId, notification)

        // 15 分钟没吃也自动撤下(状态在页面和日历里仍会记为超时)
        context.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + AUTO_DISMISS_MS,
            dismiss
        )

        // 每日重复:排到明天同一时间
        MedicationStore.rescheduleDose(context, medId, time)
    }

    private fun dismissIntent(context: Context, notifId: Int): Intent =
        Intent(context, MedReminderReceiver::class.java)
            .setAction(ACTION_DISMISS)
            .putExtra(EXTRA_NOTIF_ID, notifId)

    companion object {
        private const val CHANNEL = "medication_channel"
        private const val ACTION_DISMISS = "com.mo.daobufei.action.DISMISS_MED_REMINDER"
        private const val EXTRA_NOTIF_ID = "notifId"
        private const val DISMISS_FLAG = 1 shl 20
        private const val AUTO_DISMISS_MS = 15 * 60 * 1000L
    }
}
