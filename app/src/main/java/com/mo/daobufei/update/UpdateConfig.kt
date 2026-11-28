package com.mo.daobufei.update

// 更新模块配置:每个接入的 App 提供自己的 slug/包名/通道/服务地址
// (规范 §6.2:不允许把某一个软件的值写死成所有软件共用)
data class UpdateConfig(
    val appId: String = "daobufei",
    val packageName: String = "com.mo.daobufei",
    val channel: String = "stable",
    val baseUrl: String = "https://updates.855886.xyz"
) {
    companion object {
        // 契约 §4.1:appId 只允许小写字母、数字和连字符
        private val APP_ID_REGEX = Regex("^[a-z0-9][a-z0-9-]{0,49}$")

        fun isValidAppId(appId: String): Boolean = APP_ID_REGEX.matches(appId)
        fun isValidChannel(channel: String): Boolean = channel == "stable" || channel == "beta"
    }

    // 最终 URL:{baseUrl}/v1/apps/{appId}/{channel}.json(规范 §4.1)
    fun manifestUrl(): String {
        require(isValidAppId(appId)) { "invalid appId: $appId" }
        require(isValidChannel(channel)) { "invalid channel: $channel" }
        val base = baseUrl.trimEnd('/')
        return "$base/v1/apps/$appId/$channel.json"
    }
}
