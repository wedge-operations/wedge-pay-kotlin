package com.wedge.wedgesdk.sdk

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WebViewBottomSheetFragment(
    private val url: String,
    private val callback: Callback
) : BottomSheetDialogFragment() {

    interface Callback {
        fun onSuccess(data: String)
        fun onExit()
        fun onError(error: String)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val webView = WebView(requireContext())
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                when {
                    url.startsWith("sdk://success") -> {
                        val data = url.substringAfter("sdk://success?data=")
                        callback.onSuccess(data)
                        dismiss()
                        return true
                    }
                    url.startsWith("sdk://exit") -> {
                        Log.d("WebViewBottomSheet", "Callback onExit triggered")
                        callback.onExit()
                        dismiss()
                        return true
                    }
                    url.startsWith("sdk://error") -> {
                        val error = url.substringAfter("sdk://error?message=")
                        callback.onError(error)
                        dismiss()
                        return true
                    }
                }

                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        webView.loadUrl(url)
        return webView
    }

    override fun getTheme(): Int = com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog
} 