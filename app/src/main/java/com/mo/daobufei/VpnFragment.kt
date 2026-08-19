package com.mo.daobufei

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class VpnFragment : Fragment() {

    private lateinit var heroCard: MaterialCardView
    private lateinit var flagImage: ImageView
    private lateinit var latencyText: TextView
    private lateinit var statusPill: TextView
    private lateinit var countryStatText: TextView
    private lateinit var qualityStatText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val scrollView = ScrollView(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 160, 48, 64)
        }

        // ---------- 顶部状态大卡片 ----------
        heroCard = MaterialCardView(context).apply {
            radius = 64f
            cardElevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 32) }
        }
        val heroContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(48, 96, 48, 96)
        }
        flagImage = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(180, 180)
        }
        latencyText = TextView(context).apply {
            textSize = 56f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        statusPill = TextView(context).apply {
            textSize = 13f
            setPadding(40, 12, 40, 12)
            background = GradientDrawable().apply {
                cornerRadius = 100f
            }
        }
        heroContent.addView(flagImage)
        heroContent.addView(latencyText)
        heroContent.addView(statusPill)
        heroCard.addView(heroContent)
        root.addView(heroCard)

        // ---------- 中间两张并排小卡片 ----------
        val statRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 32) }
        }
        val (countryCard, countryValueView) = buildStatCard(context, "国家/地区")
        val (qualityCard, qualityValueView) = buildStatCard(context, "网络质量")
        countryStatText = countryValueView
        qualityStatText = qualityValueView
        (countryCard.layoutParams as LinearLayout.LayoutParams).apply {
            width = 0; weight = 1f; marginEnd = 16
        }
        (qualityCard.layoutParams as LinearLayout.LayoutParams).apply {
            width = 0; weight = 1f; marginStart = 16
        }
        statRow.addView(countryCard)
        statRow.addView(qualityCard)
        root.addView(statRow)

        // ---------- 底部设置列表行 ----------
        root.addView(buildSwitchRow(context))

        scrollView.addView(root)
        return scrollView
    }

    // 生成一张"标题 + 数值"的小卡片,返回卡片本身和数值 TextView(方便后续更新内容)
    private fun buildStatCard(context: android.content.Context, title: String): Pair<MaterialCardView, TextView> {
        val card = MaterialCardView(context).apply {
            radius = 32f
            cardElevation = 2f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        val titleText = TextView(context).apply {
            text = title
            textSize = 12f
            setTextColor(Color.GRAY)
        }
        val valueText = TextView(context).apply {
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 0)
        }
        col.addView(titleText)
        col.addView(valueText)
        card.addView(col)
        return card to valueText
    }

    // 设置列表行:图标 + 标题 + 右侧开关
    private fun buildSwitchRow(context: android.content.Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = 32f
            cardElevation = 2f
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(40, 32, 40, 32)
        }
        val label = TextView(context).apply {
            text = "VPN 断开时提醒"
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        val switch = MaterialSwitch(context).apply {
            isChecked = SettingsPrefs.isIdleNotificationEnabled(context)
            setOnCheckedChangeListener { _, isChecked ->
                SettingsPrefs.setIdleNotificationEnabled(context, isChecked)
            }
        }
        row.addView(label)
        row.addView(switch)
        card.addView(row)
        return card
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                VpnMonitorService.vpnStatus.collect { status ->
                    updateUi(status)
                }
            }
        }
    }

    private fun updateUi(status: VpnStatus) {
        if (!status.isConnected) {
            flagImage.setImageDrawable(null)
            latencyText.text = "--"
            latencyText.setTextColor(Color.GRAY)
            applyHeroColor(Color.parseColor("#EEEEEE"))
            setPill("未连接", Color.parseColor("#9E9E9E"))
            countryStatText.text = "--"
            qualityStatText.text = "--"
            return
        }

        val flagResId = FlagUtils.getFlagDrawableResId(requireContext(), status.countryCode)
        if (flagResId != 0) {
            flagImage.setImageResource(flagResId)
        } else {
            flagImage.setImageDrawable(null)
        }
        countryStatText.text = status.countryName

        when {
            status.isTimeout -> {
                latencyText.text = "Timeout"
                val color = Color.parseColor("#F44336")
                latencyText.setTextColor(color)
                applyHeroColor(Color.parseColor("#FFEBEE"))
                setPill("超时", color)
                qualityStatText.text = "较差"
            }
            status.latencyMs != null -> {
                latencyText.text = "${status.latencyMs}ms"
                val (color, bg, quality) = when {
                    status.latencyMs < 100 -> Triple(
                        Color.parseColor("#4CAF50"), Color.parseColor("#E8F5E9"), "优秀"
                    )
                    status.latencyMs < 300 -> Triple(
                        Color.parseColor("#FF9800"), Color.parseColor("#FFF3E0"), "良好"
                    )
                    else -> Triple(
                        Color.parseColor("#F44336"), Color.parseColor("#FFEBEE"), "较差"
                    )
                }
                latencyText.setTextColor(color)
                applyHeroColor(bg)
                setPill("已连接", color)
                qualityStatText.text = quality
            }
            else -> {
                latencyText.text = "测速中..."
                latencyText.setTextColor(Color.GRAY)
                applyHeroColor(Color.parseColor("#EEEEEE"))
                setPill("已连接", Color.parseColor("#9E9E9E"))
                qualityStatText.text = "--"
            }
        }
    }

    private fun applyHeroColor(color: Int) {
        heroCard.setCardBackgroundColor(color)
    }

    private fun setPill(text: String, color: Int) {
        statusPill.text = text
        statusPill.setTextColor(color)
        (statusPill.background as GradientDrawable).setColor(withAlpha(color, 30))
    }

    private fun withAlpha(color: Int, alphaPercent: Int): Int {
        val alpha = (255 * alphaPercent / 100)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}