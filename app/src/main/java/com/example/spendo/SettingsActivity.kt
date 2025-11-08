package com.example.spendo

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.spendo.utils.PreferencesManager
import java.util.Currency

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var tvCurrentCurrency: TextView
    
    // Common currencies
    private val currencies = listOf(
        "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", 
        "INR", "SGD", "HKD", "NZD", "LKR", "PKR", "BDT", "MYR", "THB", "IDR", "PHP", "VND"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        preferencesManager = PreferencesManager.getInstance(this)
        tvCurrentCurrency = findViewById(R.id.tv_current_currency)
        
        setupViews()
        loadCurrentCurrency()
    }
    
    private fun setupViews() {
        // Back button
        findViewById<View>(R.id.iv_back).setOnClickListener {
            finish()
        }
        
        // Currency selection button
        findViewById<View>(R.id.layout_currency).setOnClickListener {
            showCurrencySelectionDialog()
        }
    }
    
    private fun loadCurrentCurrency() {
        val currentCurrency = preferencesManager.getDefaultCurrency()
        tvCurrentCurrency.text = currentCurrency
    }
    
    private fun showCurrencySelectionDialog() {
        val currencyNames = currencies.map { currencyCode ->
            try {
                val currency = Currency.getInstance(currencyCode)
                "$currencyCode - ${currency.displayName}"
            } catch (e: Exception) {
                currencyCode
            }
        }.toTypedArray()
        
        val currentCurrency = preferencesManager.getDefaultCurrency()
        val selectedIndex = currencies.indexOf(currentCurrency).takeIf { it >= 0 } ?: 0
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Default Currency")
            .setSingleChoiceItems(currencyNames, selectedIndex) { dialog, which ->
                val selectedCurrency = currencies[which]
                preferencesManager.setDefaultCurrency(selectedCurrency)
                loadCurrentCurrency()
                Toast.makeText(this, "Default currency changed to $selectedCurrency", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

