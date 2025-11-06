package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.spendo.adapters.TransactionAdapter
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var btnMonth: MaterialButton
    private val months = DateFormatSymbols().months
    private var selectedMonthIndex = 0

    private lateinit var repository: Repository
    private lateinit var transactionAdapter: TransactionAdapter
    private var transactions = mutableListOf<Transaction>()

    // Add a variable for the profile ImageView
    private lateinit var ivProfile: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Find the profile image view
        ivProfile = findViewById(R.id.iv_profile)
        btnMonth = findViewById(R.id.btn_month)

        // Set the current month as the default
        val calendar = Calendar.getInstance()
        selectedMonthIndex = calendar.get(Calendar.MONTH)
        updateMonthButtonText()

        // Set a click listener to show the dialog
        btnMonth.setOnClickListener {
            showMonthSelectorDialog()
        }

        // Add a click listener for the profile picture to go to the Edit Profile screen
        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        repository = Repository()
        setupViews()
        loadData()
        loadProfilePicture() // Call the new function
    }

    // --- ADD THIS NEW FUNCTION ---
    private fun loadProfilePicture() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // If the user has a photo URL, load it with Glide
            if (user.photoUrl != null) {
                Glide.with(this)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.ic_profile_placeholder) // Show placeholder while loading
                    .error(R.drawable.ic_profile_placeholder) // Show placeholder if loading fails
                    .into(ivProfile)
            } else {
                // If there's no photo URL, load the default placeholder
                ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else {
            // If no user is logged in, load the default placeholder
            ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun setupViews() {
        // ... (your existing setupViews code remains the same)
        // Bottom navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, FinancialReportActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_home

        // Add transaction FAB
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        // See all transactions
        findViewById<View>(R.id.tv_see_all).setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }

        // Setup recycler view
        transactionAdapter = TransactionAdapter(transactions)
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_recent_transactions).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = transactionAdapter
        }
    }

    private fun loadData() {
        // ... (your existing loadData code remains the same)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    val allTransactions = result.getOrNull() ?: emptyList()

                    // Filter transactions for the selected month
                    val calendar = Calendar.getInstance()
                    val filteredTransactions = allTransactions.filter {
                        calendar.time = it.date.toDate()
                        calendar.get(Calendar.MONTH) == selectedMonthIndex
                    }

                    transactions.clear()
                    transactions.addAll(filteredTransactions)

                    // Update UI with calculated values
                    updateSummary()
                    transactionAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@HomeActivity, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSummary() {
        // ... (your existing updateSummary code remains the same)
        val totalIncome = transactions.filter { it.type == com.example.spendo.data.TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == com.example.spendo.data.TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses

        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_balance).text = "LKR ${String.format("%,d", balance)}"
        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_income).text = "LKR ${String.format("%,d", totalIncome)}"
        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_expenses).text = "LKR ${String.format("%,d", totalExpenses)}"
    }

    private fun showMonthSelectorDialog() {
        // ... (your existing showMonthSelectorDialog code remains the same)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a Month")

        builder.setSingleChoiceItems(months, selectedMonthIndex) { dialog, which ->
            selectedMonthIndex = which
            updateMonthButtonText()
            loadData() // Refresh data for the newly selected month
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun updateMonthButtonText() {
        btnMonth.text = months[selectedMonthIndex]
    }

    override fun onResume() {
        super.onResume()
        loadData() // Refresh transaction data
        loadProfilePicture() // Refresh the profile picture
    }
}
