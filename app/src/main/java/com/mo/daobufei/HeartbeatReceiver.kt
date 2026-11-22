package com.mo.daobufei

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

// 保活心跳:灭屏/充电时设备深度休眠,协程 delay 不会唤醒 CPU;
// 精确闹钟(USE_EXACT_ALARM)不受 Doze 限制,到点解冻进程,
// 服务里检查测量循环是否存活并刷新通知,再排下一次心跳
class HeartbeatReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val svc = Intent(context, VpnMonitorService::class.java)
            .setAction(VpnMonitorService.ACTION_HEARTBEAT)
        ContextCompat.startForegroundService(context, svc)
    }
}
