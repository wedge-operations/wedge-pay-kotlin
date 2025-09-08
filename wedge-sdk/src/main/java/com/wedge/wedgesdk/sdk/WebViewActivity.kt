package com.wedge.wedgesdk.sdk

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.json.JSONObject

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var hasResponded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val root = FrameLayout(this)
        setContentView(root)

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Evita que las barras de scroll reserven espacio
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }
        }
        root.addView(webView)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
            val tappable = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomSafe = listOf(
                sysBars.bottom,
                navBars.bottom,
                gestures.bottom,
                tappable.bottom,
                ime.bottom
            ).max()

            v.updatePadding(left = sysBars.left, top = sysBars.top, right = sysBars.right)
            webView.updatePadding(bottom = bottomSafe)

            insets
        }

        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val token = intent.getStringExtra("token") ?: run {
            OnboardingSDK.handleError("Missing token")
            finish()
            return
        }

        val env = intent.getStringExtra("env") ?: "sandbox"
        val type = intent.getStringExtra("type") ?: "onboarding"
        
        val baseUrl = when (env) {
            "production" -> "https://onboarding.wedge-can.com"
            "sandbox" -> "https://onboarding-sandbox.wedge-can.com"
            else -> "https://onboarding-integration.wedge-can.com"
        }
        val url = "$baseUrl?onboardingToken=$token&type=$type"

        webView.addJavascriptInterface(JSBridge(), "WedgeSDKAndroid")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                webView.evaluateJavascript("window.__ONBOARDING_TOKEN__ = '$token';", null)
                OnboardingSDK.handleLoad(url ?: "")
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (!hasResponded) {
                    OnboardingSDK.handleError(error.description.toString())
                    hasResponded = true
                    finish()
                }
            }
        }

        webView.loadUrl(url)
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("WedgeSDKAndroid")
        webView.stopLoading()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!hasResponded) {
            OnboardingSDK.handleExit("User closed the onboarding via back button")
            hasResponded = true
        }
        super.onBackPressed()
    }

    inner class JSBridge {
        private val mainHandler = Handler(Looper.getMainLooper())

        private fun finishActivity() {
            if (!isFinishing) {
                try {
                    finish()
                } catch (_: Exception) {
                    try {
                        finishAndRemoveTask()
                    } catch (_: Exception) {
                        try {
                            moveTaskToBack(true)
                        } catch (_: Exception) {
                            // no-op
                        }
                    }
                }
            }
        }

        @JavascriptInterface
        fun onSuccess(data: String) {
            mainHandler.post {
                if (!hasResponded && !isFinishing) {
                    hasResponded = true
                    OnboardingSDK.handleSuccess(data)
                    finishActivity()
                }
            }
        }

        @JavascriptInterface
        fun onExit(reason: String) {
            mainHandler.post {
                if (!hasResponded && !isFinishing) {
                    hasResponded = true
                    OnboardingSDK.handleClose(reason)
                    finishActivity()
                }
            }
        }

        @JavascriptInterface
        fun onError(error: String) {
            mainHandler.post {
                if (!hasResponded && !isFinishing) {
                    hasResponded = true
                    OnboardingSDK.handleError(error)
                    finishActivity()
                }
            }
        }

        @JavascriptInterface
        fun onClose(reason: String) {
            mainHandler.post {
                if (!hasResponded && !isFinishing) {
                    hasResponded = true
                    OnboardingSDK.handleClose(reason)
                    finishActivity()
                }
            }
        }

        @JavascriptInterface
        fun postMessage(json: String) {
            mainHandler.post {
                try {
                    val obj = JSONObject(json)
                    val type = obj.optString("event", obj.optString("type"))
                    val dataObj = obj.opt("data")
                    val data = when (dataObj) {
                        is JSONObject -> dataObj.toString()
                        null -> obj.toString()
                        else -> dataObj.toString()
                    }
                    when (type) {
                        "SUCCESS", "onSuccess" -> if (!hasResponded && !isFinishing) {
                            hasResponded = true
                            OnboardingSDK.handleSuccess(data)
                            finishActivity()
                        }
                        "EXIT", "onClose", "onExit" -> if (!hasResponded && !isFinishing) {
                            hasResponded = true
                            OnboardingSDK.handleClose(data)
                            finishActivity()
                        }
                        "ERROR", "onError" -> if (!hasResponded && !isFinishing) {
                            hasResponded = true
                            OnboardingSDK.handleError(data)
                            finishActivity()
                        }
                        "onEvent" -> {
                            OnboardingSDK.handleEvent(data)
                        }
                        "onLoad" -> {
                            OnboardingSDK.handleLoad(data)
                        }
                    }
                } catch (_: Exception) {
                    if (!hasResponded && !isFinishing) {
                        hasResponded = true
                        OnboardingSDK.handleError("Invalid message format")
                        finishActivity()
                    }
                }
            }
        }
    }
}
