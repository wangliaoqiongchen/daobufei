package com.mo.daobufei

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("VpnCheck", "通知权限: $granted")
            if (granted) startMonitorService()
        }

    private val selectedTab = mutableIntStateOf(0)
    private var activeFragment: Fragment? = null
    private lateinit var container: FragmentContainerView
    private lateinit var glassBarView: ComposeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 15+ 强制 edge-to-edge,内容会延伸到状态栏/导航栏下面
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 状态栏图标颜色跟随深浅色主题(浅色模式用深色图标)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_YES

        setContentView(R.layout.activity_main)
        container = findViewById(R.id.fragmentContainer)
        glassBarView = findViewById(R.id.glassBarView)

        // 系统栏/键盘高度折算成容器内边距:顶部不被状态栏压住,键盘弹出时避让
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(top = bars.top, bottom = ime.bottom)
            insets
        }
        // 玻璃底栏抬到手势条上方
        ViewCompat.setOnApplyWindowInsetsListener(glassBarView) { view, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = nav.bottom)
            insets
        }

        // 玻璃底栏:CanvasBackdrop 把容器内容按屏幕位置画进玻璃做折射;
        // 页面滚动时通过 OnScrollChangedListener 触发重画,保持折射实时
        glassBarView.setContent {
            val backdrop = rememberCanvasBackdrop {
                val containerLoc = IntArray(2)
                val barLoc = IntArray(2)
                container.getLocationOnScreen(containerLoc)
                glassBarView.getLocationOnScreen(barLoc)
                val canvas = drawContext.canvas.nativeCanvas
                canvas.save()
                canvas.translate(
                    (containerLoc[0] - barLoc[0]).toFloat(),
                    (containerLoc[1] - barLoc[1]).toFloat()
                )
                container.draw(canvas)
                canvas.restore()
            }
            GlassBottomBar(
                selected = selectedTab.intValue,
                onSelected = { index -> onTabSelected(index) },
                backdrop = backdrop
            )
        }
        container.viewTreeObserver.addOnScrollChangedListener {
            glassBarView.invalidate()
        }

        if (savedInstanceState == null) {
            switchTo(R.id.nav_vpn)
        } else {
            // 进程被杀后恢复:FragmentManager 已还原各 fragment,找出当前显示的那个
            activeFragment = supportFragmentManager.fragments.firstOrNull { it.isVisible }
            selectedTab.intValue = savedInstanceState.getInt(KEY_TAB, 0)
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
