package com.mo.daobufei

import android.content.Context

object SettingsPrefs {
    private const val PREFS_NAME = "settings"
    private const val KEY_SHOW_IDLE_NOTIFICATION = "show_idle_notification"

    fun isIdleNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_IDLE_NOTIFICATION, true) // 默认开启
    }

    fun setIdleNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_IDLE_NOTIFICATION, enabled).apply()
    }
}