package com.wedge.wedgesdk.sdk

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
        Log.d("WebViewActivity", "🌐 Loading URL: $url")

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
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

            override fun onReceivedHttpError(
                view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse
            ) {
                if (!hasResponded) {
                    OnboardingSDK.handleError("HTTP Error ${errorResponse.statusCode}")
                    hasResponded = true
                    finish()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("WebViewActivity", "Console: ${consoleMessage?.message()}")
                return true
            }
        }

        webView.loadUrl(url)
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("WedgeSDKAndroid")
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
        @JavascriptInterface
        fun onSuccess(data: String) {
            if (!hasResponded) {
                hasResponded = true
                OnboardingSDK.handleSuccess(data)
                finish()
            }
        }

        @JavascriptInterface
        fun onExit(reason: String) {
            if (!hasResponded) {
                hasResponded = true
                OnboardingSDK.handleExit(reason)
                finish()
            }
        }

        @JavascriptInterface
        fun onError(error: String) {
            if (!hasResponded) {
                hasResponded = true
                OnboardingSDK.handleError(error)
                finish()
            }
        }

        @JavascriptInterface
        fun postMessage(json: String) {
            try {
                val obj = JSONObject(json)
                val type = obj.getString("type")
                val data = obj.getJSONObject("data").toString()
                when (type) {
                    "SUCCESS" -> onSuccess(data)
                    "EXIT" -> onExit(data)
                    "ERROR" -> onError(data)
                }
            } catch (e: Exception) {
                onError("Invalid message format")
            }
        }
    }
} 