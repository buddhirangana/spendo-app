package com.example.spendo

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.util.UUID

class EditProfileActivity : AppCompatActivity() {

    // Views
    private lateinit var etUsername: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var ivBack: ImageView
    private lateinit var tvGoogleMessage: TextView
    private lateinit var groupEmailUser: Group
    private lateinit var tvChangePhoto: TextView
    private lateinit var ivProfilePicture: CircleImageView

    // Progress Dialog for UX
    private lateinit var progressDialog: ProgressDialog

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // To hold the URI of the newly selected image
    private var newImageUri: Uri? = null

    // ActivityResultLauncher for picking an image
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            newImageUri = it
            // Load the selected image into the ImageView for preview
            Glide.with(this)
                .load(it)
                .into(ivProfilePicture)
        }
    }

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
        ivProfilePicture = findViewById(R.id.iv_profile_picture)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Uploading...")
        progressDialog.setMessage("Please wait while we upload your new profile picture.")
        progressDialog.setCancelable(false)

        ivBack.setOnClickListener { finish() }

        // Set up listeners
        tvChangePhoto.setOnClickListener {
            selectImageLauncher.launch("image/*") // Launch image picker
        }

        btnSave.setOnClickListener {
            saveChanges()
        }

        checkUserProvider()
        loadUserProfile()
    }

    private fun checkUserProvider() {
        val user = auth.currentUser
        if (user != null) {
            val isGoogleUser = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

            if (isGoogleUser) {
                // User logged in with Google
                tvGoogleMessage.visibility = View.VISIBLE
                findViewById<View>(R.id.til_username).visibility = View.GONE
                findViewById<View>(R.id.til_new_password).visibility = View.GONE
                findViewById<View>(R.id.til_confirm_password).visibility = View.GONE
                btnSave.visibility = View.VISIBLE
            } else {
                // User logged in with Email/Password
                tvGoogleMessage.visibility = View.GONE
                groupEmailUser.visibility = View.VISIBLE
                etUsername.setText(user.displayName ?: "")
            }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        user?.let {
            etUsername.setText(it.displayName)
            Glide.with(this)
                .load(it.photoUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(ivProfilePicture)
        }
    }

    private fun saveChanges() {
        if (newImageUri != null) {
            uploadImageAndUpdateProfile()
        } else {
            val isGoogleUser = auth.currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } ?: false
            if (!isGoogleUser) {
                updateUsernameAndPasswordIfNeeded()
            } else {
                Toast.makeText(this, "No new profile picture selected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageAndUpdateProfile() {
        val user = auth.currentUser ?: return
        progressDialog.show() // Show loading indicator

        // **SECURITY FIX**: Store image under the user's unique ID
        val filename = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("profile_pictures/${user.uid}/$filename")

        newImageUri?.let { uri ->
            storageRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }.addOnCompleteListener { task ->
                    progressDialog.dismiss() // Hide loading indicator
                    if (task.isSuccessful) {
                        val downloadUrl = task.result
                        updateUserProfile(downloadUrl)
                    } else {
                        Toast.makeText(this, "Upload failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun updateUserProfile(photoUrl: Uri?) {
        // ... (The rest of your code remains the same)
        val user = auth.currentUser ?: return
        val isGoogleUser = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

        val profileUpdatesBuilder = UserProfileChangeRequest.Builder()
        photoUrl?.let { profileUpdatesBuilder.setPhotoUri(it) }

        if (!isGoogleUser) {
            val newUsername = etUsername.text.toString().trim()
            if (newUsername.isNotEmpty() && newUsername != user.displayName) {
                profileUpdatesBuilder.setDisplayName(newUsername)
            }
        }

        val profileUpdates = profileUpdatesBuilder.build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    if (!isGoogleUser) {
                        updatePasswordIfNeeded()
                    } else {
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUsernameAndPasswordIfNeeded() {
        // ... (This function remains the same)
        val newUsername = etUsername.text.toString().trim()
        val newPassword = etNewPassword.text.toString()
        val currentUser = auth.currentUser!!
        val usernameChanged = newUsername.isNotEmpty() && newUsername != currentUser.displayName
        val passwordChanged = newPassword.isNotEmpty()

        if (!usernameChanged && !passwordChanged) {
            Toast.makeText(this, "No changes were made.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (usernameChanged) {
            updateUserProfile(null)
        } else {
            updatePasswordIfNeeded()
        }
    }


    private fun updatePasswordIfNeeded() {
        // ... (This function remains the same)
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (newPassword.isNotEmpty()) {
            if (newPassword != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return
            }
            if (newPassword.length < 6) {
                etNewPassword.error = "Password must be at least 6 characters"
                return
            }

            auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update password. Please re-login and try again.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            finish()
        }
    }
}