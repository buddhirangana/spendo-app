package com.example.spendo.utils

import android.content.Context

object CurrencyFormatter {
    
    fun formatAmount(context: Context, amount: Long): String {
        return formatAmountWithCode(context, amount)
    }
    
    fun formatAmountWithCode(context: Context, amount: Long): String {
        val preferencesManager = PreferencesManager.getInstance(context)
        val currencyCode = preferencesManager.getDefaultCurrency()
        return "$currencyCode ${String.format("%,d", amount)}"
    }
    
    fun getCurrencyCode(context: Context): String {
        val preferencesManager = PreferencesManager.getInstance(context)
        return preferencesManager.getDefaultCurrency()
    }
}