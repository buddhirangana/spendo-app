package com.example.spendo

import android.annotation.SuppressLint
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
import android.widget.LinearLayout
import androidx.activity.result.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    private lateinit var layoutGoogleMessage: LinearLayout // Add this


    // Progress Dialog for UX
    private lateinit var progressDialog: ProgressDialog

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // To hold the URI of the newly selected image
    private var newImageUri: Uri? = null

    // ActivityResultLauncher for picking an image
    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                newImageUri = it
                // Load the selected image into the ImageView for preview
                Glide.with(this)
                    .load(it)
                    .into(ivProfilePicture)
            }
        }

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
        groupEmailUser = findViewById(R.id.group_email_user)
        tvChangePhoto = findViewById(R.id.tv_change_photo)
        ivProfilePicture = findViewById(R.id.iv_profile_picture)
        layoutGoogleMessage = findViewById(R.id.layout_google_message) // Update this
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
            val isGoogleUser =
                user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

            if (isGoogleUser) {
                // User logged in with Google
                layoutGoogleMessage.visibility = View.VISIBLE // Show the new layout
                groupEmailUser.visibility = View.GONE // Hide the editable fields
                // The save button is now only for the profile picture for Google users
                btnSave.text = "Save Picture"
            } else {
                // User logged in with Email/Password
                layoutGoogleMessage.visibility = View.GONE // Hide the message
                groupEmailUser.visibility = View.VISIBLE
                etUsername.setText(user.displayName ?: "")
                btnSave.text = "Save Changes"
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
            // No new image selected, just update name/password if applicable
            val isGoogleUser =
                auth.currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                    ?: false
            if (!isGoogleUser) {
                // For email users, proceed to update username/password
                progressDialog.setTitle("Saving Changes...")
                progressDialog.setMessage("Please wait...")
                progressDialog.show()
                updateUserProfile(null) // Pass null to indicate no new photo
            } else {
                // For Google users, if there's no new picture, there's nothing to save.
                Toast.makeText(this, "No new profile picture selected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageAndUpdateProfile() {
        val user = auth.currentUser ?: return
        val imageUri = newImageUri ?: return

        progressDialog.setTitle("Uploading...")
        progressDialog.setMessage("Please wait while we upload your new profile picture.")
        progressDialog.show()

        lifecycleScope.launch {
            try {
                val filename = UUID.randomUUID().toString()
                val storageRef = storage.reference.child("profile_pictures/$filename")

                // 1. Await the file upload to complete
                storageRef.putFile(imageUri).await()

                // 2. Only after upload is successful, get the download URL
                val downloadUrl = storageRef.downloadUrl.await()

                // 3. Now, update the user's profile with the new URL
                updateUserProfile(downloadUrl)

            } catch (e: Exception) {
                // If anything fails, dismiss dialog and show an error
                progressDialog.dismiss()
                Toast.makeText(
                    this@EditProfileActivity,
                    "Upload failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun updateUserProfile(photoUrl: Uri?) {
        val user = auth.currentUser ?: run {
            progressDialog.dismiss()
            return
        }
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

        // Check if there are any actual changes to be made (photo or name)
        if (profileUpdates.photoUri == null && profileUpdates.displayName == null) {
            // No profile changes, so just handle password update if needed
            if (!isGoogleUser) {
                updatePasswordIfNeeded()
            } else {
                // Google user with no photo change
                progressDialog.dismiss()
                Toast.makeText(this, "No changes were made.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Profile (name/photo) updated successfully. Now check for password.
                if (!isGoogleUser) {
                    updatePasswordIfNeeded()
                } else {
                    // Google user photo updated successfully
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Profile picture updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } else {
                // Failed to update profile (name/photo)
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Failed to update profile: ${task.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updatePasswordIfNeeded() {
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        // If no new password is entered, the update process is finished.
        if (newPassword.isBlank()) {
            progressDialog.dismiss()
            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Validate new password ---
        if (newPassword.length < 6) {
            progressDialog.dismiss()
            etNewPassword.error = "Password must be at least 6 characters"
            return
        }
        if (newPassword != confirmPassword) {
            progressDialog.dismiss()
            etConfirmPassword.error = "Passwords do not match"
            return
        }

        // --- Update password in Firebase ---
        auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            progressDialog.dismiss() // Always dismiss dialog when operation completes
            if (task.isSuccessful) {
                Toast.makeText(
                    this,
                    "Profile and password updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Failed to update password. Please re-login and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}