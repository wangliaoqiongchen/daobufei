package com.mo.daobufei

import com.mo.daobufei.update.InvalidManifestException
import com.mo.daobufei.update.UpdateConfig
import com.mo.daobufei.update.UpdateManifestParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 2026-11-23 云端实测 Manifest(契约 §4.2),versionCode 5
private const val REAL_MANIFEST = """
{
  "schemaVersion": 1,
  "appId": "daobufei",
  "packageName": "com.mo.daobufei",
  "channel": "stable",
  "versionCode": 5,
  "versionName": "2.1.1",
  "title": "更新服务联调测试版",
  "publishedAt": "2026-11-23T10:09:08.186Z",
  "minSupportedVersionCode": 1,
  "changelog": [
    "855886 Update Center 首个测试版本",
    "通过统一更新接口检测到的演示更新"
  ],
  "downloadPageUrl": "https://github.com/wangliaoqiongchen/daobufei/releases",
  "downloadUrl": null,
  "sha256": null,
  "fileSizeBytes": null
}
"""

class UpdateManifestParserTest {

    private val config = UpdateConfig()

    @Test
    fun `真实契约 Manifest 解析通过`() {
        val m = UpdateManifestParser.parse(REAL_MANIFEST, config)
        assertEquals(5L, m.versionCode)
        assertEquals("2.1.1", m.versionName)
        assertEquals("daobufei", m.appId)
        assertEquals("com.mo.daobufei", m.packageName)
        assertEquals("stable", m.channel)
        assertEquals(1L, m.minSupportedVersionCode)
        assertEquals(2, m.changelog.size)
        assertEquals(null, m.downloadUrl)
        assertEquals(null, m.sha256)
        assertEquals(null, m.fileSizeBytes)
    }

    @Test
    fun `schemaVersion 非 1 拒绝`() {
        val bad = REAL_MANIFEST.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        assertTrue(runCatching { UpdateManifestParser.parse(bad, config) }.isFailure)
    }

    @Test
    fun `packageName 不匹配拒绝`() {
        val bad = REAL_MANIFEST.replace("com.mo.daobufei", "com.other.app")
        assertTrue(runCatching { UpdateManifestParser.parse(bad, config) }.isFailure)
    }

    @Test
    fun `appId 不匹配拒绝`() {
        val bad = REAL_MANIFEST.replace("\"appId\": \"daobufei\"", "\"appId\": \"other\"")
        assertTrue(runCatching { UpdateManifestParser.parse(bad, config) }.isFailure)
    }

    @Test
    fun `changelog 缺失拒绝`() {
        val bad = REAL_MANIFEST.replace(Regex("\"changelog\": \\[[^]]*\\]"), "\"changelog\": []")
        assertTrue(runCatching { UpdateManifestParser.parse(bad, config) }.isFailure)
    }

    @Test
    fun `downloadPageUrl 非 https 拒绝`() {
        val bad = REAL_MANIFEST.replace("https://github.com", "http://github.com")
        assertTrue(runCatching { UpdateManifestParser.parse(bad, config) }.isFailure)
    }

    @Test
    fun `publishedAt 无效拒绝`() {
        val bad = REAL_MANIFEST.replace("\"publishedAt\": \"2026-11-23T10:09:08.186Z\"", "\"publishedAt\": \"not-a-time\"")
        assertTrue(runCatching { UpdateManifestParser.parse(bad, config) }.isFailure)
    }

    @Test
    fun `minSupportedVersionCode 大于当前版本拒绝`() {
        val bad = REAL_MANIFEST.replace("\"minSupportedVersionCode\": 1", "\"minSupportedVersionCode\": 9")
        assertTrue(runCatching { UpdateManifestParser.parse(bad, config) }.isFailure)
    }
}
