package com.wedge.wedgesdk.sdk

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import org.json.JSONObject

/** Default scheme for Hosted Link completion redirect. Host apps can use this as hostedLinkCompletionRedirectUri. */
const val HOSTED_LINK_DEFAULT_SCHEME = "wedgehostedlink"
const val HOSTED_LINK_DEFAULT_HOST = "complete"

/** Default full redirect URI for Hosted Link. Pass this as [com.wedge.wedgesdk.sdk.OnboardingSDK.startOnboarding] hostedLinkCompletionRedirectUri to use the SDK's built-in deep link handling. */
const val HOSTED_LINK_DEFAULT_REDIRECT_URI = "$HOSTED_LINK_DEFAULT_SCHEME://$HOSTED_LINK_DEFAULT_HOST"

class WebViewActivity : AppCompatActivity() {

    companion object {
        /** Map of environment names to base URLs. */
        private val ENVIRONMENTS = mapOf(
            "integration" to "https://onboarding-integration.wedge-can.com",
            "sandbox" to "https://onboarding-sandbox.wedge-can.com",
            "production" to "https://onboarding-production.wedge-can.com"
        )
    }

    private lateinit var webView: WebView
    private var hasResponded = false
    /** True after opening Hosted Link in Custom Tab; cleared when we invoke __hostedLinkComplete. */
    private var hostedLinkPending = false
    /** Redirect URI for Hosted Link (from intent); used to detect success deep link. */
    private var hostedLinkCompletionRedirectUri: String? = null

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
        val customBaseUrl = intent.getStringExtra("customBaseUrl")?.trim()?.takeIf { it.isNotBlank() }
        hostedLinkCompletionRedirectUri = intent.getStringExtra("hostedLinkCompletionRedirectUri")?.trim()?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra("plaidCompletionRedirectUri")?.trim()?.takeIf { it.isNotBlank() }

        var baseUrl = (customBaseUrl ?: (ENVIRONMENTS[env] ?: ENVIRONMENTS["integration"]!!)).trim().trimEnd('/')
        val url = buildWebViewUrl(baseUrl, token, type, this.hostedLinkCompletionRedirectUri)
        Log.d("WedgeSDK", "Loading URL: $url (env=$env, baseUrl=$baseUrl)")

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

    /** Same URL shape for all environments: baseUrl + onboardingToken + type + optional hostedLinkCompletionRedirectUri. */
    private fun buildWebViewUrl(
        baseUrl: String,
        onboardingToken: String,
        type: String,
        hostedLinkCompletionRedirectUri: String?
    ): String {
        val separator = if (baseUrl.contains("?")) "&" else "?"
        val params = listOfNotNull(
            "onboardingToken=${Uri.encode(onboardingToken)}",
            "type=${Uri.encode(type)}",
            hostedLinkCompletionRedirectUri?.let { "hostedLinkCompletionRedirectUri=${Uri.encode(it)}" }
        )
        return baseUrl + separator + params.joinToString("&")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkHostedLinkRedirect(intent)
    }

    override fun onResume() {
        super.onResume()
        checkHostedLinkRedirect(intent)
        if (hostedLinkPending) {
            hostedLinkPending = false
            invokeHostedLinkComplete("cancel", null)
        }
    }

    /**
     * Runs the web app's Hosted Link completion callback. Call from main thread.
     * @param status "success" or "cancel"
     * @param callbackUrl optional redirect URL that returned to the app
     */
    private fun invokeHostedLinkComplete(status: String, callbackUrl: String?) {
        if (!::webView.isInitialized) return
        val urlArg = if (callbackUrl != null) {
            val escaped = callbackUrl.replace("\\", "\\\\").replace("\"", "\\\"")
            "\"$escaped\""
        } else "undefined"
        val js = "if (window.__hostedLinkComplete) { window.__hostedLinkComplete({ status: \"$status\", callbackUrl: $urlArg }); }"
        webView.evaluateJavascript(js, null)
    }

    private fun openHostedLinkUrl(url: String): Boolean {
        val u = url.trim()
        if (u.isEmpty()) {
            Log.w("WedgeSDK", "openHostedLink: empty url")
            return false
        }
        if (!u.startsWith("https://")) {
            Log.w("WedgeSDK", "openHostedLink: url must be https")
            return false
        }
        if (!u.contains("plaid.com/link/") && !u.contains("/link/")) {
            Log.w("WedgeSDK", "openHostedLink: url does not look like a typical Hosted Link URL")
            // still allow opening so web app can pass any URL they need
        }
        try {
            val uri = Uri.parse(u)
            hostedLinkPending = true
            CustomTabsIntent.Builder().build().launchUrl(this, uri)
            return true
        } catch (e: Exception) {
            Log.e("WedgeSDK", "openHostedLink: failed to launch Custom Tab", e)
            return false
        }
    }

    private fun checkHostedLinkRedirect(intent: Intent) {
        if (!hostedLinkPending) return
        var callbackUrl: String? = null
        val isSuccess = when {
            intent.getBooleanExtra("hostedLinkSuccess", false) -> {
                callbackUrl = intent.getStringExtra("hostedLinkCallbackUrl")
                true
            }
            intent.data != null -> {
                val data = intent.data!!
                val matchesDefault = data.scheme == HOSTED_LINK_DEFAULT_SCHEME && data.host == HOSTED_LINK_DEFAULT_HOST
                val matchesCustom = hostedLinkCompletionRedirectUri?.let { uri ->
                    try {
                        val expected = Uri.parse(uri)
                        data.scheme == expected.scheme && data.host == expected.host
                    } catch (_: Exception) { false }
                } ?: false
                if (matchesDefault || matchesCustom) {
                    callbackUrl = data.toString()
                    true
                } else false
            }
            else -> false
        }
        if (isSuccess) {
            hostedLinkPending = false
            invokeHostedLinkComplete("success", callbackUrl)
        }
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

        /** Opens the given Hosted Link URL in a Chrome Custom Tab. Returns true if the URL was accepted and the tab was launched. */
        @JavascriptInterface
        fun openHostedLink(url: String): Boolean {
            return openHostedLinkUrl(url)
        }

        @JavascriptInterface
        fun postMessage(json: String) {
            mainHandler.post {
                try {
                    val obj = JSONObject(json)
                    val type = obj.optString("event", obj.optString("type"))
                    when (type) {
                        "OPEN_HOSTED_LINK" -> {
                            val url = obj.optString("url", "").trim()
                            if (url.isNotEmpty()) {
                                openHostedLinkUrl(url)
                            }
                            return@post
                        }
                        else -> { /* fall through to existing handling */ }
                    }
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
