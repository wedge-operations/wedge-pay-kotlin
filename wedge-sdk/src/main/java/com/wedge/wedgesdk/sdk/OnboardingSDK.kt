package com.wedge.wedgesdk.sdk

import android.content.Intent
import androidx.fragment.app.FragmentActivity

interface OnboardingCallback {
    fun onSuccess(data: String)
    fun onExit(reason: String)
    fun onError(error: String)
}

object OnboardingSDK {
    private var callback: OnboardingCallback? = null

    fun startOnboarding(
        activity: FragmentActivity,
        apiKey: String,
        environment: String,
        callback: OnboardingCallback
    ) {
        this.callback = callback

        val intent = Intent(activity, WebViewActivity::class.java)
        intent.putExtra("apiKey", apiKey)
        intent.putExtra("environment", environment)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        activity.startActivity(intent)
    }

    fun handleSuccess(data: String) {
        callback?.onSuccess(data)
    }

    fun handleExit(reason: String) {
        callback?.onExit(reason)
    }

    fun handleError(error: String) {
        callback?.onError(error)
    }
} 