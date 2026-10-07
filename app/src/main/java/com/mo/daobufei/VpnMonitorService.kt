package com.mo.daobufei

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
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
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class VpnMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ---- 连接状态 ----
    private var lastVpnState = false
    private var latencyJob: Job? = null
    private var countryJob: Job? = null
    private var disconnectDebounceJob: Job? = null

    // 状态代数:每次连接/断开 +1,在途协程完成后比对,不一致就丢弃,
    // 解决"断开后在途国家查询返回,把 UI 翻回已连接"的竞态
    private var generation = 0

    // ---- 国家/出口信息 ----
    private var currentFlag = "🏳️"
    private var currentCountryName = "未知"
    private var currentCountryCode = ""
    private var lastSeenIp: String? = null          // 出口 IP 去重:IP 没变就不重复查/更新
    private var nextCountryAttemptAt = 0L           // 统一节流门:成功后 3s,失败后指数退避
    private var countryQueryFails = 0

    // ---- 延迟 ----
    private var latencyIntervalMs = 10_000L         // 息屏后调大到 30s 省电
    private var latestLatency: Long? = null         // 平滑后的显示值(median-3)
    private var latencyTimeout = false
    // 首个样本略偏高(TLS 握手),median-3 窗口会很快把它稀释掉,不做特殊丢弃
    private val lastThree = ArrayDeque<Long>()      // 平滑窗口
    private val recentSamples = ArrayDeque<Long>()  // 给 ProgressStyle 画最近 16 次历史

    // ---- 用户控制 ----
    private var monitoringPaused = false

    private val flagBitmapCache = HashMap<Int, Bitmap>()

    // 常驻 CPU 锁:灭屏/充电时设备会深度休眠,不持锁的话 delay() 定时不会唤醒 CPU,
    // 测量循环停摆(流体云冻住、后台不刷新)。配合用户的电池白名单保证后台常转
    private lateinit var wakeLock: android.os.PowerManager.WakeLock

    private val vpnCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = checkVpnStatus("onAvailable 触发")
        override fun onLost(network: Network) = checkVpnStatus("onLost 触发")
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            checkVpnStatus("onCapabilitiesChanged 触发")
    }

    // 息屏后流体云基本不看,降频测量省电;亮屏立即恢复
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> latencyIntervalMs = 30_000L
                Intent.ACTION_SCREEN_ON -> latencyIntervalMs = 10_000L
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        wakeLock = getSystemService(android.os.PowerManager::class.java)
            .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "daobufei:VpnMonitor")
            .apply {
                setReferenceCounted(false)
                acquire()
            }
        createNotificationChannel()

        startForeground(LIVE_UPDATE_NOTIFICATION_ID, buildIdleNotification())

        connectivityManager.registerDefaultNetworkCallback(vpnCallback)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        checkVpnStatus("Service 启动时检查")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REFRESH -> {
                // 通知上的"刷新":恢复监控并立即测一轮;国家查询走节流门,不放大请求量
                monitoringPaused = false
                checkVpnStatus("手动刷新")
                if (lastVpnState) {
                    fetchCountryInfo()
                    startLatencyLoop()
                }
            }
            ACTION_PAUSE -> {
                monitoringPaused = true
                stopLatencyLoop()
                renderNotification()
                publishUiState()
            }
            ACTION_RESUME -> {
                monitoringPaused = false
                if (lastVpnState) startLatencyLoop()
                renderNotification()
                publishUiState()
            }
            ACTION_EXIT -> {
                // 用户主动退出:连通知一起移除;下次打开 App 会重新启动
                _vpnStatus.value = VpnStatus(isConnected = false)
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_APPLY_IDLE_SETTING -> {
                // 用户切了"断开时显示待机通知"开关:立即按新配置重发,不等下次状态变化
                if (!lastVpnState) renderNotification()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        connectivityManager.unregisterNetworkCallback(vpnCallback)
        unregisterReceiver(screenReceiver)
        stopLatencyLoop()
        countryJob?.cancel()
        disconnectDebounceJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ==================== VPN 状态检测 ====================

    private fun checkVpnStatus(trigger: String) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isVpnActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        Log.d(TAG, "[$trigger] VPN active = $isVpnActive")

        if (isVpnActive && lastVpnState) {
            // 连接中的网络参数变化:可能节点/出口变了,节流地重查一次国家
            fetchCountryInfo()
            return
        }
        if (!isVpnActive && !lastVpnState) return

        if (isVpnActive) {
            disconnectDebounceJob?.cancel()
            onVpnConnected()
        } else {
            // 断开做 2 秒防抖:网络切换瞬间 activeNetwork 可能短暂无 VPN transport,
            // 立即重置会让流体云"闪一下"又恢复;防抖期间新事件会不断顺延确认
            disconnectDebounceJob?.cancel()
            disconnectDebounceJob = serviceScope.launch {
                delay(DISCONNECT_CONFIRM_MS)
                val recheck = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (recheck?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) != true) {
                    onVpnDisconnected()
                } else {
                    Log.d(TAG, "断开防抖复核:VPN 仍在,忽略本次抖动")
                }
            }
        }
    }

    private fun onVpnConnected() {
        lastVpnState = true
        generation++
        monitoringPaused = false
        latencyTimeout = false
        latestLatency = null
        lastThree.clear()
        recentSamples.clear()
        countryQueryFails = 0
        nextCountryAttemptAt = 0L
        Log.d(TAG, "VPN 已连接,开始监控")
        fetchCountryInfo()
        startLatencyLoop()
        publishUiState()
    }

    private fun onVpnDisconnected() {
        lastVpnState = false
        generation++
        stopLatencyLoop()
        countryJob?.cancel()
        lastSeenIp = null
        lastThree.clear()
        recentSamples.clear()
        _latencyHistory.value = emptyList()
        latencyIntervalMs = 10_000L
        renderNotification()
        _vpnStatus.value = VpnStatus(isConnected = false)
        Log.d(TAG, "VPN 已断开,流体云已重置")
    }

    // ==================== 国家查询 ====================

    private fun fetchCountryInfo() {
        val now = System.currentTimeMillis()
        if (now < nextCountryAttemptAt) return
        nextCountryAttemptAt = now + COUNTRY_MIN_INTERVAL_MS

        val gen = generation
        countryJob?.cancel()
        countryJob = serviceScope.launch {
            val result = withContext(Dispatchers.IO) { queryCountryInfo() }
            // 竞态防护:断开后 generation 已变,在途结果一律丢弃,不把状态翻回"已连接"
            if (gen != generation || !lastVpnState) return@launch

            if (result == null) {
                countryQueryFails++
                val backoff = minOf(
                    COUNTRY_MIN_INTERVAL_MS * (1L shl (countryQueryFails - 1).coerceIn(0, 2)),
                    COUNTRY_BACKOFF_MAX_MS
                )
                nextCountryAttemptAt = System.currentTimeMillis() + backoff
                Log.d(TAG, "国家查询失败(连续 $countryQueryFails 次),退避 ${backoff}ms")
                return@launch
            }
            countryQueryFails = 0

            val (ip, code) = result
            if (ip == lastSeenIp && currentCountryCode.isNotEmpty()) return@launch
            lastSeenIp = ip
            currentCountryCode = code
            currentCountryName = localeCountryName(code)
            currentFlag = FlagUtils.countryCodeToFlag(code)
            Log.d(TAG, "国家: $currentCountryName ($code) $currentFlag")

            renderNotification()
            publishUiState()
        }
    }

    // 依次尝试多个免费 HTTPS 源,任一成功即返回 (出口IP, 国家码);
    // 全部支持 HTTPS,原来的 http://ip-api.com 已废弃(明文可被中间人看/改)
    private fun queryCountryInfo(): Pair<String, String>? {
        return queryIpWhoIs() ?: queryCountryIs() ?: queryCloudflareTrace()
    }

    private fun queryIpWhoIs(): Pair<String, String>? {
        return try {
            val body = httpGet("https://ipwho.is/") ?: return null
            val json = JSONObject(body)
            val code = json.optString("country_code")
            val ip = json.optString("ip")
            if (json.optBoolean("success", true) && code.length == 2 && ip.isNotEmpty()) {
                ip to code.uppercase(Locale.ROOT)
            } else null
        } catch (e: IOException) {
            Log.d(TAG, "ipwho.is 查询失败: ${e.message}")
            null
        } catch (e: Exception) {
            Log.d(TAG, "ipwho.is 解析失败: ${e.message}")
            null
        }
    }

    private fun queryCountryIs(): Pair<String, String>? {
        return try {
            val body = httpGet("https://api.country.is/") ?: return null
            val json = JSONObject(body)
            val code = json.optString("country")
            val ip = json.optString("ip")
            if (code.length == 2 && ip.isNotEmpty()) ip to code.uppercase(Locale.ROOT) else null
        } catch (e: IOException) {
            Log.d(TAG, "country.is 查询失败: ${e.message}")
            null
        } catch (e: Exception) {
            Log.d(TAG, "country.is 解析失败: ${e.message}")
            null
        }
    }

    private fun queryCloudflareTrace(): Pair<String, String>? {
        return try {
            val body = httpGet("https://www.cloudflare.com/cdn-cgi/trace") ?: return null
            val fields = body.lineSequence()
                .mapNotNull { line ->
                    val i = line.indexOf('=')
                    if (i > 0) line.substring(0, i) to line.substring(i + 1) else null
                }
                .toMap()
            val code = fields["loc"]
            val ip = fields["ip"]
            if (code != null && code.length == 2 && !ip.isNullOrEmpty()) {
                ip to code.uppercase(Locale.ROOT)
            } else null
        } catch (e: IOException) {
            Log.d(TAG, "cloudflare trace 查询失败: ${e.message}")
            null
        } catch (e: Exception) {
            Log.d(TAG, "cloudflare trace 解析失败: ${e.message}")
            null
        }
    }

    private fun httpGet(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        } catch (e: IOException) {
            Log.d(TAG, "HTTP 请求失败 $url: ${e.message}")
            null
        }
    }

    // 用设备语言显示国家名(中文系统显示"美国"而不是"United States")
    private fun localeCountryName(code: String): String {
        if (code.length != 2) return "未知"
        val name = Locale("", code).displayCountry
        return if (name.isNotEmpty() && name != code) name else code
    }

    // ==================== 延迟测量 ====================

    private fun startLatencyLoop() {
        stopLatencyLoop()
        latencyJob = serviceScope.launch {
            var tick = 0
            while (isActive) {
                tick++
                if (tick % 3 == 0) fetchCountryInfo()

                val gen = generation
                val raw = withContext(Dispatchers.IO) { measureLatency() }
                if (gen != generation) return@launch   // 断开了:丢弃,别再写状态/通知
                if (raw != null) {
                    pushLatencySample(raw)
                    Log.d(TAG, "延迟: ${raw}ms (显示 ${latestLatency}ms)")
                    latencyTimeout = false
                } else {
                    Log.d(TAG, "延迟测量失败")
                    latencyTimeout = true
                }
                renderNotification()
                publishUiState()
                delay(latencyIntervalMs)
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
            Log.e(TAG, "延迟测量出错: ${e.message}")
            null
        }
    }

    private fun pushLatencySample(sample: Long) {
        lastThree.addLast(sample)
        if (lastThree.size > 3) lastThree.removeFirst()
        recentSamples.addLast(sample)
        if (recentSamples.size > RECENT_SAMPLE_MAX) recentSamples.removeFirst()
        // median-3 平滑:单个毛刺不再让流体云数字突兀跳动
        latestLatency = if (lastThree.size == 3) lastThree.sorted()[1] else sample
        _latencyHistory.value = recentSamples.toList()
    }

    // ==================== 状态发布(UI) ====================

    private fun publishUiState() {
        _vpnStatus.value = _vpnStatus.value.copy(
            isConnected = lastVpnState,
            isPaused = monitoringPaused,
            countryCode = currentCountryCode,
            countryName = currentCountryName,
            flag = currentFlag,
            latencyMs = latestLatency,
            isTimeout = latencyTimeout
        )
    }

    // ==================== 通知渲染(统一出口) ====================

    // 真机实测:ColorOS 流体云 chip 的内容不会随原地 notify() 刷新,会冻结在首次
    // promoted 时的文字。所以每次渲染都先 cancel 再 notify,强制 chip 重建,
    // 保证延迟数字/超时/断开消失都能实时反映;副作用是重复弹横幅,所以
    // 连接/超时/暂停三个形态都 setSilent(true) 压掉声音和横幅
    private fun renderNotification() {
        val notification = when {
            !lastVpnState -> buildIdleNotification()
            monitoringPaused -> buildPausedNotification()
            latencyTimeout -> buildTimeoutNotification()
            else -> buildConnectedNotification()
        }
        val nm = NotificationManagerCompat.from(this)
        nm.cancel(LIVE_UPDATE_NOTIFICATION_ID)
        nm.notify(LIVE_UPDATE_NOTIFICATION_ID, notification)
    }

    private fun buildIdleNotification(): android.app.Notification {
        val showNotification = SettingsPrefs.isIdleNotificationEnabled(this)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_app_running))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // 关闭开关时用最低优先级,通知栏静默存在,不弹横幅、不提示音;开启时保持可见
            .setPriority(if (showNotification) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MIN)
            .setSilent(!showNotification)
            .setContentIntent(buildContentIntent())
            .build()
    }

    private fun buildPausedNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_paused))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(getString(R.string.notif_paused))
            .setContentIntent(buildContentIntent())
            .addAction(buildAction(ACTION_RESUME, R.drawable.ic_play, getString(R.string.notif_action_resume), REQ_RESUME))
            .addAction(buildAction(ACTION_EXIT, R.drawable.ic_close, getString(R.string.notif_action_exit), REQ_EXIT))
            .build()
    }

    private fun buildConnectedNotification(): android.app.Notification {
        val flagResId = FlagUtils.getFlagDrawableResId(currentCountryCode)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (flagResId != 0) flagResId else R.drawable.ic_notification)
            .setContentTitle("$currentFlag $currentCountryName")
            .setContentText("延迟 ${latestLatency ?: "--"}ms")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setRequestPromotedOngoing(true)
            // ColorOS 流体云 chip 右侧的文字就是取这个字段(v1 实测),延迟必须放这里
            .setShortCriticalText(latestLatency?.let { getString(R.string.notif_latency, it) } ?: "...")
            .setContentIntent(buildContentIntent())
            .addAction(buildAction(ACTION_REFRESH, R.drawable.ic_refresh, getString(R.string.notif_action_refresh), REQ_REFRESH))
            .addAction(buildAction(ACTION_PAUSE, R.drawable.ic_pause, getString(R.string.notif_action_pause), REQ_PAUSE))
            .addAction(buildAction(ACTION_EXIT, R.drawable.ic_close, getString(R.string.notif_action_exit), REQ_EXIT))

        if (flagResId != 0) {
            builder.setLargeIcon(getFlagBitmap(flagResId))
        }
        // Android 16 ProgressStyle:把最近 16 次延迟按绿/橙/红画成历史条,
        // 在流体云展开态/通知抽屉展开可见;构建失败只影响这一处视觉,不影响状态
        buildProgressStyleOrNull()?.let { builder.setStyle(it) }
        return builder.build()
    }

    // 超时单独一个形态:chip 显示"超时",不带历史条,一眼区分"数字差"和"不通了"
    private fun buildTimeoutNotification(): android.app.Notification {
        val flagResId = FlagUtils.getFlagDrawableResId(currentCountryCode)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (flagResId != 0) flagResId else R.drawable.ic_notification)
            .setContentTitle("$currentFlag $currentCountryName")
            .setContentText(getString(R.string.notif_timeout_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(getString(R.string.notif_timeout_chip))
            .setContentIntent(buildContentIntent())
            .addAction(buildAction(ACTION_REFRESH, R.drawable.ic_refresh, getString(R.string.notif_action_refresh), REQ_REFRESH))
            .addAction(buildAction(ACTION_PAUSE, R.drawable.ic_pause, getString(R.string.notif_action_pause), REQ_PAUSE))
            .addAction(buildAction(ACTION_EXIT, R.drawable.ic_close, getString(R.string.notif_action_exit), REQ_EXIT))

        if (flagResId != 0) {
            builder.setLargeIcon(getFlagBitmap(flagResId))
        }
        return builder.build()
    }

    private fun buildProgressStyleOrNull(): NotificationCompat.ProgressStyle? {
        // ColorOS 实测:分段超过 ~10 个渲染异常(变长白条),压到 8 段以内
        val samples = recentSamples.takeLast(PROGRESS_BAR_MAX)
        if (samples.size < 2) return null
        return try {
            val good = color(R.color.status_good)
            val warn = color(R.color.status_warn)
            val bad = color(R.color.status_bad)
            val segments = samples.map { sample ->
                // androidx 版 Segment 只有 (length) 一个构造,颜色用 setColor 单独设置
                val segment = NotificationCompat.ProgressStyle.Segment(1)
                segment.setColor(
                    when {
                        sample < 100 -> good
                        sample < 300 -> warn
                        else -> bad
                    }
                )
                segment
            }
            NotificationCompat.ProgressStyle()
                .setProgress(samples.size)
                .setProgressSegments(segments)
        } catch (t: Throwable) {
            Log.w(TAG, "ProgressStyle 构建失败: ${t.message}")
            null
        }
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildAction(action: String, iconRes: Int, label: String, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(this, VpnMonitorService::class.java).setAction(action)
        val pendingIntent = PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action(iconRes, label, pendingIntent)
    }

    private fun getFlagBitmap(resId: Int): Bitmap =
        flagBitmapCache.getOrPut(resId) { BitmapFactory.decodeResource(resources, resId) }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.channel_vpn_name), NotificationManager.IMPORTANCE_HIGH
        ).apply { description = getString(R.string.channel_vpn_desc) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "VpnCheck"
        private const val CHANNEL_ID = "vpn_live_update_channel"
        private const val LIVE_UPDATE_NOTIFICATION_ID = 1001
        private const val DISCONNECT_CONFIRM_MS = 2_000L
        private const val COUNTRY_MIN_INTERVAL_MS = 3_000L
        private const val COUNTRY_BACKOFF_MAX_MS = 120_000L
        private const val RECENT_SAMPLE_MAX = 16
        private const val PROGRESS_BAR_MAX = 8

        private const val ACTION_REFRESH = "com.mo.daobufei.action.REFRESH"
        // 暂停/继续用两个显式动作而不是一个 toggle:流体云和抽屉两层视图可能重复投递,
        // 幂等的显式动作保证"继续"永远恢复、"暂停"永远暂停
        private const val ACTION_PAUSE = "com.mo.daobufei.action.PAUSE"
        private const val ACTION_RESUME = "com.mo.daobufei.action.RESUME"
        private const val ACTION_EXIT = "com.mo.daobufei.action.EXIT"
        // 需要被其它组件(设置开关)发起,所以公开
        const val ACTION_APPLY_IDLE_SETTING = "com.mo.daobufei.action.APPLY_IDLE_SETTING"

        private const val REQ_REFRESH = 1
        private const val REQ_PAUSE = 2
        private const val REQ_EXIT = 3
        private const val REQ_RESUME = 4

        // 界面通过这个只读的 StateFlow 观察最新状态,Service 内部通过 _vpnStatus 更新
        private val _vpnStatus = MutableStateFlow(VpnStatus())
        val vpnStatus: StateFlow<VpnStatus> = _vpnStatus

        // 最近 16 次延迟采样,给首页趋势图用
        private val _latencyHistory = MutableStateFlow<List<Long>>(emptyList())
        val latencyHistory: StateFlow<List<Long>> = _latencyHistory
    }
}
