package com.mo.daobufei

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 开机 / 应用更新后系统会清掉所有 AlarmManager 闹钟,这里把
// 自定义提醒和用药提醒的闹钟全部重排,不需要用户手动打开 App
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        // 读取量小,同步执行即可
        ReminderStore.list(context).forEach { ReminderScheduler.schedule(context, it) }
        MedicationStore.meds(context).forEach { MedicationStore.scheduleMed(context, it) }
    }
}
