package com.example.spendo

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class SpendoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val theme = sharedPrefs.getInt("Theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(theme)
    }
}
