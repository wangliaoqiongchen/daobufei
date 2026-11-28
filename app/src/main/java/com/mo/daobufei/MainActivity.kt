package com.mo.daobufei

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.mo.daobufei.update.UpdateChecker
import com.mo.daobufei.update.UpdateDialogs
import com.mo.daobufei.update.UpdateResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("VpnCheck", "通知权限: $granted")
            if (granted) startMonitorService()
        }

    private val selectedTab = mutableIntStateOf(0)
    private val isScrolling = mutableStateOf(false)
    private val scrollStopHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var activeFragment: Fragment? = null
    private lateinit var container: FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 深色模式偏好(更多页可改):先应用再创建视图,颜色 token 自动跟随
        when (SettingsPrefs.nightMode(this)) {
            SettingsPrefs.NIGHT_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            SettingsPrefs.NIGHT_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // Android 15+ 强制 edge-to-edge,内容会延伸到状态栏/导航栏下面
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 状态栏图标颜色跟随深浅色主题(浅色模式用深色图标)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_YES

        setContentView(R.layout.activity_main)
        container = findViewById(R.id.fragmentContainer)

        // 玻璃层(底栏+顶条)挂为 fragment 容器的兄弟节点;
        // 容器在 onCreate 时已在真实层级里(onStart 前),FragmentManager 找得到,
        // 首帧时再由 AndroidView 把容器重定父级进 Compose 树,作为 backdrop 源
        val glassLayer = ComposeView(this).apply {
            setContent {
                AppRoot(
                    container = container,
                    isScrolling = isScrolling.value,
                    selected = selectedTab.intValue,
                    onSelected = { index -> onTabSelected(index) }
                )
            }
        }
        addContentView(
            glassLayer,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 键盘弹出时避让(fragment 内容自己处理状态栏 inset)
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(bottom = ime.bottom)
            insets
        }

        // 滚动状态驱动顶部磨砂条的模糊淡入淡出
        container.viewTreeObserver.addOnScrollChangedListener {
            // 滚动中:置为滑动状态;180ms 无滚动事件视为停止
            isScrolling.value = true
            scrollStopHandler.removeCallbacksAndMessages(null)
            scrollStopHandler.postDelayed({ isScrolling.value = false }, 180)
        }

        // 自动检测更新(规范 §6.3):RESUMED 期间每分钟评估一次条件
        // (每天首次进入 / 距上次成功满 12 小时),条件满足才真正发请求;
        // 页面进入后台时 repeatOnLifecycle 自动取消计时,回前台自动恢复
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    val result = UpdateChecker.get(applicationContext).autoCheckIfNeeded()
                    when (result) {
                        is UpdateResult.NewUpdate ->
                            UpdateDialogs.showUpdateDialog(this@MainActivity, result.manifest, forced = false)
                        is UpdateResult.ForcedUpdate ->
                            UpdateDialogs.showUpdateDialog(this@MainActivity, result.manifest, forced = true)
                        else -> { /* 未满足条件/无网络等静默处理,不影响主功能 */ }
                    }
                    delay(60_000)
                }
            }
        }

        if (savedInstanceState == null) {
            switchTo(R.id.nav_vpn)
        } else {
            // 进程被杀/配置变更(切深色等)后恢复:
            // show/hide 的隐藏状态不跨重建,所有 fragment 会全部恢复可见 → 叠影,
            // 按 KEY_TAB 只显示目标,其余全部隐藏
            selectedTab.intValue = savedInstanceState.getInt(KEY_TAB, 0)
            val targetTag = when (selectedTab.intValue) {
                0 -> "vpn"
                1 -> "medication"
                2 -> "reminder"
                else -> "more"
            }
            val fm = supportFragmentManager
            val tx = fm.beginTransaction()
            activeFragment = fm.fragments.firstOrNull { it.tag == targetTag }
            fm.fragments.forEach {
                if (it.isAdded && it !== activeFragment) tx.hide(it)
            }
            activeFragment?.let { tx.show(it) }
            tx.commit()
        }

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startMonitorService()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_TAB, selectedTab.intValue)
    }

    override fun onResume() {
        super.onResume()
        // 用户从系统设置打开通知权限后回来,这里补上服务启动,形成权限闭环
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startMonitorService()
        }
    }

    // 底栏点击 → 切换 fragment,选中态写回 Compose
    private fun onTabSelected(index: Int) {
        selectedTab.intValue = index
        val itemId = when (index) {
            0 -> R.id.nav_vpn
            1 -> R.id.nav_medication
            2 -> R.id.nav_reminder
            else -> R.id.nav_more
        }
        switchTo(itemId)
    }

    // tab 切换用 show/hide 缓存的 fragment,不再每次 replace 重建:
    // WebView 不整页重载、列表滚动位置/开关状态都保留
    private fun switchTo(itemId: Int) {
        val tag = when (itemId) {
            R.id.nav_vpn -> "vpn"
            R.id.nav_medication -> "medication"
            R.id.nav_reminder -> "reminder"
            else -> "more"
        }
        val fm = supportFragmentManager
        val tx = fm.beginTransaction()
        val target = fm.findFragmentByTag(tag)
            ?: createFragment(itemId).also { tx.add(R.id.fragmentContainer, it, tag) }
        activeFragment?.takeIf { it !== target }?.let { tx.hide(it) }
        tx.show(target)
        activeFragment = target
        tx.commit()
    }

    private fun createFragment(itemId: Int): Fragment = when (itemId) {
        R.id.nav_vpn -> VpnFragment()
        R.id.nav_medication -> MedicationFragment()
        R.id.nav_reminder -> ReminderFragment()
        else -> MoreFragment()
    }

    private fun startMonitorService() {
        val intent = Intent(this, VpnMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    companion object {
        private const val KEY_TAB = "tab"
    }
}

// 全屏根:页面内容(容器)通过 AndroidView 承载并登记为 backdrop 源,
// 库会帧同步采样它——磨砂不会随机变透明;顶条/底栏悬浮其上
@Composable
private fun AppRoot(
    container: FragmentContainerView,
    isScrolling: Boolean,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    val backdrop: LayerBackdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                // 容器在 onCreate 时已挂在 XML 层级里(onStart 前 FragmentManager 需要),
                // 这里重定父级进 Compose 树
                (container.parent as? ViewGroup)?.removeView(container)
                container
            },
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        )
        GlassStatusBar(
            backdrop = backdrop,
            isScrolling = isScrolling,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        GlassBottomBar(
            selected = selected,
            onSelected = onSelected,
            contentView = { container },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 16.dp)
        )
    }
}
