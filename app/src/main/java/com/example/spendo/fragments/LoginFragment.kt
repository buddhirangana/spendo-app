package com.example.spendo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.spendo.R
import com.example.spendo.data.Repository
import kotlinx.coroutines.launch
import android.widget.ImageView

class LoginFragment : Fragment() {
    private lateinit var repository: Repository
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivBackArrow = view.findViewById<ImageView>(R.id.iv_back_arrow)

        ivBackArrow.setOnClickListener {
            // This will simulate a press of the system's back button
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        
        try {
            repository = Repository()
            
            view.findViewById<View>(R.id.btn_login).setOnClickListener {
                val email = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_email).text.toString()
                val password = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_password).text.toString()
                
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                login(email, password)
            }

            view.findViewById<View>(R.id.tv_forgot_password).setOnClickListener {
                (activity as? com.example.spendo.AuthActivity)?.showForgotPassword()
            }
            
            // Sign up link
            val signupPrompt = view.findViewById<View>(R.id.tv_signup_link)
            if (signupPrompt != null) {
                signupPrompt.setOnClickListener {
                    (activity as? com.example.spendo.AuthActivity)?.showSignUp()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error setting up login: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ... other code in LoginFragment.kt

    private fun login(email: String, password: String) {    // Show loading indicator
        val progressBar = view?.findViewById<View>(R.id.progress_bar)
        progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Call the updated repository function
            val loginResult = repository.login(email, password)

            // Hide loading indicator
            progressBar?.visibility = View.GONE

            // Handle the result from the repository
            loginResult.onSuccess {
                // Login was successful
                Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                // Navigate to the home screen
                (activity as? com.example.spendo.AuthActivity)?.navigateToHome()
            }.onFailure { exception ->
                // Login failed, show a user-friendly error message
                val errorMessage = when (exception) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found with this email."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please try again."
                    else -> "Login failed. Please check your connection."
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}
