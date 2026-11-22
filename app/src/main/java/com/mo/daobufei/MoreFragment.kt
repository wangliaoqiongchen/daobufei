package com.mo.daobufei

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            // 内容穿透到顶部磨砂条和液态玻璃底栏后面;首项从条下开始
            clipToPadding = false
            setPadding(context.dp(20), 0, context.dp(20), context.dp(120))
            applyTopInset(24)
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // ---------- App 淇℃伅澶?----------
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(context.dp(4), context.dp(8), context.dp(4), context.dp(20))
        }
        val appIcon = ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(context.dp(56), context.dp(56))
            background = context.circleDrawable(context.color(R.color.card_bg))
            setPadding(context.dp(3), context.dp(3), context.dp(3), context.dp(3))
            clipToBackgroundShape()
        }
        val appInfoCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(14), 0, 0, 0)
        }
        val appNameText = TextView(context).apply {
            text = context.getString(R.string.app_name)
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        }
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
        val appVersionText = TextView(context).apply {
            text = context.getString(R.string.more_tagline, versionName)
            textSize = 13f
            setTextColor(context.color(R.color.text_secondary))
            setPadding(0, context.dp(4), 0, 0)
        }
        appInfoCol.addView(appNameText)
        appInfoCol.addView(appVersionText)
        headerRow.addView(appIcon)
        headerRow.addView(appInfoCol)
        root.addView(headerRow)

        // ---------- 鍔熻兘涓€瑙?----------
        val featureCard = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(14)) }
        }
        val featureCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(8))
        }
        featureCol.addView(cardTitle(context, context.getString(R.string.more_features_title)))
        featureCol.addView(
            featureRow(
                context, R.drawable.ic_vpn,
                context.getString(R.string.feature_vpn_title),
                context.getString(R.string.feature_vpn_desc)
            )
        )
        featureCol.addView(
            featureRow(
                context, R.drawable.ic_alarm,
                context.getString(R.string.feature_reminder_title),
                context.getString(R.string.feature_reminder_desc)
            )
        )
        featureCol.addView(
            featureRow(
                context, R.drawable.ic_medication,
                context.getString(R.string.feature_med_title),
                context.getString(R.string.feature_med_desc)
            )
        )
        featureCol.addView(
            featureRow(
                context, R.drawable.ic_globe,
                context.getString(R.string.feature_diag_title),
                context.getString(R.string.feature_diag_desc)
            )
        )
        featureCard.addView(featureCol)
        root.addView(featureCard)

        // ---------- 鏉冮檺璇存槑 ----------
        val permissionCard = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(14)) }
        }
        val permissionContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(8))
        }
        permissionContent.addView(cardTitle(context, context.getString(R.string.more_permissions_title)))
        val permissions = listOf(
            context.getString(R.string.perm_notif_title) to context.getString(R.string.perm_notif_desc),
            context.getString(R.string.perm_net_title) to context.getString(R.string.perm_net_desc),
            context.getString(R.string.perm_fgs_title) to context.getString(R.string.perm_fgs_desc),
            context.getString(R.string.perm_netstate_title) to context.getString(R.string.perm_netstate_desc)
        )
        permissions.forEach { (title, desc) ->
            val itemCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, context.dp(8), 0, context.dp(8))
            }
            val itemTitle = TextView(context).apply {
                text = "鈥?$title"
                textSize = 15f
                setTextColor(context.color(R.color.text_primary))
            }
            val itemDesc = TextView(context).apply {
                text = desc
                textSize = 13f
                setTextColor(context.color(R.color.text_secondary))
                setPadding(context.dp(10), context.dp(2), 0, 0)
            }
            itemCol.addView(itemTitle)
            itemCol.addView(itemDesc)
            permissionContent.addView(itemCol)
        }
        permissionCard.addView(permissionContent)
        root.addView(permissionCard)

        // ---------- 娣辫壊妯″紡 ----------
        root.addView(buildDarkModeCard(context))

        // ---------- 鍚庡彴淇濇椿 ----------
        root.addView(buildKeepAliveCard(context))

        // ---------- 浣滆€呬笌鑱旂郴鏂瑰紡 ----------
        root.addView(
            sectionCard(
                context,
                context.getString(R.string.more_author_title),
                context.getString(R.string.more_author_content)
            )
        )

        // ---------- GitHub ----------
        root.addView(buildGitHubCard(context))

        scrollView.addView(root)
        return scrollView
    }

    // 娣辫壊妯″紡閫夋嫨鍗?璺熼殢绯荤粺 / 娴呰壊 / 娣辫壊,鐐瑰嚮绔嬪嵆鍏ㄥ眬鐢熸晥
    private fun buildDarkModeCard(context: android.content.Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(14)) }
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16))
        }
        col.addView(cardTitle(context, context.getString(R.string.dark_mode_title)))

        val current = SettingsPrefs.nightMode(context)
        val options = listOf(
            SettingsPrefs.NIGHT_FOLLOW_SYSTEM to context.getString(R.string.dark_follow),
            SettingsPrefs.NIGHT_LIGHT to context.getString(R.string.dark_light),
            SettingsPrefs.NIGHT_DARK to context.getString(R.string.dark_dark)
        )
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        options.forEach { (mode, label) ->
            val isSelected = mode == current
            row.addView(TextView(context).apply {
                text = label
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(
                    context.color(if (isSelected) R.color.accent else R.color.text_secondary)
                )
                setPadding(context.dp(14), context.dp(8), context.dp(14), context.dp(8))
                background = context.pillDrawable(
                    context.color(if (isSelected) R.color.accent_container else R.color.card_bg_muted)
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    weight = 1f
                    setMargins(context.dp(2), 0, context.dp(2), 0)
                }
                gravity = android.view.Gravity.CENTER
                setOnClickListener {
                    if (mode != SettingsPrefs.nightMode(context)) {
                        SettingsPrefs.setNightMode(context, mode)
                        when (mode) {
                            SettingsPrefs.NIGHT_LIGHT ->
                                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                )
                            SettingsPrefs.NIGHT_DARK ->
                                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                )
                            else ->
                                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                )
                        }
                    }
                }
            })
        }
        col.addView(row)
        card.addView(col)
        return card
    }

    // 鍚庡彴淇濇椿鍗?妫€鏌ョ數姹犱紭鍖栫櫧鍚嶅崟,娌″姞鐧藉氨涓€閿敵璇?
    // (ColorOS 鐨?鍚庡彴鏃犻檺鍒?鍜岀郴缁熺殑鐢垫睜浼樺寲鐧藉悕鍗曟槸涓ゅ洖浜?鍚庤€呮墠鏄‖寮€鍏?
    private fun buildKeepAliveCard(context: android.content.Context): MaterialCardView {
        val pm = context.getSystemService(android.os.PowerManager::class.java)
        val isIgnoring = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(14)) }
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16))
        }
        col.addView(cardTitle(context, context.getString(R.string.keepalive_title)))
        col.addView(TextView(context).apply {
            text = context.getString(R.string.keepalive_desc)
            textSize = 12f
            setTextColor(context.color(R.color.text_secondary))
            setPadding(0, 0, 0, context.dp(10))
        })

        val statusText = TextView(context).apply {
            text = if (isIgnoring) {
                context.getString(R.string.keepalive_ok)
            } else {
                context.getString(R.string.keepalive_bad)
            }
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(
                context.color(if (isIgnoring) R.color.status_good else R.color.status_bad)
            )
            setPadding(0, 0, 0, context.dp(12))
        }
        col.addView(statusText)

        if (!isIgnoring) {
            col.addView(MaterialButton(context).apply {
                text = context.getString(R.string.keepalive_action)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    try {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(android.net.Uri.parse("package:${context.packageName}"))
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            context, context.getString(R.string.action_failed), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        }
        card.addView(col)
        return card
    }

    // GitHub 鍗?鐐瑰嚮鎵撳紑寮€婧愪粨搴?
    private fun buildGitHubCard(context: android.content.Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, android.net.Uri.parse(context.getString(R.string.github_url)))
                    )
                } catch (e: Exception) {
                    // 娌℃湁娴忚鍣ㄥ氨蹇界暐
                }
            }
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(context.dp(16), context.dp(14), context.dp(16), context.dp(14))
        }
        val iconBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40))
            background = context.roundDrawable(context.color(R.color.accent_container), 12f)
        }
        val icon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(context.dp(20), context.dp(20), android.view.Gravity.CENTER)
            setImageResource(R.drawable.ic_globe)
            imageTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.accent))
        }
        iconBox.addView(icon)

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(context.dp(12), 0, 0, 0) }
        }
        val titleText = TextView(context).apply {
            text = context.getString(R.string.github_title)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        }
        val descText = TextView(context).apply {
            text = context.getString(R.string.github_desc)
            textSize = 12f
            setTextColor(context.color(R.color.text_secondary))
            setPadding(0, context.dp(2), 0, 0)
        }
        textCol.addView(titleText)
        textCol.addView(descText)

        row.addView(iconBox)
        row.addView(textCol)
        card.addView(row)
        return card
    }

    // 鍗＄墖鍐呯殑灏忔爣棰?
    private fun cardTitle(context: android.content.Context, title: String): TextView {
        return TextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
            setPadding(0, 0, 0, context.dp(8))
        }
    }

    // 鍔熻兘涓€瑙堣:鍦嗚鍥炬爣 + 鏍囬 + 鎻忚堪
    private fun featureRow(
        context: android.content.Context, iconRes: Int, title: String, desc: String
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(10), 0, context.dp(10))
        }
        val iconBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40))
            background = context.roundDrawable(context.color(R.color.accent_container), 12f)
        }
        val icon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(context.dp(20), context.dp(20), android.view.Gravity.CENTER)
            setImageResource(iconRes)
            imageTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.accent))
        }
        iconBox.addView(icon)

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(context.dp(12), 0, 0, 0) }
        }
        val titleText = TextView(context).apply {
            text = title
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        }
        val descText = TextView(context).apply {
            text = desc
            textSize = 12f
            setTextColor(context.color(R.color.text_secondary))
            setPadding(0, context.dp(2), 0, 0)
        }
        textCol.addView(titleText)
        textCol.addView(descText)

        row.addView(iconBox)
        row.addView(textCol)
        return row
    }

    // 鐢熸垚涓€寮犳爣棰?姝ｆ枃鐨勫崱鐗?鍑忓皯閲嶅浠ｇ爜
    private fun sectionCard(
        context: android.content.Context, title: String, content: String
    ): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(14)) }
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16))
        }
        val titleText = TextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
            setPadding(0, 0, 0, context.dp(6))
        }
        val contentText = TextView(context).apply {
            text = content
            textSize = 14f
            setTextColor(context.color(R.color.text_secondary))
        }
        col.addView(titleText)
        col.addView(contentText)
        card.addView(col)
        return card
    }
}
