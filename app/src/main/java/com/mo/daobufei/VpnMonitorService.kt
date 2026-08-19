package com.mo.daobufei

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class VpnMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastVpnState = false
    private var latencyJob: Job? = null
    private var currentFlag = "🏳️"
    private var currentCountryName = "未知"
    private var currentCountryCode = ""
    private var lastCountryFetchTime = 0L

    private val vpnCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = checkVpnStatus("onAvailable 触发")
        override fun onLost(network: Network) = checkVpnStatus("onLost 触发")
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            checkVpnStatus("onCapabilitiesChanged 触发")
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        createNotificationChannel()

        startForeground(LIVE_UPDATE_NOTIFICATION_ID, buildIdleNotification())

        connectivityManager.registerDefaultNetworkCallback(vpnCallback)
        checkVpnStatus("Service 启动时检查")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(vpnCallback)
        latencyJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "VPN 流体云状态", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "VPN 连接期间显示国家和延迟" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildIdleNotification(): android.app.Notification {
        val showNotification = SettingsPrefs.isIdleNotificationEnabled(this)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("岛不废")
            .setContentText("等待 VPN 连接...")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // 关闭开关时用最低优先级,通知栏静默存在,不弹横幅、不提示音;开启时保持可见
            .setPriority(if (showNotification) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MIN)
            .setSilent(!showNotification)
            .build()
    }

    private fun checkVpnStatus(trigger: String) {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isVpnActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        Log.d("VpnCheck", "[$trigger] VPN active = $isVpnActive")

        if (isVpnActive != lastVpnState) {
            lastVpnState = isVpnActive
            if (isVpnActive) {
                fetchCountryInfo()
                startLatencyLoop()
            } else {
                stopLatencyLoop()
                NotificationManagerCompat.from(this)
                    .notify(LIVE_UPDATE_NOTIFICATION_ID, buildIdleNotification())
                _vpnStatus.value = VpnStatus(isConnected = false)
                Log.d("VpnCheck", "VPN 已断开,流体云已重置")
            }
        } else if (isVpnActive) {
            maybeFetchCountryInfo()
        }
    }

    private fun maybeFetchCountryInfo() {
        val now = System.currentTimeMillis()
        if (now - lastCountryFetchTime > 3000) {
            fetchCountryInfo()
        }
    }

    private fun fetchCountryInfo() {
        lastCountryFetchTime = System.currentTimeMillis()
        serviceScope.launch {
            val result = withContext(Dispatchers.IO) { queryIpApi() }
            if (result != null) {
                val (countryCode, countryName, _) = result
                val changed = countryCode != currentCountryCode
                currentFlag = FlagUtils.countryCodeToFlag(countryCode)
                currentCountryName = countryName
                currentCountryCode = countryCode
                Log.d("VpnCheck", "国家: $countryName ($countryCode) $currentFlag ${if (changed) "(节点已切换)" else ""}")

                _vpnStatus.value = _vpnStatus.value.copy(
                    isConnected = true,
                    countryCode = currentCountryCode,
                    countryName = currentCountryName,
                    flag = currentFlag,
                    isTimeout = false
                )
            } else {
                Log.d("VpnCheck", "查询国家信息失败")
            }
        }
    }

    private fun queryIpApi(): Triple<String, String, String>? {
        return try {
            val request = Request.Builder()
                .url("http://ip-api.com/json/?fields=status,country,countryCode,query")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                if (json.optString("status") != "success") return null
                Triple(
                    json.optString("countryCode"),
                    json.optString("country"),
                    json.optString("query")
                )
            }
        } catch (e: IOException) {
            Log.e("VpnCheck", "网络请求失败: ${e.message}")
            null
        }
    }

    private fun startLatencyLoop() {
        stopLatencyLoop()
        var loopCount = 0
        latencyJob = serviceScope.launch {
            while (isActive) {
                loopCount++
                if (loopCount % 3 == 0) {
                    fetchCountryInfo()
                }

                val latency = withContext(Dispatchers.IO) { measureLatency() }
                if (latency != null) {
                    Log.d("VpnCheck", "延迟: ${latency}ms")
                    showConnectedState(latency)
                } else {
                    Log.d("VpnCheck", "延迟测量失败")
                    showTimeoutState()
                }
                delay(10_000)
            }
        }
    }

    private fun stopLatencyLoop() {
        latencyJob?.cancel()
        latencyJob = null
    }

    private fun measureLatency(): Long? {
        return try {
            val request = Request.Builder().url("https://www.gstatic.com/generate_204").build()
            val start = System.currentTimeMillis()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
            }
            System.currentTimeMillis() - start
        } catch (e: IOException) {
            Log.e("VpnCheck", "延迟测量出错: ${e.message}")
            null
        }
    }

    private fun showConnectedState(latencyMs: Long) {
        val flagResId = FlagUtils.getFlagDrawableResId(this, currentCountryCode)
        val smallIconRes =
            if (flagResId != 0) flagResId else android.R.drawable.stat_sys_download_done

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle("$currentFlag $currentCountryName")
            .setContentText("延迟 ${latencyMs}ms")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText("${latencyMs}ms")

        if (flagResId != 0) {
            builder.setLargeIcon(BitmapFactory.decodeResource(resources, flagResId))
        }

        NotificationManagerCompat.from(this).notify(LIVE_UPDATE_NOTIFICATION_ID, builder.build())

        _vpnStatus.value = _vpnStatus.value.copy(
            isConnected = true,
            latencyMs = latencyMs,
            isTimeout = false
        )
    }

    private fun showTimeoutState() {
        val flagResId = FlagUtils.getFlagDrawableResId(this, currentCountryCode)
        val smallIconRes =
            if (flagResId != 0) flagResId else android.R.drawable.stat_sys_download_done

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle("$currentFlag $currentCountryName")
            .setContentText("延迟超时,节点可能不可用")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText("Timeout")

        if (flagResId != 0) {
            builder.setLargeIcon(BitmapFactory.decodeResource(resources, flagResId))
        }

        NotificationManagerCompat.from(this).notify(LIVE_UPDATE_NOTIFICATION_ID, builder.build())

        _vpnStatus.value = _vpnStatus.value.copy(
            isConnected = true,
            latencyMs = null,
            isTimeout = true
        )
    }

    companion object {
        private const val CHANNEL_ID = "vpn_live_update_channel"
        private const val LIVE_UPDATE_NOTIFICATION_ID = 1001

        // 界面通过这个只读的 StateFlow 观察最新状态,Service 内部通过 _vpnStatus 更新
        private val _vpnStatus = MutableStateFlow(VpnStatus())
        val vpnStatus: StateFlow<VpnStatus> = _vpnStatus
    }
}