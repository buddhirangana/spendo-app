package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase already initialized or error
        }

        // Set a simple layout to prevent crashes
        setContentView(R.layout.activity_main)

        try {
            // For now, always go to onboarding to test the app
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            // Fallback to onboarding
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }
}