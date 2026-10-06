package com.mo.daobufei

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.inputmethod.InputMethodManager
import android.widget.NumberPicker
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import java.util.Locale

// UI 通用工具:dp 换算、按深浅色取色 token、胶囊/圆形背景
// 所有页面统一走这里,避免再出现硬编码像素和颜色
fun Context.dp(dp: Int): Int = (dp * resources.displayMetrics.density + 0.5f).toInt()

fun Context.color(@ColorRes res: Int): Int = ContextCompat.getColor(this, res)

fun Context.pillDrawable(color: Int): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = 999f
    setColor(color)
}

fun Context.circleDrawable(color: Int): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(color)
}

fun Context.roundDrawable(color: Int, cornerDp: Float): GradientDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = cornerDp * resources.displayMetrics.density
    setColor(color)
}

// 让 View 按 background 的形状裁剪内容,配合圆形背景即可得到圆形图片
fun View.clipToBackgroundShape() {
    clipToOutline = true
    outlineProvider = ViewOutlineProvider.BACKGROUND
}

// 点击输入框以外的区域时收起键盘并清除焦点
// (挂在页面根 ScrollView 上,返回 false 不影响子 View 的点击和滚动)
fun View.hideKeyboardOnTouchOutside() {
    setOnTouchListener { view, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            findFocus()?.clearFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        false
    }
}

// 滚轮式时间选择:时/分两个转轮一屏全部展开,直接上下拨动,不用两步式表盘
fun showTimePickerDialog(
    context: Context, hour: Int, minute: Int, onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val container = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER
        setPadding(context.dp(24), context.dp(16), context.dp(24), context.dp(8))
    }
    val hourPicker = NumberPicker(context).apply {
        minValue = 0
        maxValue = 23
        value = hour
        wrapSelectorWheel = true
    }
    val minutePicker = NumberPicker(context).apply {
        minValue = 0
        maxValue = 59
        value = minute
        wrapSelectorWheel = true
        displayedValues = (0..59).map { String.format(Locale.ROOT, "%02d", it) }.toTypedArray()
    }
    val colon = TextView(context).apply {
        text = ":"
        textSize = 22f
        setPadding(context.dp(8), 0, context.dp(8), 0)
    }
    container.addView(hourPicker)
    container.addView(colon)
    container.addView(minutePicker)
    AlertDialog.Builder(context)
        .setTitle("选择时间")
        .setView(container)
        .setPositiveButton("确定") { _, _ -> onConfirm(hourPicker.value, minutePicker.value) }
        .setNegativeButton("取消", null)
        .show()
}


