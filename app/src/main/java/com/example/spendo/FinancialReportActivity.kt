package com.example.spendo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.adapters.CategoryBreakdownAdapter
import com.example.spendo.adapters.formatCurrency
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*

class FinancialReportActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    private lateinit var categoryAdapter: CategoryBreakdownAdapter
    private var isShowingExpenses = true
    private var currentCurrency: String = "LKR"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_report)

        repository = Repository()
        setupViews()
    }

    override fun onResume() {
        super.onResume()
        val sharedPrefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        currentCurrency = sharedPrefs.getString("Currency", "LKR") ?: "LKR"
        if (::categoryAdapter.isInitialized) {
            categoryAdapter.updateCurrency(currentCurrency)
        }
        loadData()
    }

    private fun setupViews() {
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
                R.id.nav_budget -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_budget

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

        categoryAdapter = CategoryBreakdownAdapter(emptyList(), currentCurrency)
        findViewById<RecyclerView>(R.id.rv_category_breakdown).apply {
            layoutManager = LinearLayoutManager(this@FinancialReportActivity)
            adapter = categoryAdapter
        }

        updateToggleButtons()

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun updateToggleButtons() {
        val expenseBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_expense_toggle)
        val incomeBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_income_toggle)

        if (isShowingExpenses) {
            expenseBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            expenseBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
            incomeBtn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            incomeBtn.setTextColor(ContextCompat.getColor(this, R.color.gray))
        } else {
            incomeBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
            incomeBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
            expenseBtn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            expenseBtn.setTextColor(ContextCompat.getColor(this, R.color.gray))
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
        val total = transactions.sumOf { it.amount }.toDouble()
        // Use the consistent currency formatting helper
        findViewById<TextView>(R.id.tv_total_amount).text = formatCurrency(total, currentCurrency)
    }

    private fun updateCategoryBreakdown(transactions: List<Transaction>) {
        val categoryMap = transactions.groupBy { it.category }
        val categoryData = categoryMap.map { (category, txs) ->
            CategoryBreakdownData(
                category = category,
                amount = txs.sumOf { it.amount }.toDouble(),
                color = getCategoryColor(category)
            )
        }.sortedByDescending { it.amount }

        categoryAdapter.updateData(categoryData)
    }

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "Food" -> ContextCompat.getColor(this, R.color.red)
            "Transportation" -> ContextCompat.getColor(this, R.color.blue)
            "Shopping" -> ContextCompat.getColor(this, R.color.orange)
            "Entertainment" -> ContextCompat.getColor(this, R.color.yellow)
            "Bills" -> ContextCompat.getColor(this, R.color.primary_green)
            "Healthcare" -> ContextCompat.getColor(this, R.color.blue)
            "Education" -> ContextCompat.getColor(this, R.color.orange)
            else -> ContextCompat.getColor(this, R.color.gray)
        }
    }
}

// Change amount type from Long to Double to match Transaction model
data class CategoryBreakdownData(
    val category: String,
    val amount: Double,
    val color: Int
)
