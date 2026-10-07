package com.mo.daobufei

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class NextTriggerTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 7, 9, 0)

    private fun expect(date: LocalDateTime): Long =
        date.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `未来时间排今天`() {
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 7, 10, 30)),
            ReminderScheduler.nextTriggerAt(10, 30, now)
        )
    }

    @Test
    fun `已过时间排明天`() {
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 8, 8, 0)),
            ReminderScheduler.nextTriggerAt(8, 0, now)
        )
    }

    @Test
    fun `恰好等于当前时间排明天`() {
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 8, 9, 0)),
            ReminderScheduler.nextTriggerAt(9, 0, now)
        )
    }

    @Test
    fun `跨月末进位`() {
        val lastDay = LocalDateTime.of(2026, 10, 31, 23, 0)
        assertEquals(
            expect(LocalDateTime.of(2026, 11, 1, 7, 30)),
            ReminderScheduler.nextTriggerAt(7, 30, lastDay)
        )
    }

    @Test
    fun `字符串时间同样排期`() {
        assertEquals(
            expect(LocalDateTime.of(2026, 10, 8, 8, 0)),
            MedicationStore.nextTriggerAt("08:00", now)
        )
    }
}
