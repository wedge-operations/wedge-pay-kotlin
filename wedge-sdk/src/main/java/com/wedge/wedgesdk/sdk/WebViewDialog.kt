package com.wedge.wedgesdk.sdk

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.wedge.wedgesdk.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject

private const val ARG_ENVIRONMENT = "environment"

private val environmentUrls = mapOf(
    "production" to "https://onboarding.wedge-can.com/",
    "sandbox" to "https://onboarding-integration.wedge-can.com/"
)

class WebViewDialog : BottomSheetDialogFragment() {

    private lateinit var webView: WebView
    private var apiKey: String? = null
    private var callback: OnboardingCallback? = null
    private var hasResponded = false

    companion object {
        const val TAG = "WebViewDialog"
        private const val ARG_API_KEY = "api_key"

        fun newInstance(apiKey: String, environment: String = "sandbox"): WebViewDialog {
            val fragment = WebViewDialog()
            val args = Bundle()
            args.putString(ARG_API_KEY, apiKey)
            args.putString(ARG_ENVIRONMENT, environment)
            fragment.arguments = args
            return fragment
        }
    }

    fun setCallback(callback: OnboardingCallback) {
        this.callback = callback
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_webview_dialog, container, false)

        apiKey = arguments?.getString(ARG_API_KEY)
        webView = view.findViewById(R.id.dialogWebView)

        if (apiKey.isNullOrEmpty()) {
            callback?.onError("API key is missing")
            hasResponded = true
            dismiss()
            return view
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowUniversalAccessFromFileURLs = false
            allowFileAccessFromFileURLs = false
        }

        webView.addJavascriptInterface(JSBridge(), "WedgeSDKAndroid")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("window.__ONBOARDING_API_KEY__ = '$apiKey';", null)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                if (!hasResponded) {
                    callback?.onError(error.description.toString())
                    hasResponded = true
                    dismiss()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "WebConsole",
                    "JS: ${consoleMessage.message()} [${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}]"
                )
                return true
            }
        }

        val environment = arguments?.getString(ARG_ENVIRONMENT) ?: "sandbox"
        val baseUrl = environmentUrls[environment] ?: environmentUrls["sandbox"]
        val urlToLoad = "$baseUrl?onboardingToken=$apiKey"

        webView.loadUrl(urlToLoad)

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!hasResponded) {
            callback?.onExit("User closed the onboarding manually")
            hasResponded = true
        }
    }

    override fun onDestroyView() {
        webView.removeJavascriptInterface("WedgeSDKAndroid")
        super.onDestroyView()
        callback = null
    }

    inner class JSBridge {
        @JavascriptInterface
        fun onSuccess(data: String) {
            if (!hasResponded) {
                callback?.onSuccess(data)
                hasResponded = true
                dismiss()
            }
        }

        @JavascriptInterface
        fun onExit(reason: String) {
            if (!hasResponded) {
                callback?.onExit(reason)
                hasResponded = true
                dismiss()
            }
        }

        @JavascriptInterface
        fun onError(error: String) {
            if (!hasResponded) {
                callback?.onError(error)
                hasResponded = true
                dismiss()
            }
        }

        @JavascriptInterface
        fun postMessage(json: String) {
            try {
                val jsonObj = JSONObject(json)
                val event = jsonObj.getString("type")
                val payload = jsonObj.getJSONObject("data").toString()

                when (event) {
                    "SUCCESS" -> onSuccess(payload)
                    "EXIT" -> onExit(payload)
                    "ERROR" -> onError(payload)
                    else -> Log.w("JSBridge", "❓ Unknown event: $event")
                }
            } catch (e: Exception) {
                onError("Invalid message format")
            }
        }
    }
}
