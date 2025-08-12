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

        // 1) Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2) Contenedor raíz para manejar insets
        val root = FrameLayout(this)
        setContentView(root)

        // 3) WebView a pantalla completa
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

        // 4) Aplicar WindowInsets: top/left/right al root, bottom/IME al WebView
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

            // Lados y top al contenedor
            v.updatePadding(left = sysBars.left, top = sysBars.top, right = sysBars.right)
            // Solo bottom al WebView para evitar doble padding en el scroll
            webView.updatePadding(bottom = bottomSafe)

            insets
        }

        // (Opcional) Iconos oscuros en barras si tu fondo es claro
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 5) Lógica existente
        val apiKey = intent.getStringExtra("apiKey") ?: run {
            OnboardingSDK.handleError("Missing API key")
            finish()
            return
        }

        val environment = intent.getStringExtra("environment") ?: "sandbox"
        val baseUrl = when (environment) {
            "production" -> "https://onboarding.wedge-can.com/"
            else -> "https://onboarding-integration.wedge-can.com/"
        }
        val url = "$baseUrl?onboardingToken=$apiKey"

        webView.addJavascriptInterface(JSBridge(), "WedgeSDKAndroid")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                webView.evaluateJavascript("window.__ONBOARDING_API_KEY__ = '$apiKey';", null)
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
                    OnboardingSDK.handleExit(reason)
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
        fun postMessage(json: String) {
            mainHandler.post {
                try {
                    val obj = JSONObject(json)
                    val type = obj.getString("type")
                    val data = obj.getJSONObject("data").toString()
                    when (type) {
                        "SUCCESS" -> if (!hasResponded && !isFinishing) {
                            hasResponded = true
                            OnboardingSDK.handleSuccess(data)
                            finishActivity()
                        }
                        "EXIT" -> if (!hasResponded && !isFinishing) {
                            hasResponded = true
                            OnboardingSDK.handleExit(data)
                            finishActivity()
                        }
                        "ERROR" -> if (!hasResponded && !isFinishing) {
                            hasResponded = true
                            OnboardingSDK.handleError(data)
                            finishActivity()
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
