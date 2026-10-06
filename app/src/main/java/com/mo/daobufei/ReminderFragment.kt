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
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

// 自定义提醒:自选大标题/小标题/时间,到点弹出系统通知,每日重复;支持修改和删除
class ReminderFragment : Fragment() {

    private var selectedHour = 8
    private var selectedMinute = 0
    private var editingId: Long? = null    // 非空 = 正在修改这条提醒

    private lateinit var titleInput: TextInputEditText
    private lateinit var subtitleInput: TextInputEditText
    private lateinit var timeText: TextView
    private lateinit var editStateRow: LinearLayout
    private lateinit var actionButton: MaterialButton
    private lateinit var listContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            setPadding(context.dp(20), context.dp(12), context.dp(20), context.dp(20))
            hideKeyboardOnTouchOutside()
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(buildHeaderCard(context))
        root.addView(buildFormCard(context))
        root.addView(buildListCard(context))

        scrollView.addView(root)
        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun buildHeaderCard(context: android.content.Context): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.dp(28).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(context.color(R.color.accent_container))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(14)) }
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(context.dp(24), context.dp(28), context.dp(24), context.dp(28))
        }
        val iconCircle = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(72), context.dp(72))
            background = context.circleDrawable(context.color(R.color.card_bg))
        }
        val icon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(context.dp(36), context.dp(36), android.view.Gravity.CENTER)
            setImageResource(R.drawable.ic_alarm)
            imageTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.accent))
        }
        iconCircle.addView(icon)
        val titleText = TextView(context).apply {
            text = "自定义提醒"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.on_accent_container))
            setPadding(0, context.dp(14), 0, context.dp(6))
        }
        val descText = TextView(context).apply {
            text = "到点弹出系统通知 · 每日重复 · 可随时修改"
            textSize = 13f
            setTextColor(context.color(R.color.on_accent_container))
        }
        col.addView(iconCircle)
        col.addView(titleText)
        col.addView(descText)
        card.addView(col)
        return card
    }

    private fun buildFormCard(context: android.content.Context): MaterialCardView {
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

        // 编辑状态行:修改已有提醒时显示
        editStateRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(0, 0, 0, context.dp(10))
        }
        editStateRow.addView(TextView(context).apply {
            text = "正在修改已有提醒"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.status_warn))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        editStateRow.addView(TextView(context).apply {
            text = "取消"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_secondary))
            setPadding(context.dp(10), context.dp(4), context.dp(10), context.dp(4))
            background = context.pillDrawable(context.color(R.color.bg_idle))
            setOnClickListener { exitEditMode() }
        })
        col.addView(editStateRow)

        col.addView(TextView(context).apply {
            text = "提醒内容"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
            setPadding(0, 0, 0, context.dp(8))
        })

        val tilTitle = TextInputLayout(context).apply {
            hint = "大标题(必填)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(10)) }
        }
        titleInput = TextInputEditText(context)
        tilTitle.addView(titleInput)
        col.addView(tilTitle)

        val tilSubtitle = TextInputLayout(context).apply {
            hint = "小标题(选填)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(10)) }
        }
        subtitleInput = TextInputEditText(context)
        tilSubtitle.addView(subtitleInput)
        col.addView(tilSubtitle)

        // 时间选择行
        val timeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(4), 0, context.dp(14))
        }
        timeRow.addView(TextView(context).apply {
            text = "提醒时间"
            textSize = 15f
            setTextColor(context.color(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        timeText = TextView(context).apply {
            text = formatTime(selectedHour, selectedMinute)
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.accent))
            setPadding(context.dp(14), context.dp(8), context.dp(14), context.dp(8))
            background = context.pillDrawable(context.color(R.color.accent_container))
            setOnClickListener {
                showTimePickerDialog(context, selectedHour, selectedMinute) { h, m ->
                    selectedHour = h
                    selectedMinute = m
                    timeText.text = formatTime(h, m)
                }
            }
        }
        timeRow.addView(timeText)
        col.addView(timeRow)

        actionButton = MaterialButton(context).apply {
            text = "添加提醒"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { saveReminder() }
        }
        col.addView(actionButton)
        card.addView(col)
        return card
    }

    private fun buildListCard(context: android.content.Context): MaterialCardView {
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
        }
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(8))
        }
        col.addView(TextView(context).apply {
            text = "已设置的提醒(点一下可修改)"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
            setPadding(0, 0, 0, context.dp(8))
        })
        listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        col.addView(listContainer)
        card.addView(col)
        return card
    }

    private fun saveReminder() {
        val context = requireContext()
        val title = titleInput.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            Toast.makeText(context, "先填写大标题", Toast.LENGTH_SHORT).show()
            return
        }
        val id = editingId ?: System.currentTimeMillis()
        val reminder = Reminder(
            id = id,
            title = title,
            subtitle = subtitleInput.text?.toString()?.trim().orEmpty(),
            hour = selectedHour,
            minute = selectedMinute
        )
        // 修改:先取消旧闹钟再重排(同一 id,FLAG_UPDATE_CURRENT 也会覆盖,显式取消更稳)
        if (editingId != null) {
            ReminderScheduler.cancel(context, id)
            ReminderStore.remove(context, id)
        }
        ReminderStore.add(context, reminder)
        ReminderScheduler.schedule(context, reminder)
        Toast.makeText(
            context, "已设置,每天 ${formatTime(reminder.hour, reminder.minute)} 提醒", Toast.LENGTH_SHORT
        ).show()
        exitEditMode()
        titleInput.setText("")
        subtitleInput.setText("")
        refreshList()
    }

    // 点列表条目:载入表单进入修改模式
    private fun enterEditMode(reminder: Reminder) {
        editingId = reminder.id
        titleInput.setText(reminder.title)
        subtitleInput.setText(reminder.subtitle)
        selectedHour = reminder.hour
        selectedMinute = reminder.minute
        timeText.text = formatTime(reminder.hour, reminder.minute)
        actionButton.text = "保存修改"
        editStateRow.visibility = View.VISIBLE
        refreshList()
    }

    private fun exitEditMode() {
        editingId = null
        actionButton.text = "添加提醒"
        editStateRow.visibility = View.GONE
        titleInput.setText("")
        subtitleInput.setText("")
        selectedHour = 8
        selectedMinute = 0
        if (::timeText.isInitialized) {
            timeText.text = formatTime(selectedHour, selectedMinute)
        }
        refreshList()
    }

    private fun refreshList() {
        if (!::listContainer.isInitialized) return
        val context = requireContext()
        listContainer.removeAllViews()
        val items = ReminderStore.list(context)
        if (items.isEmpty()) {
            listContainer.addView(TextView(context).apply {
                text = "还没有提醒,先在上方创建一个"
                textSize = 13f
                setTextColor(context.color(R.color.text_secondary))
                setPadding(0, context.dp(4), 0, context.dp(12))
            })
            return
        }
        items.forEach { r ->
            listContainer.addView(buildReminderRow(context, r))
        }
    }

    private fun buildReminderRow(context: android.content.Context, r: Reminder): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(10), 0, context.dp(10))
            // 点击条目载入修改;删除按钮单独处理
            setOnClickListener {
                enterEditMode(r)
                Toast.makeText(context, "已载入,改完点\"保存修改\"", Toast.LENGTH_SHORT).show()
            }
        }

        val iconBox = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40))
            background = context.roundDrawable(context.color(R.color.accent_container), 12f)
        }
        val icon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(context.dp(20), context.dp(20), android.view.Gravity.CENTER)
            setImageResource(R.drawable.ic_alarm)
            imageTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.accent))
        }
        iconBox.addView(icon)

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(context.dp(12), 0, context.dp(8), 0) }
        }
        val titleText = TextView(context).apply {
            text = r.title
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        }
        textCol.addView(titleText)
        if (r.subtitle.isNotEmpty()) {
            textCol.addView(TextView(context).apply {
                text = r.subtitle
                textSize = 12f
                setTextColor(context.color(R.color.text_secondary))
                setPadding(0, context.dp(2), 0, 0)
            })
        }
        textCol.addView(TextView(context).apply {
            text = "每天 ${formatTime(r.hour, r.minute)}"
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.accent))
            setPadding(context.dp(8), context.dp(3), context.dp(8), context.dp(3))
            background = context.pillDrawable(context.color(R.color.accent_container))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, context.dp(5), 0, 0) }
        })

        val deleteIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(24), context.dp(24))
            setImageResource(R.drawable.ic_close)
            imageTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.text_secondary))
            setOnClickListener {
                ReminderScheduler.cancel(context, r.id)
                ReminderStore.remove(context, r.id)
                if (editingId == r.id) exitEditMode()
                Toast.makeText(context, "已删除该提醒", Toast.LENGTH_SHORT).show()
                refreshList()
            }
        }

        row.addView(iconBox)
        row.addView(textCol)
        row.addView(deleteIcon)
        return row
    }

    private fun formatTime(hour: Int, minute: Int) =
        String.format(Locale.ROOT, "%02d:%02d", hour, minute)
}
