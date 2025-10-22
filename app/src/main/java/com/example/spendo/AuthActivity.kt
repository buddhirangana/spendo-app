package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.spendo.fragments.LoginFragment
import com.example.spendo.fragments.SignUpFragment

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        
        try {
            // Start with login fragment
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commit()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up auth: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    fun showSignUp() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SignUpFragment())
            .addToBackStack(null)
            .commit()
    }
    
    fun showLogin() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .addToBackStack(null)
            .commit()
    }
    
    fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}



