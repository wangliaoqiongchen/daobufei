package com.mo.daobufei

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class NextTriggerTest {

    // 2026-10-07 是周三
    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 7, 9, 0)

    private fun expect(date: LocalDateTime): Long =
        date.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `生效星期在今天且时间未过,排今天`() {
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 7, 10, 30)),
            ReminderScheduler.nextTriggerAt(listOf(3), 10, 30, now)
        )
    }

    @Test
    fun `生效星期在今天但时间已过,排下周同一天`() {
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 14, 8, 0)),
            ReminderScheduler.nextTriggerAt(listOf(3), 8, 0, now)
        )
    }

    @Test
    fun `空星期列表等于每天`() {
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 8, 8, 0)),
            ReminderScheduler.nextTriggerAt(emptyList(), 8, 0, now)
        )
    }

    @Test
    fun `多星期取最近一次`() {
        // 周一(10-05 已过 → 10-12)、周三(07:30 已过 → 10-14)、周五(10-09) → 最近是周五
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 9, 7, 30)),
            ReminderScheduler.nextTriggerAt(listOf(1, 3, 5), 7, 30, now)
        )
    }

    @Test
    fun `跨月进位`() {
        // 10-31 是周六;生效星期周一 → 11-02
        val lastDay = LocalDateTime.of(2026, 10, 31, 23, 0)
        assertEquals(
            expect(LocalDateTime.of(2026, 11, 2, 7, 30)),
            ReminderScheduler.nextTriggerAt(listOf(1), 7, 30, lastDay)
        )
    }

    @Test
    fun `用药每日闹钟的字符串时间排期`() {
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 8, 8, 0)),
            MedicationStore.nextTriggerAt("08:00", now)
        )
    }
}
