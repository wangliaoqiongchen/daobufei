package com.mo.daobufei

// VPN 当前状态的数据结构,Service 更新它,Fragment 读取它
data class VpnStatus(
    val isConnected: Boolean = false,
    val isPaused: Boolean = false,
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

    // 静态映射表代替 resources.getIdentifier:反射查找慢且依赖命名约定,这里改编译期确定
    fun getFlagDrawableResId(countryCode: String): Int {
        if (countryCode.length != 2) return 0
        return when (countryCode.uppercase()) {
            "AU" -> R.drawable.flag_au
            "CA" -> R.drawable.flag_ca
            "CH" -> R.drawable.flag_ch
            "DE" -> R.drawable.flag_de
            "FR" -> R.drawable.flag_fr
            "GB" -> R.drawable.flag_gb
            "HK" -> R.drawable.flag_hk
            "IN" -> R.drawable.flag_in
            "JP" -> R.drawable.flag_jp
            "KR" -> R.drawable.flag_kr
            "MO" -> R.drawable.flag_mo
            "NL" -> R.drawable.flag_nl
            "RU" -> R.drawable.flag_ru
            "SG" -> R.drawable.flag_sg
            "TW" -> R.drawable.flag_tw
            "US" -> R.drawable.flag_us
            else -> 0
        }
    }
}
