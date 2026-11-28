package com.mo.daobufei.update

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.mo.daobufei.R
import com.mo.daobufei.dp
import com.mo.daobufei.roundDrawable

// 更新弹窗(磨砂面板风格,与时间选择弹窗一致)
// forced = 强制更新:无"暂不更新"按钮,不可点外部关闭,提供"退出应用"
object UpdateDialogs {

    private var activeDialog: Dialog? = null

    // 软件更新安装后首次进入:提示"更新成功"(首次安装不弹,仅升级时弹)
    fun showUpdateSuccessDialog(context: Context, versionName: String) {
        val isDark = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
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
            setDimAmount(0.25f)
            if (Build.VERSION.SDK_INT >= 31) {
                setFlags(
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                )
                attributes.blurBehindRadius = context.dp(40)
            }
        }

        val titleText = TextView(context).apply {
            text = context.getString(R.string.update_success_title)
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(titleColor)
            gravity = Gravity.CENTER
        }
        val versionText = TextView(context).apply {
            text = context.getString(R.string.update_success_text, versionName)
            textSize = 14f
            setTextColor(secondaryColor)
            gravity = Gravity.CENTER
            setPadding(0, context.dp(10), 0, context.dp(6))
        }
        val okButton = TextView(context).apply {
            text = context.getString(R.string.dialog_ok)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accentColor)
            setPadding(context.dp(24), context.dp(10), context.dp(24), context.dp(10))
            setOnClickListener { dialog.dismiss() }
        }

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dp(24), context.dp(24), context.dp(24), context.dp(12))
            background = context.roundDrawable(panelColor, 24f)
            addView(titleText)
            addView(versionText)
            addView(okButton)
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
        activeDialog = dialog
    }

    fun showUpdateDialog(context: Context, manifest: UpdateManifest, forced: Boolean) {
        activeDialog?.dismiss()

        val isDark = (context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val panelColor =
            if (isDark) 0xE61A1D23L.toInt() else 0xE6FFFFFFL.toInt()
        val titleColor =
            if (isDark) 0xFFE9ECF0L.toInt() else 0xFF171C22L.toInt()
        val secondaryColor =
            if (isDark) 0xFF98A2B0L.toInt() else 0xFF5D6673L.toInt()
        val accentColor =
            if (isDark) 0xFF4CC38AL.toInt() else 0xFF1FA35FL.toInt()
        val dialogContext = context

        val dialog = Dialog(context)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setDimAmount(0.3f)
            if (Build.VERSION.SDK_INT >= 31) {
                setFlags(
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                    WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                )
                attributes.blurBehindRadius = context.dp(40)
            }
        }

        val titleText = TextView(context).apply {
            text = manifest.title
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(titleColor)
        }
        val versionText = TextView(context).apply {
            text = "v${manifest.versionName} · ${manifest.publishedAtDisplay}"
            textSize = 12f
            setTextColor(secondaryColor)
            setPadding(0, context.dp(4), 0, context.dp(10))
        }
        val changelogCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        manifest.changelog.forEach { line ->
            changelogCol.addView(TextView(context).apply {
                text = "• $line"
                textSize = 13f
                setTextColor(titleColor)
                setPadding(0, context.dp(3), 0, context.dp(3))
            })
        }
        val changelogScroll = ScrollView(context).apply {
            // 更新日志过长时限高滚动;条数少时自适应高度
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (manifest.changelog.size >= 5) context.dp(240)
                else ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        changelogScroll.addView(changelogCol)

        val forcedNote = TextView(context).apply {
            text = context.getString(R.string.update_dialog_forced_note)
            textSize = 11f
            setTextColor(secondaryColor)
            setPadding(0, context.dp(10), 0, 0)
        }

        val ignoreButton = TextView(context).apply {
            text = context.getString(R.string.update_dialog_ignore)
            textSize = 12f
            setTextColor(secondaryColor)
            setPadding(context.dp(14), context.dp(8), context.dp(14), context.dp(8))
            setOnClickListener {
                com.mo.daobufei.update.UpdatePrefs(context).ignoredVersionCode = manifest.versionCode
                dialog.dismiss()
            }
        }
        val skipButton = TextView(context).apply {
            text = context.getString(R.string.update_dialog_skip)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(secondaryColor)
            setPadding(context.dp(18), context.dp(10), context.dp(18), context.dp(10))
            setOnClickListener { dialog.dismiss() }
        }
        val downloadButton = TextView(context).apply {
            text = context.getString(R.string.update_dialog_download)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accentColor)
            setPadding(context.dp(18), context.dp(10), context.dp(18), context.dp(10))
            setOnClickListener {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(manifest.downloadPageUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.update_dialog_open_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (!forced) dialog.dismiss()
            }
        }
        val exitButton = TextView(context).apply {
            text = context.getString(R.string.update_dialog_exit)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(secondaryColor)
            setPadding(context.dp(18), context.dp(10), context.dp(18), context.dp(10))
            setOnClickListener {
                (dialogContext as? Activity)?.finishAffinity()
            }
        }
        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            if (!forced) addView(skipButton)
            addView(downloadButton)
            if (forced) addView(exitButton)
        }
        // 忽略入口:非强制时出现在按钮下方居中,点了不再提醒此版本号
        val ignoreRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            if (!forced) addView(ignoreButton)
        }

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(24), context.dp(20), context.dp(24), context.dp(10))
            background = context.roundDrawable(panelColor, 24f)
            addView(titleText)
            addView(versionText)
            addView(changelogScroll)
            if (forced) addView(forcedNote)
            addView(buttonsRow)
            addView(ignoreRow)
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
        dialog.setCancelable(!forced)
        dialog.show()
        activeDialog = dialog
    }
}
