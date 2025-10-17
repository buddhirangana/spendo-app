package com.example.spendo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call installSplashScreen() before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Thread.sleep(1000)
        setContentView(R.layout.activity_main)

        // Your main activity logic here
    }
}