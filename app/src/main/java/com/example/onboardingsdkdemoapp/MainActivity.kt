package com.example.onboardingsdkdemoapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class MainActivity : AppCompatActivity() {

    private lateinit var tokenEditText: EditText
    private lateinit var startButton: Button
    private lateinit var typeSpinner: Spinner
    private lateinit var envSpinner: Spinner
    private lateinit var tokenLabel: TextView

    private val environments = arrayOf("Integration", "Sandbox", "Production")
    private val envValues = mapOf(
        "Integration" to "integration",
        "Sandbox" to "sandbox",
        "Production" to "production"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val safeContainer = FrameLayout(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "Onboarding SDK Demo"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }

        val envLabel = TextView(this).apply {
            text = "Environment:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        envSpinner = Spinner(this).apply {
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, environments)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            setAdapter(adapter)
            setPadding(0, 8, 0, 16)
        }

        val typeLabel = TextView(this).apply {
            text = "Onboarding Type:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        val types = arrayOf("onboarding", "funding")
        typeSpinner = Spinner(this).apply {
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, types)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            setAdapter(adapter)
            setPadding(0, 8, 0, 16)
        }

        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedType = types[position]
                updateUIForType(selectedType)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        tokenLabel = TextView(this).apply {
            text = "Token:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        tokenEditText = EditText(this).apply {
            hint = "Enter your token here"
            setPadding(16, 16, 16, 16)
        }

        startButton = Button(this).apply {
            text = "Start Onboarding"
            setPadding(0, 16, 0, 16)
        }

        startButton.setOnClickListener {
            val token = tokenEditText.text.toString().trim()
            if (token.isEmpty()) {
                showModal("Error", "Please enter a token")
                return@setOnClickListener
            }

            val selectedEnvLabel = envSpinner.selectedItem?.toString() ?: "Integration"
            val env = envValues[selectedEnvLabel] ?: "integration"
            val selectedType = typeSpinner.selectedItem.toString()
            val plaidCompletionRedirectUri: String? = null // Optional: e.g. "yourapp://complete" for Plaid Hosted Link

            com.wedge.wedgesdk.sdk.OnboardingSDK.startOnboarding(
                activity = this@MainActivity,
                token = token,
                env = env,
                type = selectedType,
                customBaseUrl = null,
                plaidCompletionRedirectUri = plaidCompletionRedirectUri,
                callback = object : com.wedge.wedgesdk.sdk.OnboardingCallback {
                    override fun onSuccess(data: String) {
                        showModal("Success", "Onboarding was completed successfully.\n\nAnswer:\n$data")
                    }

                    override fun onClose(reason: String) {
                        showModal("Closed", "The user closed the onboarding.\n\nReason:\n$reason")
                    }

                    override fun onError(error: String) {
                        val hint = if (error.contains("ERR_CONNECTION_REFUSED") && env == "development") {
                            "\n\n• On your Mac: run the web app so it listens on all interfaces, e.g. npm run dev -- --host 0.0.0.0 (or yarn dev --host 0.0.0.0).\n\n• If still refused: use your Mac’s IP in Development URL above (e.g. http://192.168.0.85:3000). Find IP in System Settings → Network."
                        } else ""
                        showModal("Error", "An error occurred during onboarding.\n\n$error$hint")
                    }

                    override fun onEvent(event: String) {
                        // Optional: handle generic events
                    }

                    override fun onLoad(data: String) {
                        // Optional: handle load event
                    }
                }
            )
        }

        mainLayout.addView(titleText)
        mainLayout.addView(envLabel)
        mainLayout.addView(envSpinner)
        mainLayout.addView(typeLabel)
        mainLayout.addView(typeSpinner)
        mainLayout.addView(tokenLabel)
        mainLayout.addView(tokenEditText)
        mainLayout.addView(startButton)

        safeContainer.addView(mainLayout)
        setContentView(safeContainer)

        ViewCompat.setOnApplyWindowInsetsListener(safeContainer) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottom = maxOf(sysBars.bottom, ime.bottom)

            v.updatePadding(
                left = sysBars.left,
                top = sysBars.top,
                right = sysBars.right,
                bottom = bottom
            )
            insets
        }

        updateUIForType("onboarding")
    }

    private fun updateUIForType(type: String) {
        when (type) {
            "onboarding" -> {
                tokenLabel.text = "Onboarding Token:"
                startButton.text = "Start Onboarding"
            }
            "funding" -> {
                tokenLabel.text = "Funding Token:"
                startButton.text = "Start Funding Flow"
            }
        }
    }

    private fun showModal(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
