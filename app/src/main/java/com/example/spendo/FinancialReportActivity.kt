package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendo.adapters.CategoryBreakdownAdapter
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FinancialReportActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    private lateinit var categoryAdapter: CategoryBreakdownAdapter
    private var isShowingExpenses = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_report)
        
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
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_budget -> {
                    // Already on budget
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
        bottomNavigationView.selectedItemId = R.id.nav_budget
        
        // Toggle buttons
        findViewById<View>(R.id.btn_expense_toggle).setOnClickListener {
            isShowingExpenses = true
            updateToggleButtons()
            loadData()
        }
        
        findViewById<View>(R.id.btn_income_toggle).setOnClickListener {
            isShowingExpenses = false
            updateToggleButtons()
            loadData()
        }
        
        // Setup recycler view
        categoryAdapter = CategoryBreakdownAdapter(emptyList())
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_category_breakdown).apply {
            layoutManager = LinearLayoutManager(this@FinancialReportActivity)
            adapter = categoryAdapter
        }
        
        updateToggleButtons()

        // Add transaction FAB
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }
    
    private fun updateToggleButtons() {
        val expenseBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_expense_toggle)
        val incomeBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_income_toggle)
        
        if (isShowingExpenses) {
            expenseBtn.setBackgroundColor(getColor(R.color.red))
            expenseBtn.setTextColor(getColor(R.color.white))
            incomeBtn.setBackgroundColor(getColor(android.R.color.transparent))
            incomeBtn.setTextColor(getColor(R.color.gray))
        } else {
            incomeBtn.setBackgroundColor(getColor(R.color.primary_green))
            incomeBtn.setTextColor(getColor(R.color.white))
            expenseBtn.setBackgroundColor(getColor(android.R.color.transparent))
            expenseBtn.setTextColor(getColor(R.color.gray))
        }
    }
    
    private fun loadData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    val transactions = result.getOrNull() ?: emptyList()
                    val filteredTransactions = if (isShowingExpenses) {
                        transactions.filter { it.type == TransactionType.EXPENSE }
                    } else {
                        transactions.filter { it.type == TransactionType.INCOME }
                    }
                    
                    updateSummary(filteredTransactions)
                    updateCategoryBreakdown(filteredTransactions)
                } else {
                    Toast.makeText(this@FinancialReportActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FinancialReportActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateSummary(transactions: List<Transaction>) {
        val total = transactions.sumOf { it.amount }
        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_total_amount).text = 
            "LKR ${String.format("%,d", total)}"
    }
    
    private fun updateCategoryBreakdown(transactions: List<Transaction>) {
        val categoryMap = transactions.groupBy { it.category }
        val categoryData = categoryMap.map { (category, txs) ->
            CategoryBreakdownData(
                category = category,
                amount = txs.sumOf { it.amount },
                color = getCategoryColor(category)
            )
        }.sortedByDescending { it.amount }
        
        categoryAdapter.updateData(categoryData)
    }
    
    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "Food" -> getColor(R.color.red)
            "Transportation" -> getColor(R.color.blue)
            "Shopping" -> getColor(R.color.orange)
            "Entertainment" -> getColor(R.color.yellow)
            "Bills" -> getColor(R.color.primary_green)
            "Healthcare" -> getColor(R.color.blue)
            "Education" -> getColor(R.color.orange)
            else -> getColor(R.color.gray)
        }
    }
}

data class CategoryBreakdownData(
    val category: String,
    val amount: Long,
    val color: Int
)
