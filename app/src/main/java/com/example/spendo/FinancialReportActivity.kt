package com.example.spendo

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
        // Back button
        findViewById<View>(R.id.iv_back).setOnClickListener {
            finish()
        }
        
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
    }
    
    private fun updateToggleButtons() {
        val expenseBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_expense_toggle)
        val incomeBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_income_toggle)
        
        if (isShowingExpenses) {
            expenseBtn.setBackgroundColor(getColor(R.color.primary_green))
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
            "Shopping" -> getColor(R.color.yellow)
            "Entertainment" -> getColor(R.color.orange)
            "Bills" -> getColor(R.color.gray)
            "Healthcare" -> getColor(R.color.primary_green)
            "Education" -> getColor(R.color.blue)
            else -> getColor(R.color.gray)
        }
    }
}

data class CategoryBreakdownData(
    val category: String,
    val amount: Long,
    val color: Int
)
