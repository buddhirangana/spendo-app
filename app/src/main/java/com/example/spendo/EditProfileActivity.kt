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
import de.hdodenhof.circleimageview.CircleImageView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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
    private val firestore = FirebaseFirestore.getInstance()

    private var hasImageUpdated = false

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
        progressDialog.setTitle("Saving Changes...")
        progressDialog.setMessage("Please wait while we update your profile.")
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
        val user = auth.currentUser ?: return
        etUsername.setText(user.displayName ?: "")

        loadProfilePictureFromAuth(user.photoUrl)

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val base64Image = snapshot?.getString("photoBase64")
                if (!base64Image.isNullOrEmpty()) {
                    displayProfilePictureFromBase64(base64Image)
                }
            }
            .addOnFailureListener {
                // Ignore failure and keep auth photo / placeholder
            }
    }

    private fun loadProfilePictureFromAuth(photoUri: Uri?) {
        Glide.with(this)
            .load(photoUri)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(ivProfilePicture)
    }

    private fun displayProfilePictureFromBase64(base64Image: String) {
        try {
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                ivProfilePicture.setImageBitmap(bitmap)
            } else {
                loadProfilePictureFromAuth(auth.currentUser?.photoUrl)
            }
        } catch (e: IllegalArgumentException) {
            loadProfilePictureFromAuth(auth.currentUser?.photoUrl)
        }
    }

    private fun saveChanges() {
        val isGoogleUser =
            auth.currentUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                ?: false

        if (newImageUri != null) {
            handleImageAndSave(isGoogleUser)
            return
        }

        hasImageUpdated = false
        if (!isGoogleUser) {
            progressDialog.setTitle("Saving Changes...")
            progressDialog.setMessage("Please wait...")
            progressDialog.show()
            updateUserProfile()
        } else {
            Toast.makeText(this, "No new profile picture selected.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageAndSave(isGoogleUser: Boolean) {
        val user = auth.currentUser ?: return
        val imageUri = newImageUri ?: return

        progressDialog.setTitle("Saving Changes...")
        progressDialog.setMessage("Please wait while we process your profile picture.")
        progressDialog.show()

        lifecycleScope.launch {
            try {
                val base64Image = encodeImageToBase64(imageUri)
                saveProfileImageToFirestore(user.uid, base64Image)
                hasImageUpdated = true
                newImageUri = null

                if (isGoogleUser) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Profile picture updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    updateUserProfile()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(
                    this@EditProfileActivity,
                    "Failed to save profile picture: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun encodeImageToBase64(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = contentResolver.openInputStream(imageUri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalStateException("Unable to decode selected image.")

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        outputStream.close()

        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private suspend fun saveProfileImageToFirestore(userId: String, base64Image: String) {
        val data = mapOf("photoBase64" to base64Image)
        firestore.collection("users")
            .document(userId)
            .set(data, SetOptions.merge())
            .await()
    }


    private fun updateUserProfile() {
        val user = auth.currentUser ?: run {
            progressDialog.dismiss()
            return
        }
        val isGoogleUser = user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

        val profileUpdatesBuilder = UserProfileChangeRequest.Builder()

        if (!isGoogleUser) {
            val newUsername = etUsername.text.toString().trim()
            if (newUsername.isNotEmpty() && newUsername != user.displayName) {
                profileUpdatesBuilder.setDisplayName(newUsername)
            }
        }

        val profileUpdates = profileUpdatesBuilder.build()

        // Check if there are any actual changes to be made (name)
        if (profileUpdates.displayName == null) {
            if (!isGoogleUser) {
                updatePasswordIfNeeded()
            } else {
                progressDialog.dismiss()
                val message = if (hasImageUpdated) {
                    "Profile picture updated successfully!"
                } else {
                    "No changes were made."
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (hasImageUpdated) {
                    finish()
                }
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
                    val message = if (hasImageUpdated) {
                        "Profile updated successfully!"
                    } else {
                        "Profile updated successfully!"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, buildSuccessMessage(passwordChanged = false), Toast.LENGTH_SHORT)
                .show()
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
                    buildSuccessMessage(passwordChanged = true),
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

    private fun buildSuccessMessage(passwordChanged: Boolean): String {
        return when {
            passwordChanged && hasImageUpdated -> "Profile picture and password updated successfully!"
            passwordChanged -> "Password updated successfully!"
            hasImageUpdated -> "Profile updated successfully!"
            else -> "Profile updated successfully!"
        }
    }
}