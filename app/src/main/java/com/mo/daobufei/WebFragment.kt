package com.mo.daobufei

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class WebFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var errorView: LinearLayout
    private val targetUrl = "https://ipok.io/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val root = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        webView = WebView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
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
            }
        }

        errorView = buildErrorView(context)
        errorView.visibility = View.GONE

        root.addView(webView)
        root.addView(errorView)

        webView.loadUrl(targetUrl)

        return root
    }

    private fun buildErrorView(context: android.content.Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(64, 64, 64, 64)

            val titleText = TextView(context).apply {
                text = "页面加载失败"
                textSize = 18f
                gravity = android.view.Gravity.CENTER
            }
            val descText = TextView(context).apply {
                text = "请检查网络连接后重试"
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 32)
            }
            val retryButton = MaterialButton(context).apply {
                text = "重新加载"
                setOnClickListener {
                    errorView.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    webView.loadUrl(targetUrl)
                }
            }

            addView(titleText)
            addView(descText)
            addView(retryButton)
        }
    }

    private fun showError() {
        webView.visibility = View.GONE
        errorView.visibility = View.VISIBLE
    }
}