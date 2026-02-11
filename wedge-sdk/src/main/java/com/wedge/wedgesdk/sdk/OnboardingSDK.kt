package com.wedge.wedgesdk.sdk

import android.content.Intent
import androidx.fragment.app.FragmentActivity

interface OnboardingCallback {
    fun onSuccess(data: String)
    /**
     * Called when the user closes the flow.
     */
    fun onClose(reason: String) {}
    @Deprecated("Use onClose instead")
    fun onExit(reason: String) {}
    fun onError(error: String)
    /**
     * Called for miscellaneous events from the web app.
     */
    fun onEvent(event: String) {}
    /**
     * Called when the web app finishes loading.
     */
    fun onLoad(data: String) {}
}

object OnboardingSDK {
    private var callback: OnboardingCallback? = null

    /**
     * Starts the onboarding flow with the specified parameters.
     * URL is built as: baseUrl + ?onboardingToken=...&type=... (&hostedLinkCompletionRedirectUri=... if provided).
     *
     * @param activity The FragmentActivity to start the onboarding from
     * @param token The onboarding token for authentication
     * @param env The environment: "integration", "sandbox", "production"
     * @param type The type of flow: "onboarding" or "funding"
     * @param customBaseUrl Optional override for base URL. When null, base URL is taken from the environment map.
     * @param hostedLinkCompletionRedirectUri Optional; e.g. "yourapp://complete" when using Hosted Link (opens in Custom Tab and redirects back to the app)
     * @param callback The callback interface for handling onboarding events
     */
    fun startOnboarding(
        activity: FragmentActivity,
        token: String,
        env: String,
        type: String = "onboarding",
        customBaseUrl: String? = null,
        hostedLinkCompletionRedirectUri: String? = null,
        callback: OnboardingCallback
    ) {
        this.callback = callback

        val intent = Intent(activity, WebViewActivity::class.java)
        intent.putExtra("token", token)
        intent.putExtra("env", env)
        intent.putExtra("type", type)
        customBaseUrl?.let { intent.putExtra("customBaseUrl", it) }
        hostedLinkCompletionRedirectUri?.let { intent.putExtra("hostedLinkCompletionRedirectUri", it) }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        activity.startActivity(intent)
    }

    fun handleSuccess(data: String) {
        callback?.onSuccess(data)
    }

    fun handleClose(reason: String) {
        // Backward compatibility: also call onExit if implemented
        try { callback?.onExit(reason) } catch (_: Throwable) {}
        callback?.onClose(reason)
    }

    @Deprecated("Use handleClose instead")
    fun handleExit(reason: String) = handleClose(reason)

    fun handleError(error: String) {
        callback?.onError(error)
    }

    fun handleEvent(event: String) {
        callback?.onEvent(event)
    }

    fun handleLoad(data: String) {
        callback?.onLoad(data)
    }
} 