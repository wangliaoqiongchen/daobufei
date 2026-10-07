package com.mo.daobufei

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

// IP 诊断页:从首页"IP 诊断"卡片进入,查看当前出口 IP 与路由
class DiagnosisActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var errorView: LinearLayout
    private var backCallback: OnBackPressedCallback? = null
    private val targetUrl = "https://ipok.io/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.screen_bg))
        }

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    // 只有主页面加载失败才显示错误页,避免页面里某个小图标/子资源加载失败也被当成整页出错
                    if (request?.isForMainFrame == true) {
                        showError()
                    }
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    // 有历史可回退时才接管返回键,否则让返回键走默认行为(关闭页面)
                    backCallback?.isEnabled = webView.canGoBack()
                }
            }
        }

        backCallback = object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
            }
        }

        errorView = buildErrorView()
        errorView.visibility = android.view.View.GONE

        root.addView(webView)
        root.addView(errorView)
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, backCallback!!)
        webView.loadUrl(targetUrl)
    }

    override fun onPause() {
        super.onPause()
        // 退到后台时暂停网页 JS/定时器
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    private fun buildErrorView(): LinearLayout {
        val context = this
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(context.dp(24), context.dp(24), context.dp(24), context.dp(24))

            val iconCircle = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(context.dp(72), context.dp(72))
                background = context.circleDrawable(context.color(R.color.bg_idle))
            }
            val icon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(context.dp(32), context.dp(32), android.view.Gravity.CENTER)
                setImageResource(R.drawable.ic_globe)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    context.color(R.color.status_idle)
                )
            }
            iconCircle.addView(icon)

            val titleText = TextView(context).apply {
                text = context.getString(R.string.diag_load_failed)
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(context.color(R.color.text_primary))
                gravity = android.view.Gravity.CENTER
                setPadding(0, context.dp(16), 0, context.dp(4))
            }
            val descText = TextView(context).apply {
                text = context.getString(R.string.diag_check_network)
                textSize = 14f
                setTextColor(context.color(R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, context.dp(20))
            }
            val retryButton = MaterialButton(context).apply {
                text = context.getString(R.string.diag_retry)
                // 显式调 setIcon:外层的局部变量 icon 会遮蔽 MaterialButton 的 icon 属性
                setIcon(context.getDrawable(R.drawable.ic_refresh))
                setOnClickListener {
                    errorView.visibility = android.view.View.GONE
                    webView.visibility = android.view.View.VISIBLE
                    webView.loadUrl(targetUrl)
                }
            }

            addView(iconCircle)
            addView(titleText)
            addView(descText)
            addView(retryButton)
        }
    }

    private fun showError() {
        webView.visibility = android.view.View.GONE
        errorView.visibility = android.view.View.VISIBLE
    }
}
