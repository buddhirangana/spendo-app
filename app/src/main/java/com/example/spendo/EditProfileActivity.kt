package com.example.spendo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var ivBack: ImageView
    private lateinit var tvGoogleMessage: TextView
    private lateinit var groupEmailUser: Group
    private lateinit var tvChangePhoto: TextView

    private val auth = FirebaseAuth.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Views
        etUsername = findViewById(R.id.et_username)
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnSave = findViewById(R.id.btn_save)
        ivBack = findViewById(R.id.iv_back)
        tvGoogleMessage = findViewById(R.id.tv_google_message)
        groupEmailUser = findViewById(R.id.group_email_user)
        tvChangePhoto = findViewById(R.id.tv_change_photo)

        ivBack.setOnClickListener { finish() }

        checkUserProvider()

        btnSave.setOnClickListener {
            saveChanges()
        }

        tvChangePhoto.setOnClickListener {
            // Placeholder for profile picture change logic
            Toast.makeText(this, "Feature to change photo coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUserProvider() {
        val user = auth.currentUser
        if (user != null) {
            val isGoogleUser = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

            if (isGoogleUser) {
                // User logged in with Google
                tvGoogleMessage.visibility = View.VISIBLE
                groupEmailUser.visibility = View.GONE
                etUsername.setText(user.displayName ?: "Google User")
                etUsername.isEnabled = false // Disable username field
                tvChangePhoto.isEnabled = false
            } else {
                // User logged in with Email/Password
                tvGoogleMessage.visibility = View.GONE
                groupEmailUser.visibility = View.VISIBLE
                etUsername.setText(user.displayName ?: "")
                etUsername.isEnabled = true
                tvChangePhoto.isEnabled = true
            }
        }
    }

    private fun saveChanges() {
        val newUsername = etUsername.text.toString().trim()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (newUsername.isEmpty()) {
            etUsername.error = "Username cannot be empty"
            return
        }

        // Update username if it has changed
        val currentUser = auth.currentUser
        if (newUsername != currentUser?.displayName) {
            updateUsername(newUsername)
        }

        // Update password if fields are filled and match
        if (newPassword.isNotEmpty()) {
            if (newPassword != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return
            }
            if (newPassword.length < 6) {
                etNewPassword.error = "Password must be at least 6 characters"
                return
            }
            updatePassword(newPassword)
        } else {
            // If only username is changed and no password, finish activity
            if (newUsername != currentUser?.displayName) {
                // The username update has its own success listener which will finish
            } else {
                // Nothing was changed
                Toast.makeText(this, "No changes were made.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateUsername(newUsername: String) {
        val user = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUsername)
            .build()

        user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show()
                // If password is not being updated, finish here
                if (etNewPassword.text.toString().isEmpty()) {
                    finish()
                }
            } else {
                Toast.makeText(this, "Failed to update username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePassword(newPassword: String) {
        val user = auth.currentUser
        user?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                finish() // Go back to the profile screen
            } else {
                Toast.makeText(this, "Failed to update password. Please re-login and try again.", Toast.LENGTH_LONG).show()
            }
        }
    }
}