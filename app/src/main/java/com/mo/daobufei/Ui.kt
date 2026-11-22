package com.mo.daobufei

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

// 页面根视图把状态栏高度折算成顶部内边距:
// 滚动内容可穿透到顶部磨砂条后面,首项仍从条下开始
fun View.applyTopInset(extraTopDp: Int = 0) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(top = bars.top + view.context.dp(extraTopDp))
        insets
    }
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

// FlClash 风格磨砂弹窗:圆角 24dp 半透明面板 + 窗口后模糊(设备不支持时退化为纯半透明),
// 时/分双滚轮一屏全展开直接拨动
fun showTimePickerDialog(
    context: Context, hour: Int, minute: Int, onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val panelColor =
        if (isDark) 0xE61A1D23L.toInt() else 0xE6FFFFFFL.toInt()
    val titleColor =
        if (isDark) 0xFFE9ECF0L.toInt() else 0xFF171C22L.toInt()
    val secondaryColor =
        if (isDark) 0xFF98A2B0L.toInt() else 0xFF5D6673L.toInt()
    val accentColor =
        if (isDark) 0xFF4CC38AL.toInt() else 0xFF1FA35FL.toInt()

    val dialog = Dialog(context)
    dialog.window?.apply {
        setBackgroundDrawableResource(android.R.color.transparent)
        setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setDimAmount(0.2f)
        // 窗口后模糊:面板背后的内容被磨砂化(设备不支持时系统自动忽略,仍有半透明面板)
        if (Build.VERSION.SDK_INT >= 31) {
            setFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            )
            attributes.blurBehindRadius = context.dp(40)
        }
    }

    val title = TextView(context).apply {
        text = context.getString(R.string.time_dialog_title)
        textSize = 17f
        setTypeface(Typeface.DEFAULT_BOLD)
        setTextColor(titleColor)
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
        setTextColor(secondaryColor)
    }
    val pickersRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(0, context.dp(12), 0, context.dp(16))
        addView(hourPicker)
        addView(colon)
        addView(minutePicker)
    }

    val cancelButton = TextView(context).apply {
        text = context.getString(R.string.action_cancel)
        textSize = 15f
        setTypeface(Typeface.DEFAULT_BOLD)
        setTextColor(secondaryColor)
        setPadding(context.dp(18), context.dp(10), context.dp(18), context.dp(10))
        setOnClickListener { dialog.dismiss() }
    }
    val okButton = TextView(context).apply {
        text = context.getString(R.string.dialog_ok)
        textSize = 15f
        setTypeface(Typeface.DEFAULT_BOLD)
        setTextColor(accentColor)
        setPadding(context.dp(18), context.dp(10), context.dp(18), context.dp(10))
        setOnClickListener {
            onConfirm(hourPicker.value, minutePicker.value)
            dialog.dismiss()
        }
    }
    val buttonsRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
        addView(cancelButton)
        addView(okButton)
    }

    val panel = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dp(24), context.dp(20), context.dp(24), context.dp(10))
        background = context.roundDrawable(panelColor, 24f)
        addView(title)
        addView(pickersRow)
        addView(buttonsRow)
    }
    val wrapper = FrameLayout(context).apply {
        setPadding(context.dp(24), 0, context.dp(24), 0)
        addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    dialog.setContentView(wrapper)
    dialog.show()
}
