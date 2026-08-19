package com.mo.daobufei

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        val scrollView = ScrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 160, 48, 64)
        }

        // ---------- App 信息头 ----------
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16, 16, 16, 48)
        }
        val appIcon = ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(140, 140)
        }
        val appInfoCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 0, 0, 0)
        }
        val appNameText = TextView(context).apply {
            text = "岛不废"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "未知"
        }
        val appVersionText = TextView(context).apply {
            text = "版本 $versionName"
            textSize = 14f
            setTextColor(Color.GRAY)
        }
        appInfoCol.addView(appNameText)
        appInfoCol.addView(appVersionText)
        headerRow.addView(appIcon)
        headerRow.addView(appInfoCol)
        root.addView(headerRow)

        // ---------- 关于 ----------
        root.addView(sectionCard(
            context, "关于",
            "流体云工具箱,第一阶段实现 VPN 状态展示:连接后在流体云区域显示节点国旗与实时延迟。"
        ))

        // ---------- 作者与联系方式 ----------
        root.addView(sectionCard(
            context, "作者与联系方式",
            "作者:Mo\n联系方式:(q群:1019861339)"
        ))

        // ---------- 权限说明 ----------
        val permissionCard = MaterialCardView(context).apply {
            radius = 32f
            cardElevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 32) }
        }
        val permissionContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        val permissionTitle = TextView(context).apply {
            text = "权限说明"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        permissionContent.addView(permissionTitle)

        val permissions = listOf(
            "通知权限" to "用于在流体云和通知栏显示 VPN 连接状态",
            "网络访问权限" to "用于检测公网 IP、查询节点归属国家、测量延迟",
            "前台服务权限" to "用于在后台持续监控 VPN 状态,不被系统随意终止",
            "查看网络状态权限" to "用于检测当前是否有 VPN 连接"
        )
        permissions.forEach { (title, desc) ->
            val itemCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
            }
            val itemTitle = TextView(context).apply {
                text = "• $title"
                textSize = 15f
            }
            val itemDesc = TextView(context).apply {
                text = desc
                textSize = 13f
                setTextColor(Color.GRAY)
                setPadding(24, 4, 0, 0)
            }
            itemCol.addView(itemTitle)
            itemCol.addView(itemDesc)
            permissionContent.addView(itemCol)
        }
        permissionCard.addView(permissionContent)
        root.addView(permissionCard)

        scrollView.addView(root)
        return scrollView
    }

    // 生成一张标题+正文的卡片,减少重复代码
    private fun sectionCard(context: android.content.Context, title: String, content: String): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = 32f
            cardElevation = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 32) }
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        val titleText = TextView(context).apply {
            text = title
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        val contentText = TextView(context).apply {
            text = content
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }
        col.addView(titleText)
        col.addView(contentText)
        card.addView(col)
        return card
    }
}