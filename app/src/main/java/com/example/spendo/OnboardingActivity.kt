package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if a user is already logged in
        if (FirebaseAuth.getInstance().currentUser != null) {
            // User is already logged in, so navigate directly to HomeActivity
            navigateToHome()
            return // return here to prevent the rest of onCreate from running
        }

        // If no user is logged in, show the onboarding screen as usual
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

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // Add flags to clear the back stack, so the user can't press "back" to get to the login screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // finish OnboardingActivity so it's removed from the back stack
    }
}
