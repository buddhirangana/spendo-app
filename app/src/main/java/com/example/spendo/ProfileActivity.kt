package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.spendo.fragments.LoginFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvUsername: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        loadUserProfile()
        setupClickListeners()
    }

    private fun initViews() {
        ivProfilePicture = findViewById(R.id.iv_profile_picture)
        tvUsername = findViewById(R.id.tv_username)
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            tvUsername.text = user.displayName ?: "User"
            if (user.photoUrl != null) {
                Glide.with(this)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivProfilePicture)
            } else {
                ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    private fun setupClickListeners() {
        // Edit profile
        findViewById<View>(R.id.iv_edit).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Account
        findViewById<View>(R.id.layout_account).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Settings
        findViewById<View>(R.id.layout_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Logout
        findViewById<View>(R.id.layout_logout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Bottom navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
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
                R.id.nav_profile -> true // Already here
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_profile

        findViewById<View>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }
}
