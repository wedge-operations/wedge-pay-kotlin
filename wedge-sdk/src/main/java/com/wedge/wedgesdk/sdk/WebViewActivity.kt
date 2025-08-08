package com.wedge.wedgesdk.sdk

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var hasResponded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

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

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

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
                } catch (e: Exception) {
                    try {
                        finishAndRemoveTask()
                    } catch (e2: Exception) {
                        try {
                            moveTaskToBack(true)
                        } catch (e3: Exception) {
                            // Activity couldn't be finished
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
                        "SUCCESS" -> {
                            if (!hasResponded && !isFinishing) {
                                hasResponded = true
                                OnboardingSDK.handleSuccess(data)
                                finishActivity()
                            }
                        }
                        "EXIT" -> {
                            if (!hasResponded && !isFinishing) {
                                hasResponded = true
                                OnboardingSDK.handleExit(data)
                                finishActivity()
                            }
                        }
                        "ERROR" -> {
                            if (!hasResponded && !isFinishing) {
                                hasResponded = true
                                OnboardingSDK.handleError(data)
                                finishActivity()
                            }
                        }
                    }
                } catch (e: Exception) {
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