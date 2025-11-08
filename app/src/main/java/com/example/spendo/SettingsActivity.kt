package com.example.spendo

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var layoutCurrency: LinearLayout
    private lateinit var tvCurrency: TextView
    private lateinit var switchNotifications: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadSettings()
        setupClickListeners()
    }

    private fun initViews() {
        layoutCurrency = findViewById(R.id.layout_currency)
        tvCurrency = findViewById(R.id.tv_currency)
        switchNotifications = findViewById(R.id.switch_notifications)
    }

    private fun loadSettings() {
        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Currency
        tvCurrency.text = sharedPrefs.getString("Currency", "LKR")

        // Notifications
        switchNotifications.isChecked = sharedPrefs.getBoolean("NotificationsEnabled", true)
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }

        layoutCurrency.setOnClickListener {
            showCurrencyDialog()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting(isChecked)
        }
    }

    private fun showCurrencyDialog() {
        val currencies = arrayOf("LKR", "USD", "EUR", "GBP") // Add more currencies as needed
        AlertDialog.Builder(this)
            .setTitle("Select Currency")
            .setItems(currencies) { _, which ->
                val selectedCurrency = currencies[which]
                tvCurrency.text = selectedCurrency
                saveCurrencySetting(selectedCurrency)
            }
            .show()
    }

    private fun saveCurrencySetting(currency: String) {
        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("Currency", currency).apply()
    }

    private fun saveNotificationSetting(isEnabled: Boolean) {
        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("NotificationsEnabled", isEnabled).apply()
    }
}
