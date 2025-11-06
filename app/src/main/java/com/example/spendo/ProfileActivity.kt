package com.example.spendo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.spendo.data.Repository
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        repository = Repository()
        setupViews()
        loadUserData()
    }
    
    @SuppressLint("WrongConstant")
    private fun setupViews() {
        // Bottom navigation
        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, FinancialReportActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    // Already on profile
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_profile
        
        // Menu options
        findViewById<View>(R.id.layout_account).setOnClickListener {
            Toast.makeText(this, "Account settings coming soon", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.layout_settings).setOnClickListener {
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.layout_export_data).setOnClickListener {
            Toast.makeText(this, "Export data coming soon", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.layout_logout).setOnClickListener {
            logout()
        }
        
        // Edit profile
        findViewById<View>(R.id.iv_edit).setOnClickListener {
            Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show()
        }
        
        // Add transaction FAB
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }
    
    private fun loadUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val username = when {
                !user.displayName.isNullOrBlank() -> user.displayName
                !user.email.isNullOrBlank() -> user.email!!.substringBefore('@')
                else -> "User"
            }
            findViewById<TextView>(R.id.tv_username).text = username
        }
    }
    
    private fun logout() {
        repository.logout()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
