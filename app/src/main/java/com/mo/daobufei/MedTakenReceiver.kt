package com.mo.daobufei

import androidx.core.app.NotificationManagerCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate

// 通知上的"已服"按钮 / 页面里的已服按钮落到的接收器:记录当天该次已服,并取消提醒通知
class MedTakenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra("medId", 0L)
        val time = intent.getStringExtra("time") ?: return
        MedicationStore.markTaken(context, LocalDate.now(), time, medId)
        NotificationManagerCompat.from(context)
            .cancel(MedicationStore.doseNotificationId(time, medId))
    }
}
