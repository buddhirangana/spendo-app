package com.example.spendo.utils

import android.content.Context
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {
    
    fun formatAmount(context: Context, amount: Long): String {
        return formatAmountWithCode(context, amount)
    }

//    fun formatAmount(context: Context, amount: Long): String {
//        val preferencesManager = PreferencesManager.getInstance(context)
//        val currencyCode = preferencesManager.getDefaultCurrency()
//
//        return try {
//            val currency = Currency.getInstance(currencyCode)
//            val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
//            format.currency = currency
//            format.format(amount)
//        } catch (e: Exception) {
//            // Fallback to simple format if currency is invalid
//            "$currencyCode ${String.format("%,d", amount)}"
//        }
//    }
    
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
