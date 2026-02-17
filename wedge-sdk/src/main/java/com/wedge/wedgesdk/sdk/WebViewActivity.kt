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

/** Default scheme for Hosted Link completion redirect. Host apps can use this as hostedLinkRedirectUri. */
const val HOSTED_LINK_DEFAULT_SCHEME = "wedgehostedlink"
const val HOSTED_LINK_DEFAULT_HOST = "complete"

/** Default full redirect URI for Hosted Link. Pass this as [com.wedge.wedgesdk.sdk.OnboardingSDK.startOnboarding] hostedLinkRedirectUri to use the SDK's built-in deep link handling. */
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
    /** Delayed cancel to avoid racing redirect on returning from Custom Tab. */
    private var hostedLinkCancelRunnable: Runnable? = null
    private val hostedLinkHandler = Handler(Looper.getMainLooper())
    /** Redirect URI for Hosted Link (from intent); used to detect success deep link. */
    private var hostedLinkRedirectUri: String = HOSTED_LINK_DEFAULT_REDIRECT_URI
    /** Capability flag consumed by webapp Hosted Link routing helpers. */
    private var supportsHostedLink: Boolean = true

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
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }
        root.addView(webView)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

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
        hostedLinkRedirectUri = resolveHostedLinkRedirectUri(intent)
        supportsHostedLink = intent.getBooleanExtra("supportsHostedLink", true)

        val resolvedBaseUrl = when {
            !customBaseUrl.isNullOrBlank() -> customBaseUrl
            env.startsWith("http://") || env.startsWith("https://") -> env
            else -> ENVIRONMENTS[env] ?: ENVIRONMENTS["integration"]!!
        }
        val baseUrl = resolvedBaseUrl.trim().trimEnd('/')
        val url = buildWebViewUrl(baseUrl, token, type, this.hostedLinkRedirectUri)
        Log.d("WedgeSDK", "Loading URL: $url (env=$env, baseUrl=$baseUrl)")

        webView.addJavascriptInterface(JSBridge(), "WedgeSDKAndroid")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                webView.evaluateJavascript("window.__ONBOARDING_TOKEN__ = '$token';", null)
                injectHostedLinkWebConfig()
                injectBridgeHostedLinkRedirectUriFallback()
                OnboardingSDK.handleLoad(url ?: "")
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                if (!request.isForMainFrame) {
                    Log.w(
                        "WedgeSDK",
                        "Subresource load error ignored: url=${request.url}, code=${error.errorCode}, desc=${error.description}"
                    )
                    return
                }
                if (!hasResponded) {
                    OnboardingSDK.handleError(
                        "Main frame load failed: url=${request.url}, code=${error.errorCode}, desc=${error.description}"
                    )
                    hasResponded = true
                    finish()
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (!request.isForMainFrame || hasResponded) return
                OnboardingSDK.handleError(
                    "HTTP error loading ${request.url}: status=${errorResponse.statusCode} reason=${errorResponse.reasonPhrase}"
                )
                hasResponded = true
                finish()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "WedgeSDKConsole",
                    "${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
                )
                return true
            }
        }

        webView.loadUrl(url)
    }

    /** URL shape: baseUrl + onboardingToken (+ type when not default) + hostedLinkRedirectUri. */
    private fun buildWebViewUrl(
        baseUrl: String,
        onboardingToken: String,
        type: String,
        hostedLinkRedirectUri: String
    ): String {
        val separator = if (baseUrl.contains("?")) "&" else "?"
        val params = listOfNotNull(
            "onboardingToken=${Uri.encode(onboardingToken)}",
            type.takeIf { it.isNotBlank() && it != "onboarding" }?.let { "type=${Uri.encode(it)}" },
            "hostedLinkRedirectUri=${Uri.encode(hostedLinkRedirectUri)}"
        )
        return baseUrl + separator + params.joinToString("&")
    }

    /** Inject redirect URI into a global config object for webapp Hosted Link token creation. */
    private fun injectHostedLinkWebConfig() {
        val quotedRedirectUri = JSONObject.quote(hostedLinkRedirectUri)
        val js = """
            (function() {
              var redirectUri = $quotedRedirectUri;
              if (!window.WedgeSDKConfig || typeof window.WedgeSDKConfig !== "object") {
                window.WedgeSDKConfig = {};
              }
              window.WedgeSDKConfig.platform = "android";
              window.WedgeSDKConfig.supportsHostedLink = ${if (supportsHostedLink) "true" else "false"};
              window.WedgeSDKConfig.hostedLinkRedirectUri = redirectUri;
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    /**
     * Bridge compatibility layer:
     * - Preferred: `getHostedLinkRedirectUri()` / `hostedLinkRedirectUri`
     */
    private fun injectBridgeHostedLinkRedirectUriFallback() {
        val js = """
            (function() {
              var bridge = window.WedgeSDKAndroid;
              if (!bridge) return;
              if (typeof bridge.getHostedLinkRedirectUri !== "function") return;
              var getter = function() { return bridge.getHostedLinkRedirectUri(); };
              if (typeof bridge.hostedLinkRedirectUri === "undefined") {
                try {
                  Object.defineProperty(bridge, "hostedLinkRedirectUri", {
                    configurable: true,
                    enumerable: false,
                    get: getter
                  });
                } catch (e) {
                  try { bridge.hostedLinkRedirectUri = getter(); } catch (_) {}
                }
              }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    /** Accept preferred key name for Hosted Link redirect URI. */
    private fun resolveHostedLinkRedirectUri(intent: Intent): String {
        return intent.getStringExtra("hostedLinkRedirectUri")?.trim()?.takeIf { it.isNotBlank() }
            ?: HOSTED_LINK_DEFAULT_REDIRECT_URI
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
            scheduleHostedLinkCancelFallback()
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
        webView.post {
            webView.evaluateJavascript(js, null)
        }
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
            hostedLinkCancelRunnable?.let { hostedLinkHandler.removeCallbacks(it) }
            CustomTabsIntent.Builder().build().launchUrl(this, uri)
            return true
        } catch (e: Exception) {
            Log.e("WedgeSDK", "openHostedLink: failed to launch Custom Tab", e)
            hostedLinkPending = false
            return false
        }
    }

    private fun scheduleHostedLinkCancelFallback() {
        hostedLinkCancelRunnable?.let { hostedLinkHandler.removeCallbacks(it) }
        hostedLinkCancelRunnable = Runnable {
            if (hostedLinkPending) {
                hostedLinkPending = false
                invokeHostedLinkComplete("cancel", null)
            }
        }
        hostedLinkHandler.postDelayed(hostedLinkCancelRunnable!!, 1500L)
    }

    /**
     * Reads hosted-link outcome from callback URL query params.
     * Recognized keys: `status` and `result` with values `success`/`cancel`.
     */
    private fun inferHostedLinkStatusFromCallbackUrl(callbackUrl: String?): String? {
        if (callbackUrl.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(callbackUrl)
            val statusValue = uri.getQueryParameter("status")
                ?: uri.getQueryParameter("result")
            when (statusValue?.trim()?.lowercase()) {
                "success", "completed", "complete" -> "success"
                "cancel", "canceled", "cancelled" -> "cancel"
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun checkHostedLinkRedirect(intent: Intent) {
        if (!hostedLinkPending) return
        var callbackUrl: String? = null
        var callbackStatus: String? = null
        val isHandled = when {
            intent.getBooleanExtra("hostedLinkSuccess", false) -> {
                callbackUrl = intent.getStringExtra("hostedLinkCallbackUrl")
                callbackStatus = "success"
                true
            }
            intent.data != null -> {
                val data = intent.data!!
                val matchesDefault = data.scheme == HOSTED_LINK_DEFAULT_SCHEME && data.host == HOSTED_LINK_DEFAULT_HOST
                val matchesCustom = try {
                    val expected = Uri.parse(hostedLinkRedirectUri)
                    data.scheme == expected.scheme && data.host == expected.host
                } catch (_: Exception) { false }
                if (matchesDefault || matchesCustom) {
                    callbackUrl = data.toString()
                    callbackStatus = inferHostedLinkStatusFromCallbackUrl(callbackUrl) ?: "success"
                    true
                } else false
            }
            else -> false
        }
        if (isHandled) {
            hostedLinkCancelRunnable?.let { hostedLinkHandler.removeCallbacks(it) }
            hostedLinkPending = false
            invokeHostedLinkComplete(callbackStatus ?: "success", callbackUrl)
        }
    }

    override fun onDestroy() {
        hostedLinkCancelRunnable?.let { hostedLinkHandler.removeCallbacks(it) }
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

        /** Bridge fallback for webapp builds that fetch redirect URI from Android bridge. */
        @JavascriptInterface
        fun getHostedLinkRedirectUri(): String = hostedLinkRedirectUri

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
