package com.example.onboardingsdkdemoapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wedge.wedgesdk.sdk.OnboardingCallback
import com.wedge.wedgesdk.sdk.OnboardingSDK

class MainActivity : AppCompatActivity() {

    private lateinit var apiKeyEditText: EditText
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the main layout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Create title
        val titleText = TextView(this).apply {
            text = "Onboarding SDK Demo"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }

        // Create API key input field
        val apiKeyLabel = TextView(this).apply {
            text = "API Key:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        apiKeyEditText = EditText(this).apply {
            hint = "Enter your API key here"
            setPadding(16, 16, 16, 16)
        }

        // Create start button
        startButton = Button(this).apply {
            text = "Open SDK WebView"
            setPadding(0, 16, 0, 16)
        }

        startButton.setOnClickListener {
            val apiKey = apiKeyEditText.text.toString().trim()
            
            if (apiKey.isEmpty()) {
                showModal("Error", "Please enter an API key")
                return@setOnClickListener
            }

            val environment = "sandbox"

            OnboardingSDK.startOnboarding(
                activity = this@MainActivity,
                apiKey = apiKey,
                environment = environment,
                callback = object : OnboardingCallback {

                    override fun onSuccess(data: String) {
                        println("✅ onSuccess: $data")
                        showModal("Success", "Onboarding was completed successfully.\n\nAnswer:\n$data")
                    }

                    override fun onExit(reason: String) {
                        println("🚪 onExit: $reason")
                        showModal("Exit", "The user left the onboarding.\n\nReason:\n$reason")
                    }

                    override fun onError(error: String) {
                        println("❌ onError: $error")
                        showModal("Error", "An error occurred during onboarding.\n\nDetails:\n$error")
                    }
                }
            )
        }

        // Add views to the main layout
        mainLayout.addView(titleText)
        mainLayout.addView(apiKeyLabel)
        mainLayout.addView(apiKeyEditText)
        mainLayout.addView(startButton)

        setContentView(mainLayout)
    }

    private fun showModal(title: String, message: String) {
        runOnUiThread {
            AlertDialog.Builder(this@MainActivity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
