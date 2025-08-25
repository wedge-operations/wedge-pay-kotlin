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

    private lateinit var apiKeyEditText: EditText
    private lateinit var startButton: Button
    private lateinit var typeSpinner: Spinner
    private lateinit var apiKeyLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Habilita edge-to-edge (contenido bajo barras del sistema)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2) Contenedor "safe area" que recibirá los insets
        val safeContainer = FrameLayout(this)

        // 3) Tu layout principal dentro del contenedor seguro
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "Onboarding SDK Demo"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }

        // Type selection section
        val typeLabel = TextView(this).apply {
            text = "Onboarding Type:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        typeSpinner = Spinner(this).apply {
            val types = arrayOf("onboarding", "funding")
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, types)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            setAdapter(adapter)
            setPadding(0, 8, 0, 16)
        }

        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateUIForType(types[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        apiKeyLabel = TextView(this).apply {
            text = "API Key:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }

        apiKeyEditText = EditText(this).apply {
            hint = "Enter your API key here"
            setPadding(16, 16, 16, 16)
        }

        startButton = Button(this).apply {
            text = "Start Onboarding"
            setPadding(0, 16, 0, 16)
        }

        startButton.setOnClickListener {
            val apiKey = apiKeyEditText.text.toString().trim()
            if (apiKey.isEmpty()) {
                showModal("Error", "Please enter an API key")
                return@setOnClickListener
            }

            val environment = "sandbox"
            val selectedType = typeSpinner.selectedItem.toString()

            com.wedge.wedgesdk.sdk.OnboardingSDK.startOnboarding(
                activity = this@MainActivity,
                apiKey = apiKey,
                environment = environment,
                type = selectedType,
                callback = object : com.wedge.wedgesdk.sdk.OnboardingCallback {
                    override fun onSuccess(data: String) {
                        showModal("Success", "Onboarding was completed successfully.\n\nAnswer:\n$data")
                    }

                    override fun onExit(reason: String) {
                        showModal("Exit", "The user left the onboarding.\n\nReason:\n$reason")
                    }

                    override fun onError(error: String) {
                        showModal("Error", "An error occurred during onboarding.\n\nDetails:\n$error")
                    }
                }
            )
        }

        mainLayout.addView(titleText)
        mainLayout.addView(typeLabel)
        mainLayout.addView(typeSpinner)
        mainLayout.addView(apiKeyLabel)
        mainLayout.addView(apiKeyEditText)
        mainLayout.addView(startButton)

        safeContainer.addView(mainLayout)
        setContentView(safeContainer)

        // 4) Aplica padding según barras del sistema y teclado (IME)
        ViewCompat.setOnApplyWindowInsetsListener(safeContainer) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Si el teclado está visible, usa su altura; si no, usa la de la nav bar.
            val bottom = maxOf(sysBars.bottom, ime.bottom)

            v.updatePadding(
                left = sysBars.left,
                top = sysBars.top,
                right = sysBars.right,
                bottom = bottom
            )
            // Devuelve los insets sin consumir para que otros views también puedan usarlos si hace falta
            insets
        }

        // Initialize UI for default type
        updateUIForType("onboarding")
    }

    private fun updateUIForType(type: String) {
        when (type) {
            "onboarding" -> {
                apiKeyLabel.text = "Onboarding Token:"
                startButton.text = "Start Onboarding"
            }
            "funding" -> {
                apiKeyLabel.text = "Funding Token:"
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
