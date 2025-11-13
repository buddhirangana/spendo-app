package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.spendo.data.Repository
import com.example.spendo.fragments.ForgotPasswordFragment
import com.example.spendo.fragments.LoginFragment
import com.example.spendo.fragments.SignUpFragment
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var repository: Repository

    // ActivityResultLauncher for the Google Sign-In flow
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // Google Sign In was successful, authenticate with Firebase
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Google Sign In failed, log the specific error code
            val errorCode = e.statusCode
            val errorMessage = when (errorCode) {
                10 -> "Google Sign In configuration error. Please check SHA-1 fingerprint in Firebase Console."
                7 -> "Network error. Please check your internet connection."
                12501 -> "User cancelled the sign in process."
                else -> "Google Sign In failed. Error Code: $errorCode"
            }
            Log.w("AuthActivity", "Google Sign In failed: $errorMessage", e)
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            // Hide loading indicator on error
            hideLoadingIndicator()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        repository = Repository()

        // Configure Google Sign-In
        configureGoogleSignIn()

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

    private fun configureGoogleSignIn() {
        // Use your server's client ID from google-services.json
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    // This method will be called from the SignUpFragment
    fun startGoogleSignUp() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        lifecycleScope.launch {
            try {
                // Sign in to Firebase with the Google token
                val result = repository.signInWithGoogle(idToken)
                result.onSuccess {
                    // Hide loading indicator
                    hideLoadingIndicator()
                    Log.d("AuthActivity", "Google authentication successful!")
                    Toast.makeText(this@AuthActivity, "Google authentication successful!", Toast.LENGTH_SHORT)
                        .show()
                    navigateToHome()
                }.onFailure { exception ->
                    // Hide loading indicator
                    hideLoadingIndicator()
                    Log.e("AuthActivity", "Google authentication failed: ${exception.message}", exception)
                    val errorMessage = when {
                        exception.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your connection."
                        exception.message?.contains("invalid", ignoreCase = true) == true -> 
                            "Invalid Google account. Please try again."
                        exception.message?.contains("firestore", ignoreCase = true) == true ->
                            "User created but database error. Please try again."
                        else -> "Google authentication failed: ${exception.message}"
                    }
                    Toast.makeText(this@AuthActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Hide loading indicator
                hideLoadingIndicator()
                Toast.makeText(
                    this@AuthActivity,
                    "An error occurred: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun hideLoadingIndicator() {
        try {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            when (currentFragment) {
                is SignUpFragment -> {
                    currentFragment.view?.findViewById<View>(R.id.progress_bar)?.visibility = View.GONE
                }
                is LoginFragment -> {
                    currentFragment.view?.findViewById<View>(R.id.progress_bar)?.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.w("AuthActivity", "Error hiding loading indicator: ${e.message}")
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

    fun showForgotPassword() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ForgotPasswordFragment())
            .addToBackStack(null)
            .commit()
    }

    fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}