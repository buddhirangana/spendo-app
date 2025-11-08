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
                try {
                    // Send password reset email
                    val result = repository.sendPasswordResetEmail(email)
                    
                    result.onSuccess {
                        Toast.makeText(
                            context,
                            "Password reset link sent (if account exists).",
                            Toast.LENGTH_LONG
                        ).show()
                        // Navigate back to the login screen after success
                        (activity as? com.example.spendo.AuthActivity)?.showLogin()
                    }.onFailure { exception ->
                        Toast.makeText(
                            context,
                            "Failed to send reset email. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    println("Unexpected error in forgot password: ${e.message}")
                    Toast.makeText(
                        context,
                        "An unexpected error occurred. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
                // Always hide the progress bar after the operation is complete
                progressBar.visibility = View.GONE
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