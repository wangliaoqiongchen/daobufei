package com.mo.daobufei.update

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.OffsetDateTime

// 公开 Manifest 数据模型(契约 §4.2)
data class UpdateManifest(
    val appId: String,
    val packageName: String,
    val channel: String,
    val versionCode: Long,
    val versionName: String,
    val title: String,
    val publishedAtRaw: String,
    val publishedAtDisplay: String,
    val minSupportedVersionCode: Long,
    val changelog: List<String>,
    val downloadPageUrl: String,
    val downloadUrl: String?,
    val sha256: String?,
    val fileSizeBytes: Long?
)

// 远端 JSON 是不可信输入:任何字段缺失/类型错误/非法值都拒绝整个 Manifest(规范 §6.4)
class InvalidManifestException(reason: String) : Exception(reason)

object UpdateManifestParser {

    private const val MAX_TEXT_LENGTH = 300
    private const val MAX_CHANGELOG_LINES = 50

    fun parse(json: String, config: UpdateConfig): UpdateManifest {
        val o = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw InvalidManifestException("json_parse_failed")
        }

        // 契约版本必须为 1
        if (o.optInt("schemaVersion", -1) != 1) {
            throw InvalidManifestException("schema_version_unsupported")
        }

        // appId / channel / packageName 必须与本地配置完全一致
        val appId = o.optString("appId", "")
        if (appId != config.appId) throw InvalidManifestException("app_id_mismatch")
        val channel = o.optString("channel", "")
        if (channel != config.channel) throw InvalidManifestException("channel_mismatch")
        val packageName = o.optString("packageName", "")
        if (packageName != config.packageName) throw InvalidManifestException("package_name_mismatch")

        // versionCode:正整数,用于版本比较
        val versionCode = o.optLong("versionCode", -1L)
        if (versionCode <= 0) throw InvalidManifestException("version_code_invalid")

        // versionName / title:非空且长度受限
        val versionName = o.optString("versionName", "")
        if (versionName.isEmpty() || versionName.length > MAX_TEXT_LENGTH) {
            throw InvalidManifestException("version_name_invalid")
        }
        val title = o.optString("title", "")
        if (title.isEmpty() || title.length > MAX_TEXT_LENGTH) {
            throw InvalidManifestException("title_invalid")
        }

        // publishedAt:ISO 8601 且必须带时区信息
        val publishedAtRaw = o.optString("publishedAt", "")
        val publishedAtDisplay = try {
            val t = OffsetDateTime.parse(publishedAtRaw)
            t.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } catch (e: Exception) {
            throw InvalidManifestException("published_at_invalid")
        }

        // minSupportedVersionCode:1..versionCode
        val minSupported = o.optLong("minSupportedVersionCode", -1L)
        if (minSupported < 1 || minSupported > versionCode) {
            throw InvalidManifestException("min_supported_invalid")
        }

        // changelog:至少一条非空文本
        val changelogArr = o.optJSONArray("changelog")
            ?: throw InvalidManifestException("changelog_missing")
        if (changelogArr.length() < 1 || changelogArr.length() > MAX_CHANGELOG_LINES) {
            throw InvalidManifestException("changelog_invalid")
        }
        val changelog = mutableListOf<String>()
        for (i in 0 until changelogArr.length()) {
            val line = changelogArr.optString(i, "").trim()
            if (line.isEmpty() || line.length > MAX_TEXT_LENGTH) {
                throw InvalidManifestException("changelog_invalid")
            }
            changelog.add(line)
        }

        // downloadPageUrl:必须为合法 HTTPS URL(规范 §4.3)
        val downloadPageUrl = o.optString("downloadPageUrl", "")
        requireHttpsUrl(downloadPageUrl, "download_page_url_invalid")

        // downloadUrl:第一版可为 null;填写时必须 HTTPS
        val downloadUrl = if (o.isNull("downloadUrl")) null else o.optString("downloadUrl", null)
        if (downloadUrl != null) requireHttpsUrl(downloadUrl, "download_url_invalid")

        // sha256:可为 null;填写时必须是 64 位十六进制
        val sha256 = if (o.isNull("sha256")) null else o.optString("sha256", null)
        if (sha256 != null && !Regex("^[0-9a-fA-F]{64}$").matches(sha256)) {
            throw InvalidManifestException("sha256_invalid")
        }

        // fileSizeBytes:可为 null;填写时必须为正整数
        val fileSizeBytes = if (o.isNull("fileSizeBytes")) null else o.optLong("fileSizeBytes", -1L)
        if (fileSizeBytes != null && fileSizeBytes <= 0) {
            throw InvalidManifestException("file_size_invalid")
        }

        return UpdateManifest(
            appId = appId,
            packageName = packageName,
            channel = channel,
            versionCode = versionCode,
            versionName = versionName,
            title = title,
            publishedAtRaw = publishedAtRaw,
            publishedAtDisplay = publishedAtDisplay,
            minSupportedVersionCode = minSupported,
            changelog = changelog,
            downloadPageUrl = downloadPageUrl,
            downloadUrl = downloadUrl,
            sha256 = sha256,
            fileSizeBytes = fileSizeBytes
        )
    }

    private fun requireHttpsUrl(url: String, reason: String) {
        if (url.isEmpty() || url.length > MAX_TEXT_LENGTH) throw InvalidManifestException(reason)
        val uri = try {
            URI.create(url)
        } catch (e: Exception) {
            throw InvalidManifestException(reason)
        }
        if (uri.scheme != "https") throw InvalidManifestException(reason)
    }
}
