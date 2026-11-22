package com.mo.daobufei

import android.content.Context

object SettingsPrefs {
    private const val PREFS_NAME = "settings"
    private const val KEY_SHOW_IDLE_NOTIFICATION = "show_idle_notification"
    private const val KEY_NIGHT_MODE = "night_mode"

    // 深色模式:0=跟随系统 1=浅色 2=深色
    const val NIGHT_FOLLOW_SYSTEM = 0
    const val NIGHT_LIGHT = 1
    const val NIGHT_DARK = 2

    fun isIdleNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_IDLE_NOTIFICATION, true) // 默认开启
    }

    fun setIdleNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_IDLE_NOTIFICATION, enabled).apply()
    }

    fun nightMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NIGHT_MODE, NIGHT_FOLLOW_SYSTEM)
    }

    fun setNightMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply()
    }
}
