package com.example.spendo

import android.content.Context
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
    private var currentCurrency: String = "LKR"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        repository = Repository()
        setupViews()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        currentCurrency = sharedPrefs.getString("Currency", "LKR") ?: "LKR"
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

        findViewById<View>(R.id.layout_report_banner).setOnClickListener {
            startActivity(Intent(this, FinancialReportActivity::class.java))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_transactions -> true
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

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        transactionAdapter = TransactionGroupAdapter(
            emptyMap(),
            currentCurrency,
            onUpdate = { transaction ->
                val intent = Intent(this, AddTransactionActivity::class.java).apply {
                    putExtra("TRANSACTION_ID", transaction.id)
                    putExtra("TRANSACTION_AMOUNT", transaction.amount)
                    putExtra("TRANSACTION_CATEGORY", transaction.category)
                    putExtra("TRANSACTION_DESC", transaction.description)
                    putExtra("TRANSACTION_TYPE", transaction.type.name) // Pass enum as String
                    putExtra("TRANSACTION_DATE_SECONDS", transaction.date.seconds)
                    putExtra("TRANSACTION_DATE_NANOS", transaction.date.nanoseconds)
                }
                startActivity(intent)
            },
            onDelete = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_transactions).apply {
            layoutManager = LinearLayoutManager(this@TransactionsActivity)
            adapter = transactionAdapter
        }

        val calendar = Calendar.getInstance()
        selectedMonthIndex = calendar.get(Calendar.MONTH)
        updateMonthButtonText()
    }

    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    transactions.clear()
                    transactions.addAll(result.getOrNull() ?: emptyList())
                    filterAndGroupTransactions()
                } else {
                    Toast.makeText(this@TransactionsActivity, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TransactionsActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
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
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        lifecycleScope.launch {
            try {
                val result = repository.deleteTransaction(transaction.id)
                if (result.isSuccess) {
                    Toast.makeText(this@TransactionsActivity, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this@TransactionsActivity, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TransactionsActivity, "Error deleting transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMonthButtonText() {
        btnMonth.text = months[selectedMonthIndex]
    }
}
