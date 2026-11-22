package com.mo.daobufei

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

// 一条自定义提醒:标题 + 小标题 + 时间 + 生效的星期(1=周一..7=周日;空 = 每天)
data class Reminder(
    val id: Long,
    val title: String,
    val subtitle: String,
    val hour: Int,
    val minute: Int,
    val days: List<Int> = emptyList()
)

// 提醒的本地存储:SharedPreferences + JSON 数组,数量级小,不需要数据库
object ReminderStore {
    private const val PREFS = "reminders"
    private const val KEY_ITEMS = "items"

    fun list(context: Context): List<Reminder> {
        val json = prefs(context).getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val days = mutableListOf<Int>()
                val dArr = o.optJSONArray("days")
                if (dArr != null) {
                    (0 until dArr.length()).forEach { j -> days.add(dArr.getInt(j)) }
                }
                Reminder(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    subtitle = o.optString("subtitle"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    days = days
                )
            }.sortedWith(compareBy({ it.hour }, { it.minute }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, reminder: Reminder) {
        val items = list(context).toMutableList()
        items.add(reminder)
        save(context, items)
    }

    fun remove(context: Context, id: Long) {
        save(context, list(context).filterNot { it.id == id })
    }

    fun find(context: Context, id: Long): Reminder? = list(context).find { it.id == id }

    private fun save(context: Context, items: List<Reminder>) {
        val arr = JSONArray()
        items.forEach { r ->
            val days = JSONArray()
            r.days.forEach { days.put(it) }
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("title", r.title)
                    .put("subtitle", r.subtitle)
                    .put("hour", r.hour)
                    .put("minute", r.minute)
                    .put("days", days)
            )
        }
        prefs(context).edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

// 调度:AlarmManager.setAlarmClock 是精确闹钟且不需要 SCHEDULE_EXACT_ALARM 权限
// (系统会显示一个小闹钟图标,对用户可见,适合提醒场景)
// 每条提醒同一时刻只挂"最近一次"的闹钟,触发后重算下一次
object ReminderScheduler {

    fun nextTriggerAt(reminder: Reminder): Long =
        nextTriggerAt(reminder.days, reminder.hour, reminder.minute, LocalDateTime.now())

    fun nextTriggerAt(days: List<Int>, hour: Int, minute: Int, now: LocalDateTime): Long {
        // 空列表 = 每天:取所有星期里最近的一次触发
        val effective = days.ifEmpty { (1..7).toList() }
        return effective.minOf { d ->
            var next = now.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(d)))
                .atTime(hour, minute)
            if (!next.isAfter(now)) next = next.plusWeeks(1)
            next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }

    fun schedule(context: Context, reminder: Reminder) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val fireIntent = Intent(context, ReminderReceiver::class.java)
            .putExtra("id", reminder.id)
            .putExtra("title", reminder.title)
            .putExtra("subtitle", reminder.subtitle)
            .putExtra("hour", reminder.hour)
            .putExtra("minute", reminder.minute)
            .putExtra("days", reminder.days.toIntArray())
        val fire = PendingIntent.getBroadcast(
            context, requestId(reminder.id), fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(nextTriggerAt(reminder), openApp),
            fire
        )
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val fire = PendingIntent.getBroadcast(
            context, requestId(id),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(fire)
    }

    private fun requestId(id: Long) = (id % Int.MAX_VALUE).toInt()
}
