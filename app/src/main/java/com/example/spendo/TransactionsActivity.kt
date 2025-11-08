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
import com.example.spendo.adapters.TransactionGroupAdapter
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.example.spendo.utils.LoadingHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    private lateinit var transactionAdapter: TransactionGroupAdapter
    private var transactions = mutableListOf<Transaction>()
    private lateinit var btnMonth: MaterialButton
    private val months = DateFormatSymbols().months
    private var selectedMonthIndex = 0
    private var selectedFilterType: TransactionType? = null
    private lateinit var loadingHelper: LoadingHelper
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        repository = Repository()
        loadingHelper = LoadingHelper(this)
        setupViews()
        loadData()
    }

    private fun setupViews() {
        btnMonth = findViewById(R.id.btn_month)
        btnMonth.setOnClickListener {
            showMonthSelectorDialog()
        }

        findViewById<ImageView>(R.id.iv_filter).setOnClickListener {
            showFilterDialog()
        }

        // Financial report banner
        findViewById<View>(R.id.layout_report_banner).setOnClickListener {
            startActivity(Intent(this, FinancialReportActivity::class.java))
        }

        // Bottom navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    // Already on transactions
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
        bottomNavigationView.selectedItemId = R.id.nav_transactions

        // Add transaction FAB
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        // Setup recycler view
        transactionAdapter = TransactionGroupAdapter(emptyMap())
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_transactions).apply {
            layoutManager = LinearLayoutManager(this@TransactionsActivity)
            adapter = transactionAdapter
        }

        val calendar = Calendar.getInstance()
        selectedMonthIndex = calendar.get(Calendar.MONTH)
        updateMonthButtonText()
    }

    private fun loadData() {
        if (isLoading) return // Prevent multiple simultaneous loads
        
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        isLoading = true
        loadingHelper.showLoading("Loading transactions...")

        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    transactions.clear()
                    transactions.addAll(result.getOrNull() ?: emptyList())
                    filterAndGroupTransactions()
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when {
                        error?.message?.contains("timeout", ignoreCase = true) == true -> 
                            "Connection timeout. Please check your internet connection."
                        error?.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your internet connection."
                        else -> "Failed to load transactions. Please try again."
                    }
                    Toast.makeText(this@TransactionsActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Request timed out. Please check your connection and try again."
                    e.message?.contains("network", ignoreCase = true) == true -> 
                        "Network error. Please check your internet connection."
                    else -> "Error loading data: ${e.message ?: "Unknown error"}"
                }
                Toast.makeText(this@TransactionsActivity, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                loadingHelper.hideLoading()
            }
        }
    }

    private fun filterAndGroupTransactions() {
        val calendar = Calendar.getInstance()
        var filteredTransactions = transactions.filter { transaction ->
            calendar.time = transaction.date.toDate()
            calendar.get(Calendar.MONTH) == selectedMonthIndex
        }

        if (selectedFilterType != null) {
            filteredTransactions = filteredTransactions.filter { it.type == selectedFilterType }
        }

        // Group transactions by date
        val groupedTransactions = groupTransactionsByDate(filteredTransactions)
        transactionAdapter.updateData(groupedTransactions)
    }

    private fun groupTransactionsByDate(transactions: List<Transaction>): Map<String, List<Transaction>> {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val today = Date()
        val yesterday = Date(today.time - 24 * 60 * 60 * 1000)

        return transactions.groupBy { transaction ->
            val transactionDate = transaction.date.toDate()
            when {
                isSameDay(transactionDate, today) -> "Today"
                isSameDay(transactionDate, yesterday) -> "Yesterday"
                else -> dateFormat.format(transactionDate)
            }
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun showMonthSelectorDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select a Month")
            .setSingleChoiceItems(months, selectedMonthIndex) { dialog, which ->
                selectedMonthIndex = which
                updateMonthButtonText()
                filterAndGroupTransactions()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showFilterDialog() {
        val filterOptions = arrayOf("All", "Income", "Expense")
        val currentSelection = when (selectedFilterType) {
            null -> 0
            TransactionType.INCOME -> 1
            TransactionType.EXPENSE -> 2
        }

        AlertDialog.Builder(this)
            .setTitle("Filter by Type")
            .setSingleChoiceItems(filterOptions, currentSelection) { dialog, which ->
                selectedFilterType = when (which) {
                    0 -> null
                    1 -> TransactionType.INCOME
                    2 -> TransactionType.EXPENSE
                    else -> null
                }
                filterAndGroupTransactions()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun updateMonthButtonText() {
        btnMonth.text = months[selectedMonthIndex]
    }

    override fun onResume() {
        super.onResume()
        loadData() // Refresh data when returning from AddTransaction
    }
    
    override fun onDestroy() {
        super.onDestroy()
        loadingHelper.hideLoading()
    }
}
