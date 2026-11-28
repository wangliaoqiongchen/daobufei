package com.mo.daobufei

import com.mo.daobufei.update.UpdateGate
import com.mo.daobufei.update.UpdateManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateGateTest {

    // 基准:2026-11-28 12:00(今天已成功检测过一次)
    private val now = 1764312000000L
    private val today = "2026-11-28"

    private fun manifest(v: Long, min: Long = 1) = UpdateManifest(
        appId = "daobufei", packageName = "com.mo.daobufei", channel = "stable",
        versionCode = v, versionName = "x", title = "t", publishedAtRaw = "", publishedAtDisplay = "",
        minSupportedVersionCode = min, changelog = listOf("a"), downloadPageUrl = "https://e.com/d",
        downloadUrl = null, sha256 = null, fileSizeBytes = null
    )

    @Test
    fun `版本判断`() {
        assertEquals(UpdateGate.Verdict.NEW_UPDATE, UpdateGate.evaluate(3, manifest(5)))
        assertEquals(UpdateGate.Verdict.UP_TO_DATE, UpdateGate.evaluate(5, manifest(5)))
        assertEquals(UpdateGate.Verdict.UP_TO_DATE, UpdateGate.evaluate(6, manifest(5)))
        assertEquals(UpdateGate.Verdict.FORCED_UPDATE, UpdateGate.evaluate(1, manifest(5, min = 2)))
    }

    @Test
    fun `首次安装无记录,自动检测`() {
        assertTrue(
            UpdateGate.shouldAutoCheck(now, 0, "", 0, today)
        )
    }

    @Test
    fun `今天已成功检测且未满 12 小时,不自动检测`() {
        val successAt = now - 3600_000L // 1 小时前成功
        assertFalse(
            UpdateGate.shouldAutoCheck(now, successAt, today, successAt + 500, today)
        )
    }

    @Test
    fun `距上次成功满 12 小时,自动检测`() {
        val successAt = now - 13 * 3600_000L
        assertTrue(
            UpdateGate.shouldAutoCheck(now, successAt, today, successAt + 500, today)
        )
    }

    @Test
    fun `跨入新自然日,自动检测`() {
        val successAt = now - 3600_000L
        assertTrue(
            UpdateGate.shouldAutoCheck(now, successAt, "2026-11-27", successAt + 500, today)
        )
    }

    @Test
    fun `失败后 15 分钟内不重试`() {
        val failedAttempt = now - 5 * 60_000L // 5 分钟前失败(没有成功记录刷新)
        assertFalse(
            UpdateGate.shouldAutoCheck(now, 0, "", failedAttempt, today)
        )
    }

    @Test
    fun `失败超过 15 分钟后可重试`() {
        val failedAttempt = now - 16 * 60_000L
        assertTrue(
            UpdateGate.shouldAutoCheck(now, 0, "", failedAttempt, today)
        )
    }

    @Test
    fun `成功之后的时间戳不会触发失败节流`() {
        val successAt = now - 13 * 3600_000L
        val attemptAfterSuccess = successAt + 100 // 成功时间戳晚于该次尝试
        assertTrue(
            UpdateGate.shouldAutoCheck(now, successAt, today, attemptAfterSuccess, today)
        )
    }

    @Test
    fun `设备时间回拨不会永久卡死`() {
        val successAt = now + 3600_000L // 成功时间"在未来"(时钟被拨回)
        assertTrue(
            UpdateGate.shouldAutoCheck(now, successAt, today, successAt - 100, today)
        )
    }
}
