package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.spendo.data.Repository
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
    
    private fun setupViews() {
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
            findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_username).text = 
                user.displayName ?: "User"
        }
    }
    
    private fun logout() {
        repository.logout()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
