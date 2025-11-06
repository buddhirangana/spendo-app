package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendo.adapters.TransactionAdapter
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Find the button
        btnMonth = findViewById(R.id.btn_month)

        // Set the current month as the default
        val calendar = Calendar.getInstance()
        selectedMonthIndex = calendar.get(Calendar.MONTH)
        updateMonthButtonText()

        // Set a click listener to show the dialog
        btnMonth.setOnClickListener {
            showMonthSelectorDialog()
        }
        
        repository = Repository()
        setupViews()
        loadData()
    }
    
    private fun setupViews() {
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
                    finish()
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, FinancialReportActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    transactions.clear()
                    transactions.addAll(result.getOrNull() ?: emptyList())
                    
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
        val totalIncome = transactions.filter { it.type == com.example.spendo.data.TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == com.example.spendo.data.TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = totalIncome - totalExpenses
        
        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_balance).text = "LKR ${String.format("%,d", balance)}"
        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_income).text = "LKR ${String.format("%,d", totalIncome)}"
        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_expenses).text = "LKR ${String.format("%,d", totalExpenses)}"
    }

    private fun showMonthSelectorDialog() {
        // Create an AlertDialog builder
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a Month")

        // Set the list of months and the click listener
        builder.setSingleChoiceItems(months, selectedMonthIndex) { dialog, which ->
            // Update the selected month index
            selectedMonthIndex = which
            // Update the button text with the new month
            updateMonthButtonText()
            // Dismiss the dialog
            dialog.dismiss()
        }

        // Create and show the dialog
        val dialog = builder.create()
        dialog.show()
    }

    private fun updateMonthButtonText() {
        btnMonth.text = months[selectedMonthIndex]
    }
    
    override fun onResume() {
        super.onResume()
        loadData() // Refresh data when returning from AddTransaction
    }
}
