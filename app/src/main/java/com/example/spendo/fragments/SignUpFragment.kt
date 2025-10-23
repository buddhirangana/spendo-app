package com.example.spendo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.spendo.R
import com.example.spendo.data.Repository
import kotlinx.coroutines.launch

class SignUpFragment : Fragment() {
    private lateinit var repository: Repository
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivBackArrow = view.findViewById<ImageView>(R.id.iv_back_arrow)

        ivBackArrow.setOnClickListener {
            // This will simulate a press of the system's back button
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        
        repository = Repository()
        
        view.findViewById<View>(R.id.btn_signup).setOnClickListener {
            val name = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_name).text.toString()
            val email = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_email).text.toString()
            val password = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_password).text.toString()
            val termsAccepted = view.findViewById<android.widget.CheckBox>(R.id.cb_terms).isChecked
            
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!termsAccepted) {
                Toast.makeText(context, "Please accept terms and conditions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            signUp(name, email, password)
        }
        
        // Login link
        view.findViewById<View>(R.id.tv_login_link).setOnClickListener {
            (activity as? com.example.spendo.AuthActivity)?.showLogin()
        }
    }
    
    private fun signUp(name: String, email: String, password: String) {
        view?.findViewById<View>(R.id.progress_bar)?.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                repository.signUp(name, email, password)
                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                (activity as? com.example.spendo.AuthActivity)?.navigateToHome()
            } catch (e: Exception) {
                Toast.makeText(context, "Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                view?.findViewById<View>(R.id.progress_bar)?.visibility = View.GONE
            }
        }
    }
}
