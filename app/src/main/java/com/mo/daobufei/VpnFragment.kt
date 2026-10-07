package com.mo.daobufei

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
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
    private lateinit var latencyChart: LatencyChartView
    private var permissionCard: MaterialCardView? = null

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

        // ---------- 权限引导卡(权限齐全时不显示) ----------
        permissionCard = buildPermissionCard(context)
        if (shouldShowPermissionCard(context)) {
            root.addView(permissionCard)
        }

        // ---------- 顶部状态大卡片 ----------
        heroCard = MaterialCardView(context).apply {
            radius = context.dp(28).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.bg_idle))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(14)) }
        }
        val heroContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(context.dp(24), context.dp(32), context.dp(24), context.dp(36))
        }

        // 国旗:圆形裁剪 + 一圈卡片色描边
        val flagRing = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(112), context.dp(112))
            background = context.circleDrawable(context.color(R.color.card_bg))
            setPadding(context.dp(5), context.dp(5), context.dp(5), context.dp(5))
            clipToBackgroundShape()
        }
        flagImage = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = context.circleDrawable(context.color(R.color.card_bg_muted))
            clipToBackgroundShape()
        }
        flagRing.addView(flagImage)

        latencyText = TextView(context).apply {
            textSize = 46f
            setTypeface(typeface, Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, context.dp(20), 0, context.dp(10))
        }
        statusPill = TextView(context).apply {
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(context.dp(14), context.dp(6), context.dp(14), context.dp(6))
        }
        heroContent.addView(flagRing)
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
            ).apply { setMargins(0, 0, 0, context.dp(14)) }
        }
        val (countryCard, countryValueView) =
            buildStatCard(context, R.drawable.ic_globe, context.getString(R.string.stat_country))
        val (qualityCard, qualityValueView) =
            buildStatCard(context, R.drawable.ic_speed, context.getString(R.string.stat_quality))
        countryStatText = countryValueView
        qualityStatText = qualityValueView
        (countryCard.layoutParams as LinearLayout.LayoutParams).apply {
            width = 0; weight = 1f; marginEnd = context.dp(7)
        }
        (qualityCard.layoutParams as LinearLayout.LayoutParams).apply {
            width = 0; weight = 1f; marginStart = context.dp(7)
        }
        statRow.addView(countryCard)
        statRow.addView(qualityCard)
        root.addView(statRow)

        // ---------- 延迟趋势 ----------
        root.addView(buildTrendCard(context))

        // ---------- 底部设置列表行 ----------
        root.addView(buildSwitchRow(context))

        // ---------- 小功能入口 ----------
        root.addView(buildDiagnosisRow(context))

        scrollView.addView(root)
        return scrollView
    }

    // ==================== 权限引导 ====================

    private fun shouldShowPermissionCard(context: android.content.Context): Boolean {
        val notificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val promotedEnabled = try {
            context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications()
        } catch (e: Exception) {
            true
        }
        return !notificationEnabled || !promotedEnabled
    }

    // 通知/流体云权限缺失时的引导卡:一键跳系统设置,比原来的 Toast 给出明确去处
    private fun buildPermissionCard(context: android.content.Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.bg_warn))
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
            text = context.getString(R.string.permission_card_title)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        }
        val descText = TextView(context).apply {
            text = context.getString(R.string.permission_card_desc)
            textSize = 13f
            setTextColor(context.color(R.color.text_secondary))
            setPadding(0, context.dp(6), 0, context.dp(12))
        }
        val button = MaterialButton(context).apply {
            text = context.getString(R.string.permission_card_action)
            textSize = 14f
            setOnClickListener {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
        col.addView(titleText)
        col.addView(descText)
        col.addView(button)
        card.addView(col)
        return card
    }

    // ==================== 常规界面 ====================

    // 生成一张"图标 + 标题 + 数值"的小卡片,返回卡片本身和数值 TextView(方便后续更新内容)
    private fun buildStatCard(
        context: android.content.Context, iconRes: Int, title: String
    ): Pair<MaterialCardView, TextView> {
        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16))
        }
        val titleRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(16), context.dp(16))
            setImageResource(iconRes)
            imageTintList = android.content.res.ColorStateList.valueOf(
                context.color(R.color.text_secondary)
            )
        }
        val titleText = TextView(context).apply {
            text = title
            textSize = 12f
            setTextColor(context.color(R.color.text_secondary))
            setPadding(context.dp(6), 0, 0, 0)
        }
        val valueText = TextView(context).apply {
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
            setPadding(0, context.dp(10), 0, 0)
        }
        titleRow.addView(icon)
        titleRow.addView(titleText)
        col.addView(titleRow)
        col.addView(valueText)
        card.addView(col)
        return card to valueText
    }

    // 设置列表行:图标 + 标题 + 右侧开关
    private fun buildSwitchRow(context: android.content.Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
        }
        val iconBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40))
            background = context.roundDrawable(context.color(R.color.accent_container), 12f)
        }
        val icon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(context.dp(20), context.dp(20), android.view.Gravity.CENTER)
            setImageResource(R.drawable.ic_bell)
            imageTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.accent))
        }
        iconBox.addView(icon)
        val label = TextView(context).apply {
            text = context.getString(R.string.switch_idle_notification)
            textSize = 15f
            setTextColor(context.color(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(context.dp(12), 0, 0, 0) }
        }
        val switch = MaterialSwitch(context).apply {
            isChecked = SettingsPrefs.isIdleNotificationEnabled(context)
            setOnCheckedChangeListener { _, isChecked ->
                SettingsPrefs.setIdleNotificationEnabled(context, isChecked)
                // 立即生效:让 Service 按新配置重发当前通知,不等下一次状态变化
                context.startService(
                    Intent(context, VpnMonitorService::class.java)
                        .setAction(VpnMonitorService.ACTION_APPLY_IDLE_SETTING)
                )
            }
        }
        row.addView(iconBox)
        row.addView(label)
        row.addView(switch)
        card.addView(row)
        return card
    }

    // 延迟趋势卡片:自绘折线图,数据来自 Service 的最近 16 次采样
    private fun buildTrendCard(context: android.content.Context): MaterialCardView {
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
            text = context.getString(R.string.trend_title)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        }
        val captionText = TextView(context).apply {
            text = context.getString(R.string.trend_caption)
            textSize = 11f
            setTextColor(context.color(R.color.text_secondary))
            setPadding(0, context.dp(2), 0, context.dp(10))
        }
        latencyChart = LatencyChartView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                context.dp(120)
            )
            background = context.roundDrawable(context.color(R.color.card_bg_muted), 12f)
            setPadding(context.dp(8), context.dp(8), context.dp(8), context.dp(8))
        }
        col.addView(titleText)
        col.addView(captionText)
        col.addView(latencyChart)
        card.addView(col)
        return card
    }

    // 小功能入口:IP 诊断(原独立 tab 收进首页)
    private fun buildDiagnosisRow(context: android.content.Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.card_bg))
            strokeColor = context.color(R.color.divider)
            strokeWidth = context.dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, context.dp(14), 0, 0) }
            setOnClickListener {
                context.startActivity(Intent(context, DiagnosisActivity::class.java))
            }
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
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
            text = context.getString(R.string.diagnosis_title)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        }
        val descText = TextView(context).apply {
            text = context.getString(R.string.diagnosis_desc)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                VpnMonitorService.vpnStatus.collect { status ->
                    updateUi(status)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                VpnMonitorService.latencyHistory.collect { history ->
                    if (::latencyChart.isInitialized) latencyChart.setData(history)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置回来时复检权限,决定引导卡去留
        permissionCard?.visibility =
            if (shouldShowPermissionCard(requireContext())) View.VISIBLE else View.GONE
    }

    private fun updateUi(status: VpnStatus) {
        val context = requireContext()
        val dash = context.getString(R.string.dash)

        if (!status.isConnected) {
            flagImage.setImageDrawable(null)
            setBigText(dash, null, context.color(R.color.text_secondary))
            applyHeroColor(context.color(R.color.bg_idle))
            setPill(context.getString(R.string.status_disconnected), context.color(R.color.status_idle))
            countryStatText.text = dash
            qualityStatText.text = dash
            return
        }

        if (status.isPaused) {
            setBigText(dash, null, context.color(R.color.text_secondary))
            applyHeroColor(context.color(R.color.bg_idle))
            setPill(context.getString(R.string.status_paused_monitor), context.color(R.color.status_idle))
            return
        }

        val flagResId = FlagUtils.getFlagDrawableResId(status.countryCode)
        if (flagResId != 0) {
            flagImage.setImageResource(flagResId)
        } else {
            flagImage.setImageDrawable(null)
        }
        countryStatText.text = status.countryName

        when {
            status.isTimeout -> {
                setBigText(context.getString(R.string.status_timeout), null, context.color(R.color.status_bad))
                applyHeroColor(context.color(R.color.bg_bad))
                setPill(context.getString(R.string.status_node_unavailable), context.color(R.color.status_bad))
                qualityStatText.text = context.getString(R.string.quality_bad)
            }
            status.latencyMs != null -> {
                setBigText("${status.latencyMs}", context.getString(R.string.ms_unit), context.color(R.color.text_primary))
                val (stateColor, heroBg, quality) = when {
                    status.latencyMs < 100 -> Triple(
                        context.color(R.color.status_good),
                        context.color(R.color.bg_good), context.getString(R.string.quality_good)
                    )
                    status.latencyMs < 300 -> Triple(
                        context.color(R.color.status_warn),
                        context.color(R.color.bg_warn), context.getString(R.string.quality_fair)
                    )
                    else -> Triple(
                        context.color(R.color.status_bad),
                        context.color(R.color.bg_bad), context.getString(R.string.quality_bad)
                    )
                }
                applyHeroColor(heroBg)
                setPill(context.getString(R.string.status_connected), stateColor)
                qualityStatText.text = quality
            }
            else -> {
                setBigText(context.getString(R.string.status_measuring), null, context.color(R.color.text_secondary))
                applyHeroColor(context.color(R.color.bg_idle))
                setPill(context.getString(R.string.status_connected), context.color(R.color.status_idle))
                qualityStatText.text = dash
            }
        }
    }

    // 大号数字 + 小号单位(如 "128 ms");unit 为空则整段大号显示
    private fun setBigText(value: String, unit: String?, valueColor: Int) {
        latencyText.setTextColor(valueColor)
        if (unit == null) {
            latencyText.text = value
            return
        }
        val sb = SpannableStringBuilder(value)
        val start = sb.length
        sb.append(unit)
        sb.setSpan(
            RelativeSizeSpan(0.42f), start, sb.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        sb.setSpan(
            ForegroundColorSpan(requireContext().color(R.color.text_secondary)),
            start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        latencyText.text = sb
    }

    private fun applyHeroColor(color: Int) {
        heroCard.setCardBackgroundColor(color)
    }

    private fun setPill(text: String, color: Int) {
        statusPill.text = text
        statusPill.setTextColor(color)
        statusPill.background = requireContext().pillDrawable(withAlpha(color, 0.14f))
    }

    private fun withAlpha(color: Int, fraction: Float): Int {
        val alpha = (255 * fraction).toInt()
        return android.graphics.Color.argb(
            alpha, android.graphics.Color.red(color),
            android.graphics.Color.green(color), android.graphics.Color.blue(color)
        )
    }
}
