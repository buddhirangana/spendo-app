package com.example.spendo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spendo.adapters.CategoryBreakdownAdapter
import com.example.spendo.data.Repository
import com.example.spendo.data.Transaction
import com.example.spendo.data.TransactionType
import com.example.spendo.utils.CurrencyFormatter
import com.example.spendo.utils.LoadingHelper
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
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

    private lateinit var pieChart: PieChart

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

        pieChart = findViewById(R.id.pie_chart) // Initialize PieChart
        setupPieChart()

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
        categoryAdapter = CategoryBreakdownAdapter(emptyList(), isShowingExpenses)
        findViewById<RecyclerView>(R.id.rv_category_breakdown).apply {
            layoutManager = LinearLayoutManager(this@FinancialReportActivity)
            adapter = categoryAdapter
        }

        updateToggleButtons()

        // Add transaction FAB
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    private fun setupPieChart() {
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 77f
        pieChart.transparentCircleRadius = 81f
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.setDrawEntryLabels(true)
        pieChart.setNoDataTextColor(
            ContextCompat.getColor(
                this,
                R.color.primary_green
            )
        )
        pieChart.setNoDataText("Loading financial data...")

        pieChart.setExtraOffsets(18f, 18f, 18f, 18f)
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
        val expenseBtn = findViewById<MaterialButton>(R.id.btn_expense_toggle)
        val incomeBtn = findViewById<MaterialButton>(R.id.btn_income_toggle)

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

                    if (filteredByCategory.isEmpty()) {
                        pieChart.clear()
                        pieChart.setNoDataText("No chart data available.")
                        pieChart.invalidate()

                        Toast.makeText(this@FinancialReportActivity, "No data to display.", Toast.LENGTH_SHORT).show()
                    } else {
                        updatePieChart(filteredByCategory)
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

    private fun updatePieChart(transactions: List<Transaction>) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        val categoryMap = transactions.groupBy { it.category }
        for ((category, txs) in categoryMap) {
            val totalAmount = txs.sumOf { it.amount }.toFloat()
            entries.add(PieEntry(totalAmount, category))
            colors.add(getCategoryColor(category))
        }

        val dataSet = PieDataSet(entries, "Category Breakdown")
        dataSet.colors = colors
        dataSet.sliceSpace = 2f

        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineColor = Color.GRAY

        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = PercentFormatter(pieChart)

        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setEntryLabelTextSize(6f)

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.invalidate()
    }

    private fun updateSummary(transactions: List<Transaction>) {
        val total = transactions.sumOf { it.amount }
        val formattedTotal = CurrencyFormatter.formatAmountWithCode(this, total)

        val totalText = if (isShowingExpenses) "- $formattedTotal" else "+ $formattedTotal"
        val totalColor = if (isShowingExpenses) R.color.red else R.color.primary_green

        pieChart.centerText = totalText
        pieChart.setCenterTextColor(ContextCompat.getColor(this, totalColor))
        pieChart.setCenterTextSize(24f)
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

        categoryAdapter.updateData(categoryData, isShowingExpenses)
    }

    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "Food" -> ContextCompat.getColor(this, R.color.food)
            "Transportation" -> ContextCompat.getColor(this, R.color.transport)
            "Shopping" -> ContextCompat.getColor(this, R.color.shopping)
            "Entertainment" -> ContextCompat.getColor(this, R.color.entertainment)
            "Bills" -> ContextCompat.getColor(this, R.color.bills)
            "Healthcare" -> ContextCompat.getColor(this, R.color.health)
            "Education" -> ContextCompat.getColor(this, R.color.education)
            "Salary" -> ContextCompat.getColor(this, R.color.salary)
            else -> ContextCompat.getColor(this, R.color.other)
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
