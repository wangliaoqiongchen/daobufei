package com.mo.daobufei

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

// 简易延迟趋势折线图:最近 N 次测量,纵轴自适应,圆点按延迟分色
class LatencyChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var samples: List<Long> = emptyList()
    private val density = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = context.color(R.color.accent)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = withAlpha(context.color(R.color.accent), 0.16f)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
        color = context.color(R.color.divider)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.color(R.color.text_secondary)
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
    }
    private val colorGood = context.color(R.color.status_good)
    private val colorWarn = context.color(R.color.status_warn)
    private val colorBad = context.color(R.color.status_bad)
    private val path = Path()

    fun setData(data: List<Long>) {
        samples = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // 底线
        canvas.drawLine(0f, h - density, w, h - density, gridPaint)

        if (samples.size < 2) {
            canvas.drawText("连接后开始记录延迟趋势", w / 2, h / 2, textPaint)
            return
        }

        val top = 10f * density
        val bottom = h - 8f * density
        val maxV = (samples.max() * 1.25).coerceAtLeast(120.0)
        val stepX = w / (samples.size - 1)
        fun y(v: Long): Float = (bottom - (v / maxV) * (bottom - top)).toFloat()

        // 折线
        path.reset()
        samples.forEachIndexed { i, v ->
            if (i == 0) path.moveTo(i * stepX, y(v)) else path.lineTo(i * stepX, y(v))
        }
        // 面积填充
        val fill = Path(path)
        fill.lineTo(w, bottom)
        fill.lineTo(0f, bottom)
        fill.close()
        canvas.drawPath(fill, fillPaint)
        canvas.drawPath(path, linePaint)

        // 圆点按延迟分色
        samples.forEachIndexed { i, v ->
            dotPaint.color = when {
                v < 100 -> colorGood
                v < 300 -> colorWarn
                else -> colorBad
            }
            canvas.drawCircle(i * stepX, y(v), 2.5f * density, dotPaint)
        }
    }

    private fun withAlpha(color: Int, fraction: Float): Int {
        val alpha = (255 * fraction).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
