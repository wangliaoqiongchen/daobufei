package com.mo.daobufei

import java.time.LocalTime
import java.util.Locale

// 时间格式化/解析:自定义提醒与用药提醒共用
object TimeUtil {

    fun hhmm(hour: Int, minute: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", hour, minute)

    fun parse(time: String): LocalTime {
        val parts = time.split(":")
        return LocalTime.of(parts[0].toInt(), parts[1].toInt())
    }
}
