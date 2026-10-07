package com.mo.daobufei

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
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class MoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            setPadding(context.dp(20), context.dp(12), context.dp(20), context.dp(20))
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // ---------- App 信息头 ----------
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

        // ---------- 功能一览 ----------
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

        // ---------- 权限说明 ----------
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
                text = "• $title"
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

        // ---------- 作者与联系方式 ----------
        root.addView(
            sectionCard(
                context,
                context.getString(R.string.more_author_title),
                context.getString(R.string.more_author_content)
            )
        )

        scrollView.addView(root)
        return scrollView
    }

    // 卡片内的小标题
    private fun cardTitle(context: android.content.Context, title: String): TextView {
        return TextView(context).apply {
            text = title
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
            setPadding(0, 0, 0, context.dp(8))
        }
    }

    // 功能一览行:圆角图标 + 标题 + 描述
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

    // 生成一张标题+正文的卡片,减少重复代码
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
