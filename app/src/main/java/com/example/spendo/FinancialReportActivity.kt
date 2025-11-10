package com.example.spendo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendo.adapters.CategoryBreakdownAdapter
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.example.spendo.utils.CurrencyFormatter
import com.example.spendo.utils.LoadingHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

class FinancialReportActivity : AppCompatActivity() {
    private lateinit var repository: Repository
    private lateinit var categoryAdapter: CategoryBreakdownAdapter
    private var isShowingExpenses = true
    private lateinit var loadingHelper: LoadingHelper
    private var isLoading = false
    private var selectedMonth: Int = -1
    private var selectedYear: Int = -1
    private lateinit var btnMonth: Button
    private var selectedCategory: String? = null
    private lateinit var btnCategory: Button
    private val categories = arrayOf("Food", "Transportation", "Shopping", "Entertainment", "Bills", "Healthcare", "Education")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_report)

        repository = Repository()
        loadingHelper = LoadingHelper(this)

        val calendar = Calendar.getInstance()
        selectedMonth = calendar.get(Calendar.MONTH)
        selectedYear = calendar.get(Calendar.YEAR)

        setupViews()
        loadData()
    }

    private fun setupViews() {
        btnMonth = findViewById(R.id.btn_month)
        updateMonthButtonText()
        btnMonth.setOnClickListener {
            showMonthSelectionDialog()
        }

        btnCategory = findViewById(R.id.btn_category_filter)
        updateCategoryButtonText()
        btnCategory.setOnClickListener {
            showCategorySelectionDialog()
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

    private fun showMonthSelectionDialog() {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        AlertDialog.Builder(this)
            .setTitle("Select Month")
            .setSingleChoiceItems(months, selectedMonth) { dialog, which ->
                selectedMonth = which
                updateMonthButtonText()
                loadData()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showCategorySelectionDialog() {
        val allOptions = arrayOf("All") + categories
        val checkedItem = selectedCategory?.let { categories.indexOf(it) + 1 } ?: 0

        AlertDialog.Builder(this)
            .setTitle("Select Category")
            .setSingleChoiceItems(allOptions, checkedItem) { dialog, which ->
                selectedCategory = if (which == 0) {
                    null // "All" selected
                } else {
                    allOptions[which]
                }
                updateCategoryButtonText()
                loadData()
                dialog.dismiss()
            }
            .create()
            .show()
    }


    private fun updateMonthButtonText() {
        val monthName = java.text.DateFormatSymbols().months[selectedMonth]
        btnMonth.text = monthName
    }

    private fun updateCategoryButtonText() {
        btnCategory.text = selectedCategory ?: "Category"
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
        if (isLoading) return // Prevent multiple simultaneous loads

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        isLoading = true
        loadingHelper.showLoading("Loading financial data...")

        lifecycleScope.launch {
            try {
                val result = repository.userTransactions(userId)
                if (result.isSuccess) {
                    val transactions = result.getOrNull() ?: emptyList()

                    val filteredByType = if (isShowingExpenses) {
                        transactions.filter { it.type == TransactionType.EXPENSE }
                    } else {
                        transactions.filter { it.type == TransactionType.INCOME }
                    }

                    val filteredByMonth = filteredByType.filter {
                        val calendar = Calendar.getInstance()
                        calendar.time = it.date.toDate()
                        calendar.get(Calendar.MONTH) == selectedMonth && calendar.get(Calendar.YEAR) == selectedYear
                    }

                    val filteredByCategory = if (selectedCategory == null) {
                        filteredByMonth
                    } else {
                        filteredByMonth.filter { it.category == selectedCategory }
                    }

                    updateSummary(filteredByCategory)
                    updateCategoryBreakdown(filteredByCategory)
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = when {
                        error?.message?.contains("timeout", ignoreCase = true) == true ->
                            "Connection timeout. Please check your internet connection."
                        error?.message?.contains("network", ignoreCase = true) == true ->
                            "Network error. Please check your internet connection."
                        else -> "Failed to load data. Please try again."
                    }
                    Toast.makeText(this@FinancialReportActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Request timed out. Please check your connection and try again."
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your internet connection."
                    else -> "Error loading data: ${e.message ?: "Unknown error"}"
                }
                Toast.makeText(this@FinancialReportActivity, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
                loadingHelper.hideLoading()
            }
        }
    }

    private fun updateSummary(transactions: List<Transaction>) {
        val total = transactions.sumOf { it.amount }
        findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tv_total_amount).text =
            CurrencyFormatter.formatAmountWithCode(this, total)
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

    override fun onDestroy() {
        super.onDestroy()
        loadingHelper.hideLoading()
    }
}

data class CategoryBreakdownData(
    val category: String,
    val amount: Long,
    val color: Int
)
