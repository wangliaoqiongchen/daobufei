package com.mo.daobufei

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// 一个药品:名称 + 服用说明 + 每天的服药时间列表("HH:mm",升序)
data class Medication(
    val id: Long,
    val name: String,
    val note: String,
    val times: List<String>
)

// 药品存储 + 每日服药闹钟调度 + 服药记录
// 记录粒度:日期 + 时间 + 药品 → 是否已服;超时未服按宽限期推算,不落库
object MedicationStore {
    private const val PREFS = "medications"
    private const val KEY_MEDS = "meds"
    private const val KEY_TAKEN_PREFIX = "taken_"
    private const val GRACE_MINUTES = 60L

    const val STATE_PENDING = 0    // 未到时间/待服
    const val STATE_TAKEN = 1      // 已服
    const val STATE_MISSED = 2     // 超时未服

    fun meds(context: Context): List<Medication> {
        val json = prefs(context).getString(KEY_MEDS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val times = mutableListOf<String>()
                val tArr = o.getJSONArray("times")
                (0 until tArr.length()).forEach { j -> times.add(tArr.getString(j)) }
                Medication(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    note = o.optString("note"),
                    times = times.sorted()
                )
            }.sortedWith(compareBy({ it.times.firstOrNull() ?: "99:99" }, { it.name }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, med: Medication) {
        val items = meds(context).toMutableList()
        items.add(med)
        save(context, items)
        scheduleMed(context, med)
    }

    fun remove(context: Context, id: Long) {
        meds(context).find { it.id == id }?.let { cancelMed(context, it) }
        save(context, meds(context).filterNot { it.id == id })
    }

    fun markTaken(context: Context, date: LocalDate, time: String, medId: Long) {
        prefs(context).edit().putBoolean(takenKey(date, time, medId), true).apply()
    }

    fun isTaken(context: Context, date: LocalDate, time: String, medId: Long): Boolean =
        prefs(context).getBoolean(takenKey(date, time, medId), false)

    // 某天某次服药的状态:已服 / 超时(过了宽限期没点) / 待服
    fun doseState(context: Context, date: LocalDate, time: String, medId: Long): Int {
        return computeDoseState(
            isTaken = isTaken(context, date, time, medId),
            scheduled = date.atTime(TimeUtil.parse(time)),
            createdAtMillis = medId,
            now = LocalDateTime.now(),
            graceMinutes = GRACE_MINUTES
        )
    }

    // 纯函数版状态判定,可单测:
    // 已服 > 添加药品(创建时间戳)之前的时间点记未计划 > 过宽限期记超时 > 待服
    fun computeDoseState(
        isTaken: Boolean,
        scheduled: LocalDateTime,
        createdAtMillis: Long,
        now: LocalDateTime,
        graceMinutes: Long
    ): Int {
        if (isTaken) return STATE_TAKEN
        val scheduledMillis = scheduled.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (scheduledMillis < createdAtMillis) return STATE_PENDING
        if (scheduled.plusMinutes(graceMinutes).isBefore(now)) return STATE_MISSED
        return STATE_PENDING
    }

    // 服药通知的通知 id:同一药品同一时间每天复用,方便点击"已服"后取消
    fun doseNotificationId(time: String, medId: Long) = "$time|$medId".hashCode()

    // ==================== 调度 ====================

    fun scheduleMed(context: Context, med: Medication) {
        med.times.forEach { scheduleOne(context, med, it) }
    }

    fun cancelMed(context: Context, med: Medication) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        med.times.forEach { time ->
            am.cancel(
                PendingIntent.getBroadcast(
                    context, requestCode(med.id, time),
                    Intent(context, MedReminderReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }

    // 闹钟触发后把自己排到明天同一时间(药已删除则不再排)
    fun rescheduleDose(context: Context, medId: Long, time: String) {
        meds(context).find { it.id == medId }?.let { scheduleOne(context, it, time) }
    }

    private fun scheduleOne(context: Context, med: Medication, time: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val fire = PendingIntent.getBroadcast(
            context, requestCode(med.id, time),
            Intent(context, MedReminderReceiver::class.java)
                .putExtra("medId", med.id)
                .putExtra("name", med.name)
                .putExtra("note", med.note)
                .putExtra("time", time),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // setAlarmClock:精确闹钟,无需 SCHEDULE_EXACT_ALARM 权限,Doze 下也会准点
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(nextTriggerAt(time), openApp),
            fire
        )
    }

    fun nextTriggerAt(time: String): Long = nextTriggerAt(time, LocalDateTime.now())

    fun nextTriggerAt(time: String, now: LocalDateTime): Long {
        var next = now.toLocalDate().atTime(TimeUtil.parse(time))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun requestCode(medId: Long, time: String) = "${medId}_$time".hashCode()

    private fun takenKey(date: LocalDate, time: String, medId: Long) =
        "${KEY_TAKEN_PREFIX}${date}_${time}_$medId"

    private fun save(context: Context, items: List<Medication>) {
        val arr = JSONArray()
        items.forEach { m ->
            val times = JSONArray()
            m.times.forEach { times.put(it) }
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("name", m.name)
                    .put("note", m.note)
                    .put("times", times)
            )
        }
        prefs(context).edit().putString(KEY_MEDS, arr.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
