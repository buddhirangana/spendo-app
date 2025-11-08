package com.example.spendo.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "SpendoPreferences"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val DEFAULT_CURRENCY = "LKR" // Default to LKR
        
        @Volatile
        private var INSTANCE: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    fun getDefaultCurrency(): String {
        return prefs.getString(KEY_DEFAULT_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY
    }
    
    fun setDefaultCurrency(currency: String) {
        prefs.edit().putString(KEY_DEFAULT_CURRENCY, currency).apply()
    }
}

