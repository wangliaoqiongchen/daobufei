package com.mo.daobufei

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("VpnCheck", "通知权限: $granted")
            if (granted) startMonitorService()
        }

    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 15+ 强制 edge-to-edge,内容会延伸到状态栏/导航栏下面,
        // 必须手动把系统栏高度折算成容器内边距,否则顶部内容被状态栏压住
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val container = findViewById<FragmentContainerView>(R.id.fragmentContainer)

        // 状态栏图标颜色跟随深浅色主题(浅色模式用深色图标)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_YES

        // 系统栏/键盘高度折算成容器内边距,内容不被状态栏压住
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(top = bars.top, bottom = ime.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = bars.bottom)
            insets
        }

        bottomNav.setOnItemSelectedListener { item ->
            switchTo(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            switchTo(R.id.nav_vpn)
        } else {
            // 进程被杀后恢复:FragmentManager 已还原各 fragment,找出当前显示的那个
            activeFragment = supportFragmentManager.fragments.firstOrNull { it.isVisible }
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
}
