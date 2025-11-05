package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendo.adapters.TransactionGroupAdapter
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    private lateinit var transactionAdapter: TransactionGroupAdapter
    private var transactions = mutableListOf<Transaction>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)
        
        repository = Repository()
        setupViews()
        loadData()
    }
    
    private fun setupViews() {
        // Financial report banner
        findViewById<View>(R.id.layout_report_banner).setOnClickListener {
            startActivity(Intent(this, FinancialReportActivity::class.java))
        }
        
        // Bottom navigation
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_transactions -> {
                    // Already on transactions
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
    }
    
    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    transactions.clear()
                    transactions.addAll(result.getOrNull() ?: emptyList())
                    
                    // Group transactions by date
                    val groupedTransactions = groupTransactionsByDate(transactions)
                    transactionAdapter.updateData(groupedTransactions)
                } else {
                    Toast.makeText(this@TransactionsActivity, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TransactionsActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
    
    override fun onResume() {
        super.onResume()
        loadData() // Refresh data when returning from AddTransaction
    }
}
