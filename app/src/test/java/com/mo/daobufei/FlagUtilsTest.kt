package com.mo.daobufei

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlagUtilsTest {

    @Test
    fun `国家码转 emoji 大小写均可`() {
        assertEquals("\uD83C\uDDFA\uD83C\uDDF8", FlagUtils.countryCodeToFlag("US"))
        assertEquals("\uD83C\uDDFA\uD83C\uDDF8", FlagUtils.countryCodeToFlag("us"))
        assertEquals("\uD83C\uDDF0\uD83C\uDDF7", FlagUtils.countryCodeToFlag("Kr"))
    }

    @Test
    fun `非法国家码返回白旗`() {
        assertEquals("🏳️", FlagUtils.countryCodeToFlag(""))
        assertEquals("🏳️", FlagUtils.countryCodeToFlag("U"))
        assertEquals("🏳️", FlagUtils.countryCodeToFlag("USA"))
    }

    @Test
    fun `静态映射表能命中已收录国旗`() {
        assertTrue(FlagUtils.getFlagDrawableResId("US") != 0)
        assertTrue(FlagUtils.getFlagDrawableResId("hk") != 0)
    }

    @Test
    fun `未收录或非法国家码返回 0`() {
        assertEquals(0, FlagUtils.getFlagDrawableResId("XX"))
        assertEquals(0, FlagUtils.getFlagDrawableResId("USA"))
        assertEquals(0, FlagUtils.getFlagDrawableResId(""))
    }
}
