package com.mo.daobufei.update

import android.content.Context

// 更新检测状态(规范 §6.3 建议的三个字段):
// lastSuccessfulCheckAt / lastSuccessfulCheckLocalDate / lastAttemptAt 严格区分
class UpdatePrefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("update_check", Context.MODE_PRIVATE)

    var lastSuccessfulCheckAt: Long
        get() = sp.getLong(KEY_SUCCESS_AT, 0L)
        set(value) = sp.edit().putLong(KEY_SUCCESS_AT, value).apply()

    var lastSuccessfulCheckLocalDate: String
        get() = sp.getString(KEY_SUCCESS_DATE, "") ?: ""
        set(value) = sp.edit().putString(KEY_SUCCESS_DATE, value).apply()

    var lastAttemptAt: Long
        get() = sp.getLong(KEY_ATTEMPT_AT, 0L)
        set(value) = sp.edit().putLong(KEY_ATTEMPT_AT, value).apply()

    // 最近一次通过全部校验的 Manifest 原文(强更门禁用:每次进入前台都能按它判定)
    var lastKnownManifestJson: String
        get() = sp.getString(KEY_LAST_MANIFEST, "") ?: ""
        set(value) = sp.edit().putString(KEY_LAST_MANIFEST, value).apply()

    // 用户选择"不再提醒"的版本号(0 = 未忽略;低于等于它的更新不再弹窗)
    var ignoredVersionCode: Long
        get() = sp.getLong(KEY_IGNORED_VC, 0L)
        set(value) = sp.edit().putLong(KEY_IGNORED_VC, value).apply()

    // 上次安装时记录的 versionCode:安装新版本后首次进入时据此弹"更新成功"
    var lastSeenInstalledVersionCode: Long
        get() = sp.getLong(KEY_LAST_SEEN_VC, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_SEEN_VC, value).apply()

    // 从持久化的 Manifest 原文恢复数据模型(解析失败返回 null,不抛异常)
    fun lastKnownManifest(config: UpdateConfig): UpdateManifest? =
        lastKnownManifestJson.takeIf { it.isNotEmpty() }?.let {
            runCatching { UpdateManifestParser.parse(it, config) }.getOrNull()
        }

    private companion object {
        const val KEY_SUCCESS_AT = "lastSuccessfulCheckAt"
        const val KEY_SUCCESS_DATE = "lastSuccessfulCheckLocalDate"
        const val KEY_ATTEMPT_AT = "lastAttemptAt"
        const val KEY_LAST_MANIFEST = "lastKnownManifestJson"
        const val KEY_IGNORED_VC = "ignoredVersionCode"
        const val KEY_LAST_SEEN_VC = "lastSeenInstalledVersionCode"
    }
}
