package com.mo.daobufei

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("VpnCheck", "通知权限: $granted")
            if (granted) startMonitorService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, VpnFragment())
                .commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_vpn -> VpnFragment()
                R.id.nav_dev1 -> DevPlaceholderFragment()
                R.id.nav_dev2 -> WebFragment()
                R.id.nav_more -> MoreFragment()
                else -> VpnFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }

        checkPromotedNotificationAccess()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startMonitorService()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkPromotedNotificationAccess() {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        val canPromote = manager.canPostPromotedNotifications()
        Log.d("VpnCheck", "流体云权限 canPostPromotedNotifications = $canPromote")
        if (!canPromote) {
            Toast.makeText(
                this, "需要去系统设置里,手动允许本 App 显示流体云", Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startMonitorService() {
        val intent = Intent(this, VpnMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}