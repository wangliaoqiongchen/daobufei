package com.mo.daobufei.update

// 检测调度与版本判断的纯函数(规范 §6.3/§6.4),可单元测试
object UpdateGate {

    const val TWELVE_HOURS_MS = 12L * 60 * 60 * 1000
    const val RETRY_THROTTLE_MS = 15L * 60 * 1000

    // 自动检测触发条件(规范 §6.3):
    // 今天的本地日期 != 最近成功日期 OR 距最近成功检测满 12 小时
    // 时间回拨(差值为负)也算需要检测,避免永不检测
    fun shouldAutoCheck(
        nowMs: Long,
        lastSuccessfulCheckAt: Long,
        lastSuccessfulCheckLocalDate: String,
        lastAttemptAt: Long,
        todayLocalDate: String
    ): Boolean {
        val noSuccessRecord = lastSuccessfulCheckAt == 0L || lastSuccessfulCheckLocalDate.isEmpty()
        val dueDaily = lastSuccessfulCheckLocalDate != todayLocalDate
        val diff = nowMs - lastSuccessfulCheckAt
        val dueTwelveHours = noSuccessRecord || diff >= TWELVE_HOURS_MS || diff < 0
        if (!dueDaily && !dueTwelveHours) return false

        // 失败重试节流:最近一次尝试晚于最近一次成功(即那次尝试失败了)
        // 且距今不足 15 分钟时,自动检测暂缓(手动检测不受限)
        val sinceAttempt = nowMs - lastAttemptAt
        val failedAttemptRecent = lastAttemptAt != 0L &&
            sinceAttempt >= 0 &&
            sinceAttempt < RETRY_THROTTLE_MS &&
            lastAttemptAt > lastSuccessfulCheckAt
        if (failedAttemptRecent) return false

        return true
    }

    // 版本判断结果
    enum class Verdict { UP_TO_DATE, NEW_UPDATE, FORCED_UPDATE }

    // 以整数 versionCode 判断(规范 §6.4);低于 minSupportedVersionCode 进入强制更新
    fun evaluate(localVersionCode: Long, manifest: UpdateManifest): Verdict = when {
        localVersionCode < manifest.minSupportedVersionCode -> Verdict.FORCED_UPDATE
        manifest.versionCode > localVersionCode -> Verdict.NEW_UPDATE
        else -> Verdict.UP_TO_DATE
    }
}
