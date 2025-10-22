package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        setupViews()
    }
    
    private fun setupViews() {
        try {
            // Get Started button
            findViewById<View>(R.id.btn_get_started).setOnClickListener {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            
            // Login link
            findViewById<View>(R.id.tv_login_link).setOnClickListener {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up views: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
