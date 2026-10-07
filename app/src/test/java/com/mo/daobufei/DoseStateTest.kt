package com.mo.daobufei

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DoseStateTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 7, 12, 0)
    private val createdAt: Long = now.minusHours(6).atZone(java.time.ZoneId.systemDefault())
        .toInstant().toEpochMilli()

    @Test
    fun `已标记已服优先级最高`() {
        val scheduled = now.minusHours(2)
        assertEquals(
            MedicationStore.STATE_TAKEN,
            MedicationStore.computeDoseState(
                isTaken = true, scheduled = scheduled,
                createdAtMillis = createdAt, now = now, graceMinutes = 60
            )
        )
    }

    @Test
    fun `过宽限期未服记超时`() {
        val scheduled = now.minusHours(2)   // 10:00,宽限 1 小时,现在 12:00
        assertEquals(
            MedicationStore.STATE_MISSED,
            MedicationStore.computeDoseState(
                isTaken = false, scheduled = scheduled,
                createdAtMillis = createdAt, now = now, graceMinutes = 60
            )
        )
    }

    @Test
    fun `宽限期内仍算待服`() {
        val scheduled = now.minusMinutes(30) // 11:30,宽限到 12:30
        assertEquals(
            MedicationStore.STATE_PENDING,
            MedicationStore.computeDoseState(
                isTaken = false, scheduled = scheduled,
                createdAtMillis = createdAt, now = now, graceMinutes = 60
            )
        )
    }

    @Test
    fun `未来时间算待服`() {
        val scheduled = now.plusHours(3)
        assertEquals(
            MedicationStore.STATE_PENDING,
            MedicationStore.computeDoseState(
                isTaken = false, scheduled = scheduled,
                createdAtMillis = createdAt, now = now, graceMinutes = 60
            )
        )
    }

    @Test
    fun `添加药品之前的时间点不记超时`() {
        // 药是 12:00-6h=06:00 添加的;09:00 那次虽已过宽限期,也只算待服不算超时
        val scheduled = now.minusHours(3)   // 09:00 > 创建时间 06:00? 不,09:00 在 06:00 之后
        // 构造一个真正早于创建时间的时间点:今天 05:00
        val beforeCreation = now.withHour(5)
        assertEquals(
            MedicationStore.STATE_PENDING,
            MedicationStore.computeDoseState(
                isTaken = false, scheduled = beforeCreation,
                createdAtMillis = createdAt, now = now, graceMinutes = 60
            )
        )
        // 对照组:09:00 在创建之后,正常判超时
        assertEquals(
            MedicationStore.STATE_MISSED,
            MedicationStore.computeDoseState(
                isTaken = false, scheduled = scheduled,
                createdAtMillis = createdAt, now = now, graceMinutes = 60
            )
        )
    }
}
