package com.mo.daobufei.update

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.util.concurrent.TimeUnit

// 检测结果
sealed class UpdateResult {
    object NotDue : UpdateResult()          // 自动检测条件未满足(仅自动路径返回)
    object InFlight : UpdateResult()        // 已有请求执行中
    data class NewUpdate(val manifest: UpdateManifest) : UpdateResult()
    object UpToDate : UpdateResult()
    data class ForcedUpdate(val manifest: UpdateManifest) : UpdateResult()
    object NotFound : UpdateResult()        // 项目/通道不存在(如 beta 未发布),区别于网络错误
    data class ServerError(val code: Int) : UpdateResult()
    object NetworkError : UpdateResult()
    data class InvalidConfig(val reason: String) : UpdateResult()
}

// 内部信号:fetch 阶段用异常表达 404/5xx,由 doCheck 统一翻译成结果
private object NotFoundSignal : Exception()
private class ServerErrorSignal(val code: Int) : Exception()

// 更新检测器:网络请求、Manifest 解析校验、检测调度状态、单请求互斥
// 网络请求在 IO 线程执行(规范 §6.3);同一时刻只允许一个检测请求
class UpdateChecker private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val config = UpdateConfig()
    private val prefs = UpdatePrefs(appContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val mutex = Mutex()

    companion object {
        @Volatile
        private var instance: UpdateChecker? = null

        // 进程级单例:手动检测与自动检测共享同一互斥与状态
        fun get(context: Context): UpdateChecker =
            instance ?: synchronized(this) {
                instance ?: UpdateChecker(context).also { instance = it }
            }
    }

    // 自动检测:条件未满足返回 NotDue;满足则发起请求(失败有 15 分钟节流)
    suspend fun autoCheckIfNeeded(): UpdateResult = mutex.withLock {
        val now = System.currentTimeMillis()
        if (!UpdateGate.shouldAutoCheck(
                nowMs = now,
                lastSuccessfulCheckAt = prefs.lastSuccessfulCheckAt,
                lastSuccessfulCheckLocalDate = prefs.lastSuccessfulCheckLocalDate,
                lastAttemptAt = prefs.lastAttemptAt,
                todayLocalDate = LocalDate.now().toString()
            )
        ) {
            UpdateResult.NotDue
        } else {
            doCheck()
        }
    }

    // 手动检测:不受"当天是否检测过"、"12 小时是否到期"和失败节流限制(规范 §6.3)
    suspend fun manualCheck(): UpdateResult = mutex.withLock {
        doCheck()
    }

    private suspend fun doCheck(): UpdateResult {
        // lastAttemptAt 在请求开始时写入,防止短时间并发失败请求(规范 §6.3)
        prefs.lastAttemptAt = System.currentTimeMillis()
        val manifest = try {
            fetchAndParse()
        } catch (e: NotFoundSignal) {
            return UpdateResult.NotFound
        } catch (e: ServerErrorSignal) {
            return UpdateResult.ServerError(e.code)
        } catch (e: InvalidManifestException) {
            return UpdateResult.InvalidConfig(e.message ?: "invalid_manifest")
        } catch (e: IOException) {
            return UpdateResult.NetworkError
        }

        // 只有成功获取并通过全部校验才刷新成功时间和成功日期(规范 §6.3)
        prefs.lastSuccessfulCheckAt = System.currentTimeMillis()
        prefs.lastSuccessfulCheckLocalDate = LocalDate.now().toString()

        return when (UpdateGate.evaluate(localVersionCode(), manifest)) {
            UpdateGate.Verdict.UP_TO_DATE -> UpdateResult.UpToDate
            UpdateGate.Verdict.NEW_UPDATE -> {
                // 用户已选择"不再提醒此版本":该版本号及以下不再弹窗(更高版本仍会提示)
                if (manifest.versionCode <= prefs.ignoredVersionCode) {
                    UpdateResult.UpToDate
                } else {
                    UpdateResult.NewUpdate(manifest)
                }
            }
            UpdateGate.Verdict.FORCED_UPDATE -> UpdateResult.ForcedUpdate(manifest)
        }
    }

    private suspend fun fetchAndParse(): UpdateManifest = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(config.manifestUrl())
            .build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: IOException) {
            throw IOException("network_error", e)
        }
        try {
            when (response.code) {
                404 -> throw NotFoundSignal
                !in 200..299 -> throw ServerErrorSignal(response.code)
            }
            val body = response.body?.string() ?: throw NotFoundSignal
            val manifest = UpdateManifestParser.parse(body, config)
            // 持久化最近一次有效 Manifest 原文:强更门禁独立于检测调度时要用它
            prefs.lastKnownManifestJson = body
            manifest
        } finally {
            response.close()
        }
    }

    private fun localVersionCode(): Long = try {
        appContext.packageManager.getPackageInfo(config.packageName, 0).longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        -1L
    }
}
