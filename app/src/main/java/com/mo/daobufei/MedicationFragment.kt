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
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.time.LocalDate
import java.time.YearMonth

// 用药提醒:添加药品(每天多个服药时间) → 到点通知提醒(带"已服"按钮) →
// 今日用药清单确认 → 月视图日历按真实服药记录渲染三色
class MedicationFragment : Fragment() {

    private lateinit var nameInput: TextInputEditText
    private lateinit var noteInput: TextInputEditText
    private lateinit var draftTimesRow: LinearLayout
    private lateinit var todayContainer: LinearLayout
    private lateinit var medListContainer: LinearLayout
    private var calendarColumn: LinearLayout? = null

    private val draftTimes = sortedSetOf<String>()

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

        // ---------- 头部 ----------
        root.addView(buildHeaderCard(context))

        // ---------- 今日用药 ----------
        root.addView(buildTodayCard(context))

        // ---------- 药品管理 ----------
        root.addView(buildManageCard(context))

        // ---------- 服药日历 ----------
        root.addView(buildCalendarCard(context))

        scrollView.addView(root)
        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun refreshAll() {
        refreshToday()
        refreshMedList()
        refreshCalendar()
    }

    // ==================== 头部 ====================

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
            setImageResource(R.drawable.ic_medication)
            imageTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.accent))
        }
        iconCircle.addView(icon)
        val titleText = TextView(context).apply {
            text = context.getString(R.string.med_title)
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.on_accent_container))
            setPadding(0, context.dp(14), 0, context.dp(6))
        }
        val descText = TextView(context).apply {
            text = context.getString(R.string.med_header_desc)
            textSize = 13f
            setTextColor(context.color(R.color.on_accent_container))
        }
        col.addView(iconCircle)
        col.addView(titleText)
        col.addView(descText)
        card.addView(col)
        return card
    }

    // ==================== 今日用药 ====================

    private fun buildTodayCard(context: android.content.Context): MaterialCardView {
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
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(8))
        }
        col.addView(
            cardTitle(
                context,
                context.getString(R.string.med_today_title),
                context.getString(R.string.med_today_caption)
            )
        )
        todayContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        col.addView(todayContainer)
        card.addView(col)
        return card
    }

    private fun refreshToday() {
        if (!::todayContainer.isInitialized) return
        val context = requireContext()
        todayContainer.removeAllViews()
        val today = LocalDate.now()
        val meds = MedicationStore.meds(context)
        if (meds.isEmpty()) {
            todayContainer.addView(hintText(context, context.getString(R.string.med_today_empty)))
            return
        }
        // 汇总今天所有服药:按时间排序
        val doses = meds.flatMap { med ->
            med.times.map { time ->
                Triple(time, med, MedicationStore.doseState(context, today, time, med.id))
            }
        }.sortedBy { it.first }

        doses.forEach { dose ->
            todayContainer.addView(buildTodayRow(context, today, dose.first, dose.second, dose.third))
        }
    }

    private fun buildTodayRow(
        context: android.content.Context, today: LocalDate, time: String, med: Medication, state: Int
    ): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(8), 0, context.dp(8))
        }
        val timeChip = TextView(context).apply {
            text = time
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.on_accent_container))
            setPadding(context.dp(10), context.dp(5), context.dp(10), context.dp(5))
            background = context.pillDrawable(context.color(R.color.accent_container))
        }
        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(context.dp(12), 0, context.dp(8), 0) }
        }
        textCol.addView(TextView(context).apply {
            text = med.name
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        })
        if (med.note.isNotEmpty()) {
            textCol.addView(TextView(context).apply {
                text = med.note
                textSize = 11f
                setTextColor(context.color(R.color.text_secondary))
                setPadding(0, context.dp(2), 0, 0)
            })
        }

        // 状态胶囊
        val (statusText, statusColor) = when (state) {
            MedicationStore.STATE_TAKEN -> context.getString(R.string.status_taken) to context.color(R.color.status_good)
            MedicationStore.STATE_MISSED -> context.getString(R.string.status_missed) to context.color(R.color.status_bad)
            else -> context.getString(R.string.status_pending) to context.color(R.color.status_idle)
        }
        val statusPill = TextView(context).apply {
            text = statusText
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(statusColor)
            setPadding(context.dp(8), context.dp(3), context.dp(8), context.dp(3))
            background = context.pillDrawable(withAlpha(statusColor, 0.14f))
        }

        row.addView(timeChip)
        row.addView(textCol)
        row.addView(statusPill)

        // 未服的给一个操作按钮(待服="已服",超时="补服")
        if (state != MedicationStore.STATE_TAKEN) {
            val actionButton = MaterialButton(context).apply {
                text = if (state == MedicationStore.STATE_MISSED) {
                    context.getString(R.string.med_retake)
                } else {
                    context.getString(R.string.status_taken)
                }
                textSize = 12f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(context.dp(8), 0, 0, 0) }
                setOnClickListener {
                    MedicationStore.markTaken(context, today, time, med.id)
                    NotificationManagerCompat.from(context)
                        .cancel(MedicationStore.doseNotificationId(time, med.id))
                    refreshAll()
                }
            }
            row.addView(actionButton)
        }
        return row
    }

    // ==================== 药品管理 ====================

    private fun buildManageCard(context: android.content.Context): MaterialCardView {
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
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(8))
        }
        col.addView(
            cardTitle(
                context,
                context.getString(R.string.med_manage_title),
                context.getString(R.string.med_manage_caption)
            )
        )

        val tilName = TextInputLayout(context).apply {
            hint = context.getString(R.string.med_name_hint)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(10)) }
        }
        nameInput = TextInputEditText(context)
        tilName.addView(nameInput)
        col.addView(tilName)

        val tilNote = TextInputLayout(context).apply {
            hint = context.getString(R.string.med_note_hint)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(10)) }
        }
        noteInput = TextInputEditText(context)
        tilNote.addView(noteInput)
        col.addView(tilNote)

        // 服药时间行
        val timeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(4), 0, context.dp(8))
        }
        timeRow.addView(TextView(context).apply {
            text = context.getString(R.string.med_times_label)
            textSize = 15f
            setTextColor(context.color(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        timeRow.addView(MaterialButton(context).apply {
            text = context.getString(R.string.med_add_time)
            textSize = 13f
            isAllCaps = false
            setOnClickListener {
                showTimePickerDialog(context, 8, 0) { h, m ->
                    draftTimes.add(TimeUtil.hhmm(h, m))
                    redrawDraftTimes()
                }
            }
        })
        col.addView(timeRow)

        draftTimesRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, context.dp(10))
        }
        col.addView(draftTimesRow)
        redrawDraftTimes()

        col.addView(MaterialButton(context).apply {
            text = context.getString(R.string.med_add_button)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, context.dp(8)) }
            setOnClickListener { addMedication() }
        })

        col.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, context.dp(1)
            ).apply { setMargins(0, 0, 0, context.dp(8)) }
            setBackgroundColor(context.color(R.color.divider))
        })

        medListContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        col.addView(medListContainer)
        card.addView(col)
        return card
    }

    private fun redrawDraftTimes() {
        val context = requireContext()
        draftTimesRow.removeAllViews()
        if (draftTimes.isEmpty()) {
            draftTimesRow.addView(hintText(context, context.getString(R.string.med_times_empty_hint)))
            return
        }
        draftTimesRow.addView(TextView(context).apply {
            text = context.getString(R.string.med_times_selected, draftTimes.joinToString("  "))
            textSize = 12f
            setTextColor(context.color(R.color.text_secondary))
        })
        draftTimes.forEach { t ->
            draftTimesRow.addView(TextView(context).apply {
                text = t
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(context.color(R.color.accent))
                setPadding(context.dp(8), context.dp(4), context.dp(8), context.dp(4))
                background = context.pillDrawable(context.color(R.color.accent_container))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(context.dp(6), 0, 0, 0) }
                setOnClickListener {
                    draftTimes.remove(t)
                    redrawDraftTimes()
                }
            })
        }
    }

    private fun addMedication() {
        val context = requireContext()
        val name = nameInput.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.med_name_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (draftTimes.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.med_time_required), Toast.LENGTH_SHORT).show()
            return
        }
        val med = Medication(
            id = System.currentTimeMillis(),
            name = name,
            note = noteInput.text?.toString()?.trim().orEmpty(),
            times = draftTimes.toList()
        )
        MedicationStore.add(context, med)   // 内部会把每个时间都挂上每日闹钟
        Toast.makeText(
            context,
            context.getString(R.string.med_added, med.name, med.times.joinToString("/")),
            Toast.LENGTH_LONG
        ).show()
        nameInput.setText("")
        noteInput.setText("")
        draftTimes.clear()
        redrawDraftTimes()
        refreshAll()
    }

    private fun refreshMedList() {
        if (!::medListContainer.isInitialized) return
        val context = requireContext()
        medListContainer.removeAllViews()
        val items = MedicationStore.meds(context)
        if (items.isEmpty()) {
            medListContainer.addView(hintText(context, context.getString(R.string.med_list_empty)))
            return
        }
        items.forEach { med ->
            medListContainer.addView(buildMedRow(context, med))
        }
    }

    private fun buildMedRow(context: android.content.Context, med: Medication): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(8), 0, context.dp(8))
        }
        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { setMargins(0, 0, context.dp(8), 0) }
        }
        textCol.addView(TextView(context).apply {
            text = med.name
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
        })
        if (med.note.isNotEmpty()) {
            textCol.addView(TextView(context).apply {
                text = med.note
                textSize = 11f
                setTextColor(context.color(R.color.text_secondary))
                setPadding(0, context.dp(2), 0, 0)
            })
        }
        textCol.addView(TextView(context).apply {
            text = context.getString(R.string.daily_at, med.times.joinToString(" / "))
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.accent))
            setPadding(0, context.dp(4), 0, 0)
        })

        val deleteIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(24), context.dp(24))
            setImageResource(R.drawable.ic_close)
            imageTintList = android.content.res.ColorStateList.valueOf(context.color(R.color.text_secondary))
            setOnClickListener {
                MedicationStore.remove(context, med.id)
                Toast.makeText(context, context.getString(R.string.med_deleted, med.name), Toast.LENGTH_SHORT).show()
                refreshAll()
            }
        }

        row.addView(textCol)
        row.addView(deleteIcon)
        return row
    }

    // ==================== 服药日历(月视图,真实数据) ====================
    // 一格 = 一天,竖条 = 当天计划的每次服药(几顿就几条):
    // 绿 = 已服,红 = 超时未服,灰 = 未到时间
    private fun buildCalendarCard(context: android.content.Context): MaterialCardView {
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
        calendarColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(16))
        }
        card.addView(calendarColumn)
        refreshCalendar()
        return card
    }

    private fun refreshCalendar() {
        val column = calendarColumn ?: return
        val context = requireContext()
        column.removeAllViews()

        column.addView(
            cardTitle(
                context,
                context.getString(R.string.med_calendar_title),
                context.getString(R.string.med_calendar_caption)
            )
        )
        // 图例紧跟说明行
        column.addView(buildLegendRow(context))

        val today = LocalDate.now()
        val month = YearMonth.from(today)

        column.addView(TextView(context).apply {
            text = context.getString(R.string.med_month_label, month.year, month.monthValue)
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(context.color(R.color.text_primary))
            setPadding(0, context.dp(4), 0, context.dp(8))
        })

        val weekHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, context.dp(2))
        }
        "一二三四五六日".forEach { ch ->
            weekHeader.addView(TextView(context).apply {
                text = ch.toString()
                textSize = 11f
                gravity = android.view.Gravity.CENTER
                setTextColor(context.color(R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply { setMargins(context.dp(2), 0, context.dp(2), 0) }
            })
        }
        column.addView(weekHeader)

        val meds = MedicationStore.meds(context)
        val leadingBlank = today.dayOfWeek.value - 1
        val daysInMonth = month.lengthOfMonth()
        val rows = (leadingBlank + daysInMonth + 6) / 7
        var day = 1
        for (row in 0 until rows) {
            val rowView = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, context.dp(2), 0, context.dp(2))
            }
            for (cellIndex in row * 7 until row * 7 + 7) {
                if (cellIndex < leadingBlank || day > daysInMonth) {
                    rowView.addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, context.dp(26), 1f).apply {
                            setMargins(context.dp(2), 0, context.dp(2), 0)
                        }
                    })
                } else {
                    rowView.addView(buildDayCell(context, day, today, meds))
                    day++
                }
            }
            column.addView(rowView)
        }
    }

    private fun buildLegendRow(context: android.content.Context): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, context.dp(10))
        }
        val items = listOf(
            R.color.status_good to context.getString(R.string.status_taken),
            R.color.status_bad to context.getString(R.string.status_missed),
            R.color.card_bg to context.getString(R.string.legend_upcoming)
        )
        items.forEach { (colorRes, label) ->
            row.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(context.dp(10), context.dp(10)).apply {
                    setMargins(0, 0, context.dp(4), 0)
                }
                background = context.roundDrawable(context.color(colorRes), 3f)
            })
            row.addView(TextView(context).apply {
                text = label
                textSize = 11f
                setTextColor(context.color(R.color.text_secondary))
                setPadding(0, 0, context.dp(10), 0)
            })
        }
        return row
    }

    // 一天的格子:圆角容器 + 每个计划服药一条竖条
    private fun buildDayCell(
        context: android.content.Context, day: Int, today: LocalDate, meds: List<Medication>
    ): View {
        val isToday = day == today.dayOfMonth
        val date = today.withDayOfMonth(day)
        val container = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, context.dp(26), 1f).apply {
                setMargins(context.dp(2), 0, context.dp(2), 0)
            }
            background = context.roundDrawable(
                if (isToday) context.color(R.color.accent_container)
                else context.color(R.color.card_bg_muted),
                6f
            )
            setPadding(context.dp(3), context.dp(3), context.dp(3), context.dp(3))
        }
        val strips = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        // 当天所有计划的服药,按时间排序,一次一条
        val doses = meds.flatMap { med -> med.times.map { time -> time to med } }
            .sortedBy { it.first }
        doses.forEach { (time, med) ->
            val color = when (MedicationStore.doseState(context, date, time, med.id)) {
                MedicationStore.STATE_TAKEN -> context.color(R.color.status_good)
                MedicationStore.STATE_MISSED -> context.color(R.color.status_bad)
                else -> context.color(R.color.card_bg)
            }
            strips.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
                ).apply { setMargins(context.dp(1), 0, context.dp(1), 0) }
                background = context.roundDrawable(color, 2f)
            })
        }
        container.addView(strips)
        return container
    }

    // ==================== 通用小件 ====================

    private fun cardTitle(context: android.content.Context, title: String, caption: String?): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, context.dp(8))
            addView(TextView(context).apply {
                text = title
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(context.color(R.color.text_primary))
            })
            if (caption != null) {
                addView(TextView(context).apply {
                    text = caption
                    textSize = 12f
                    setTextColor(context.color(R.color.text_secondary))
                    setPadding(0, context.dp(2), 0, context.dp(8))
                })
            }
        }
    }

    private fun hintText(context: android.content.Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            setTextColor(context.color(R.color.text_secondary))
            setPadding(0, context.dp(4), 0, context.dp(12))
        }
    }

    private fun withAlpha(color: Int, fraction: Float): Int {
        val alpha = (255 * fraction).toInt()
        return android.graphics.Color.argb(
            alpha, android.graphics.Color.red(color),
            android.graphics.Color.green(color), android.graphics.Color.blue(color)
        )
    }
}
