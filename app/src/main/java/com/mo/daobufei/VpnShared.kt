package com.mo.daobufei

// VPN 当前状态的数据结构,Service 更新它,Fragment 读取它
data class VpnStatus(
    val isConnected: Boolean = false,
    val countryCode: String = "",
    val countryName: String = "未知",
    val flag: String = "🏳️",
    val latencyMs: Long? = null,
    val isTimeout: Boolean = false
)

// 国旗相关的公共工具方法,Service 和 Fragment 共用,避免写两份
object FlagUtils {

    fun countryCodeToFlag(countryCode: String): String {
        if (countryCode.length != 2) return "🏳️"
        val base = 0x1F1E6
        val first = Character.toUpperCase(countryCode[0]) - 'A' + base
        val second = Character.toUpperCase(countryCode[1]) - 'A' + base
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    // 根据国家代码找 drawable 里对应的 flag_xx 图片,找不到返回 0
    fun getFlagDrawableResId(context: android.content.Context, countryCode: String): Int {
        val resName = "flag_${countryCode.lowercase()}"
        return context.resources.getIdentifier(resName, "drawable", context.packageName)
    }
}