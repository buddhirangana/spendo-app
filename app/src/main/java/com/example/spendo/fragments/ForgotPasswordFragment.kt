package com.example.spendo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.spendo.R
import com.example.spendo.data.Repository
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ForgotPasswordFragment : Fragment() {

    private lateinit var repository: Repository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = Repository()

        val btnSubmit = view.findViewById<Button>(R.id.btn_submit)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
        val tvBackToLogin = view.findViewById<TextView>(R.id.tv_back_to_login)

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                // Call the updated repository function
                val result = repository.sendPasswordResetEmail(email)

                // Always hide the progress bar after the operation is complete
                progressBar.visibility = View.GONE

                // Handle the success and failure cases
                result.onSuccess {
                    Toast.makeText(
                        context,
                        "Password reset link sent to your email.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Navigate back to the login screen after success
                    (activity as? com.example.spendo.AuthActivity)?.showLogin()
                }.onFailure { exception ->
                    // Check the exception type to give a more specific error message
                    val errorMessage = when (exception) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email address."
                        else -> "Failed to send reset email. Please try again."
                    }
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        val ivBackArrow = view.findViewById<ImageView>(R.id.iv_back_arrow)
        ivBackArrow.setOnClickListener {
            // This simulates a back press, returning to the previous fragment on the stack
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Logic for "Back to Login" text
        tvBackToLogin.setOnClickListener {
            (activity as? com.example.spendo.AuthActivity)?.showLogin()
        }
    }
}