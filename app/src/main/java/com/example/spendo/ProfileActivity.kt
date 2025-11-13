package com.example.spendo

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.spendo.data.Repository
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    private lateinit var ivProfilePicture: CircleImageView
    private lateinit var tvUsername: TextView
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        repository = Repository()
        ivProfilePicture = findViewById(R.id.iv_profile_picture)
        tvUsername = findViewById(R.id.tv_username)

        setupViews()
    }

    override fun onResume() {
        super.onResume()
        loadUserData() // Refresh data when returning from EditProfileActivity
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
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.layout_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.layout_export_data).setOnClickListener {
            startActivity(Intent(this, ExportActivity::class.java))
        }

        findViewById<View>(R.id.layout_app_info).setOnClickListener {
            startActivity(Intent(this, AppInfoActivity::class.java))
        }

        findViewById<View>(R.id.layout_logout).setOnClickListener {
            logout()
        }

        // Edit profile
        findViewById<View>(R.id.iv_edit).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
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
            tvUsername.text = username

            loadProfileImage(user.uid, user.photoUrl)
        } else {
            tvUsername.text = "User"
            ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun loadProfileImage(userId: String, fallbackUri: Uri?) {
        setProfileImageFromUri(fallbackUri)

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val base64Image = snapshot?.getString("photoBase64")
                if (!base64Image.isNullOrEmpty()) {
                    setProfileImageFromBase64(base64Image, fallbackUri)
                }
            }
    }

    private fun setProfileImageFromUri(uri: Uri?) {
        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(ivProfilePicture)
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun setProfileImageFromBase64(base64Image: String, fallbackUri: Uri?) {
        try {
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                ivProfilePicture.setImageBitmap(bitmap)
            } else {
                setProfileImageFromUri(fallbackUri)
            }
        } catch (e: IllegalArgumentException) {
            setProfileImageFromUri(fallbackUri)
        }
    }

    private fun logout() {
        repository.logout()
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}