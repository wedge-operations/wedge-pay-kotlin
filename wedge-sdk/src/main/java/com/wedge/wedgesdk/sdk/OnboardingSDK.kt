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

    /**
     * Starts the onboarding flow with the specified parameters
     * @param activity The FragmentActivity to start the onboarding from
     * @param apiKey The API key for authentication
     * @param environment The environment (sandbox, production, integration)
     * @param type The type of onboarding flow ("onboarding" for new users, "funding" for existing users)
     * @param callback The callback interface for handling onboarding events
     */
    fun startOnboarding(
        activity: FragmentActivity,
        apiKey: String,
        environment: String,
        type: String = "onboarding",
        callback: OnboardingCallback
    ) {
        this.callback = callback

        val intent = Intent(activity, WebViewActivity::class.java)
        intent.putExtra("apiKey", apiKey)
        intent.putExtra("environment", environment)
        intent.putExtra("type", type)
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