package com.mo.daobufei

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeUtilTest {

    @Test
    fun `小时分钟补零格式化`() {
        assertEquals("08:05", TimeUtil.hhmm(8, 5))
        assertEquals("00:00", TimeUtil.hhmm(0, 0))
        assertEquals("23:59", TimeUtil.hhmm(23, 59))
    }

    @Test
    fun `解析与格式化互为逆运算`() {
        val t = TimeUtil.parse("22:30")
        assertEquals(LocalTime.of(22, 30), t)
        assertEquals("22:30", TimeUtil.hhmm(t.hour, t.minute))
    }

    @Test(expected = NumberFormatException::class)
    fun `非法输入解析抛异常`() {
        TimeUtil.parse("abc")
    }
}
