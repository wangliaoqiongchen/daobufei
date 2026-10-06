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
            text = "岛不废"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        }
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "未知"
        }
        val appVersionText = TextView(context).apply {
            text = "版本 $versionName · 流体云工具箱"
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
        featureCol.addView(cardTitle(context, "功能一览"))
        featureCol.addView(
            featureRow(
                context, R.drawable.ic_vpn, "VPN 流体云监控",
                "连接后在流体云显示节点国旗与实时延迟,支持历史趋势"
            )
        )
        featureCol.addView(
            featureRow(
                context, R.drawable.ic_alarm, "自定义提醒",
                "自选标题与时间,到点弹出系统通知,每日重复"
            )
        )
        featureCol.addView(
            featureRow(
                context, R.drawable.ic_medication, "用药提醒",
                "多时段提醒 + 服药日历统计(开发中,敬请期待)"
            )
        )
        featureCol.addView(
            featureRow(
                context, R.drawable.ic_globe, "IP 诊断",
                "查看当前出口 IP 与网络路由,排查节点是否生效"
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
        permissionContent.addView(cardTitle(context, "权限说明"))
        val permissions = listOf(
            "通知权限" to "用于在流体云和通知栏显示 VPN 状态与提醒",
            "网络访问权限" to "用于检测公网 IP、查询节点归属国家、测量延迟",
            "前台服务权限" to "用于在后台持续监控,不被系统随意终止",
            "查看网络状态权限" to "用于检测当前是否有 VPN 连接"
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
        root.addView(sectionCard(
            context, "作者与联系方式",
            "作者:Mo\n联系方式:(q群:1019861339)\n\n有问题或想法欢迎来群里聊。"
        ))

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
